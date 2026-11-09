package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import com.gberguy.rei.network.VisualPresenceMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class CompanionVisuals {
    private final VisualPresence presence = new VisualPresence();
    private final Map<UUID, PlayerAnimation> animations = new HashMap<>();
    private final CompanionRenderer renderer;
    private final VisualSettings settings;
    private final Random particles = new Random();
    private BlockbenchModel previousModel;
    private WingDustEmitter wingDust;
    private net.minecraft.world.World visualWorld;

    public CompanionVisuals(CompanionRenderer renderer, VisualSettings settings) {
        this.renderer = renderer; this.settings = settings;
    }

    public void update(VisualPresenceMessage message) {
        synchronizeWorld();
        presence.update(message);
        if (!message.equipped) animations.remove(message.playerId);
    }

    public void clear() { presence.clear(); animations.clear(); visualWorld = null; }

    private void synchronizeWorld() {
        net.minecraft.world.World current = Minecraft.getMinecraft().world;
        if (current != visualWorld) { clear(); visualWorld = current; }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld() == visualWorld) clear();
    }

    private void synchronizeModel(BlockbenchModel model) {
        if (previousModel != model) {
            animations.clear(); previousModel = model;
            wingDust = model == null ? null : new WingDustEmitter(model);
        }
    }

    private PlayerAnimation animation(EntityPlayer player, double now) {
        PlayerAnimation state = animations.get(player.getUniqueID());
        if (state == null || state.entityId != player.getEntityId()) {
            state = new PlayerAnimation(player, now, settings);
            animations.put(player.getUniqueID(), state);
        }
        return state;
    }

    private boolean visible(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return !player.isDead && !player.isSpectator() && !player.isInvisible()
                && presence.equipped(player.getUniqueID(), player.getEntityId(), player.dimension)
                && minecraft.player != null && !player.isInvisibleToPlayer(minecraft.player);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || minecraft.world == null || minecraft.isGamePaused()) return;
        synchronizeWorld();
        BlockbenchModel model = renderer.model();
        synchronizeModel(model);
        double now = minecraft.world.getTotalWorldTime() / 20.0;
        Set<UUID> live = new HashSet<>();
        for (EntityPlayer player : minecraft.world.playerEntities) {
            if (visible(player)) {
                live.add(player.getUniqueID());
                if (settings.particles && minecraft.gameSettings.particleSetting < 2 && model != null
                        && player.ticksExisted % 5 == 0 && player.getDistanceSq(minecraft.player) < 24 * 24) {
                    double[][] pose = animation(player, now).animation.pose(model, now, settings);
                    double[][] sites = wingDust.emissions(pose, anchor(player, 1, settings), bodyYaw(player, 1), settings.height, minecraft.gameSettings.particleSetting, particles);
                    for (double[] site : sites) {
                        minecraft.effectRenderer.addEffect(new FairyDustParticle(minecraft.world, site[0], site[1], site[2], settings.height));
                    }
                }
            }
        }
        animations.keySet().retainAll(live);
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.getRenderViewEntity();
        if (minecraft.world == null || minecraft.player == null || camera == null || !renderer.prepare()) return;
        BlockbenchModel model = renderer.model();
        synchronizeWorld();
        synchronizeModel(model);
        float partial = event.getPartialTicks();
        double cx = interpolate(camera.lastTickPosX, camera.posX, partial);
        double cy = interpolate(camera.lastTickPosY, camera.posY, partial);
        double cz = interpolate(camera.lastTickPosZ, camera.posZ, partial);
        Frustum frustum = new Frustum(); frustum.setPosition(cx, cy, cz);
        double now = (minecraft.world.getTotalWorldTime() + partial) / 20.0;
        for (EntityPlayer player : minecraft.world.playerEntities) {
            if (!visible(player) || player.getDistanceSq(camera) > settings.distance * settings.distance) continue;
            double[] anchor = anchor(player, partial, settings);
            double radius = settings.height * 3;
            if (!frustum.isBoundingBoxInFrustum(new AxisAlignedBB(anchor[0] - radius, anchor[1] - radius, anchor[2] - radius,
                    anchor[0] + radius, anchor[1] + radius, anchor[2] + radius))) continue;
            PlayerAnimation state = animation(player, now);
            double yaw = bodyYaw(player, partial);
            renderer.draw(model, state.animation.pose(model, now, settings), anchor[0] - cx, anchor[1] - cy, anchor[2] - cz,
                    yaw, player.getBrightnessForRender(), settings);
        }
    }

    public static double[] anchor(EntityPlayer player, float partial, VisualSettings settings) {
        double yaw = Math.toRadians(bodyYaw(player, partial));
        double side = settings.side + Math.copySign(settings.height * settings.extraOutward, settings.side == 0 ? 1 : settings.side);
        return new double[] {interpolate(player.lastTickPosX, player.posX, partial) - Math.cos(yaw) * side + Math.sin(yaw) * settings.behind,
                interpolate(player.lastTickPosY, player.posY, partial) + player.getEyeHeight() + settings.vertical + settings.height * (settings.verticalClearance + settings.extraLift),
                interpolate(player.lastTickPosZ, player.posZ, partial) - Math.sin(yaw) * side - Math.cos(yaw) * settings.behind};
    }

    private static double bodyYaw(EntityPlayer player, float partial) {
        return player.prevRenderYawOffset + MathHelper.wrapDegrees(player.renderYawOffset - player.prevRenderYawOffset) * partial;
    }

    private static double interpolate(double old, double current, float partial) { return old + (current - old) * partial; }

    private static final class PlayerAnimation {
        final int entityId;
        final VisualAnimationState animation;
        PlayerAnimation(EntityPlayer player, double now, VisualSettings settings) {
            entityId = player.getEntityId();
            animation = new VisualAnimationState(player.getUniqueID().getMostSignificantBits() ^ player.getUniqueID().getLeastSignificantBits(), now, settings);
        }
    }
}
