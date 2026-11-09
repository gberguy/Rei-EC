package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

public final class CompanionRenderer implements IResourceManagerReloadListener {
    public static final ResourceLocation MODEL = new ResourceLocation("rei", "companion/rei.bbmodel");
    private BlockbenchModel model;
    private final CompanionTextures textures = new CompanionTextures();
    private TextureManager textureManager;
    private CompanionTextures.Backend textureBackend;
    private int glow = -1;

    public BlockbenchModel model() { return model; }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        model = null; glow = -1;
        try (InputStreamReader reader = new InputStreamReader(resources.getResource(MODEL).getInputStream(), StandardCharsets.UTF_8)) {
            BlockbenchModel loaded = BlockbenchModel.read(reader);
            for (int index = 0; index < loaded.textureNames.size(); index++) {
                if (loaded.textureNames.get(index).toLowerCase(java.util.Locale.ROOT).contains("glowmask")) glow = index;
            }
            model = loaded;
        } catch (Exception exception) {
            LogManager.getLogger("Rei").error("Could not load Rei's Blockbench companion model; companion visuals are disabled", exception);
        }
    }

    boolean prepare() {
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureManager manager = minecraft == null ? null : minecraft.getTextureManager();
        if (manager == null || (model == null && textures.size() == 0)) return false;
        if (textureManager != manager) {
            textureManager = manager;
            textureBackend = new CompanionTextures.Backend() {
                @Override
                public ResourceLocation upload(int index, java.awt.image.BufferedImage image) {
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation location = null;
                    try {
                        location = manager.getDynamicTextureLocation("rei_companion_" + index, texture);
                        texture.setBlurMipmap(false, false);
                        return location;
                    } catch (RuntimeException exception) {
                        if (location == null) texture.deleteGlTexture();
                        else manager.deleteTexture(location);
                        throw exception;
                    }
                }

                @Override
                public void delete(ResourceLocation location) {
                    manager.deleteTexture(location);
                }
            };
        }
        if (textures.matches(model, textureBackend)) return true;
        try (VisualRenderState ignored = new VisualRenderState()) {
            return textures.prepare(model, textureBackend);
        } catch (RuntimeException exception) {
            model = null;
            LogManager.getLogger("Rei").error("Could not upload Rei's companion textures; companion visuals are disabled", exception);
            return false;
        }
    }

    public void draw(BlockbenchModel model, double[][] pose, double x, double y, double z, double yaw,
                     int brightness, VisualSettings settings) {
        if (model != this.model || !prepare()) return;
        double[][][] vertices = model.vertices(pose);
        try (VisualRenderState ignored = new VisualRenderState()) {
            Minecraft.getMinecraft().entityRenderer.enableLightmap();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness & 65535, brightness >>> 16);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D(); GlStateManager.enableBlend(); GlStateManager.disableLighting();
            GlStateManager.disableCull(); GlStateManager.enableAlpha(); GlStateManager.enableDepth();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f); GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate((float) (180 - yaw), 0, 1, 0);
            double scale = settings.height / model.height;
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.translate(-model.center[0], -model.center[1], -model.center[2]);
            for (int texture = 0; texture < textures.size(); texture++) batch(model, vertices, texture, texture);
            if (settings.glow && glow >= 0) {
                GlStateManager.depthMask(false);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                batch(model, vertices, 0, glow);
            }
        }
    }

    private void batch(BlockbenchModel model, double[][][] vertices, int faceTexture, int boundTexture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(textures.get(boundTexture));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int cube = 0; cube < model.cubes.size(); cube++) {
            for (BlockbenchModel.Face face : model.cubes.get(cube).faces) {
                if (face.texture != faceTexture) continue;
                double[][] uv = {{face.uv[0], face.uv[1]}, {face.uv[0], face.uv[3]},
                        {face.uv[2], face.uv[3]}, {face.uv[2], face.uv[1]}};
                for (int vertex = 0; vertex < 4; vertex++) {
                    double[] point = vertices[cube][face.corners[vertex]];
                    double[] tex = uv[Math.floorMod(vertex + face.rotation / 90, 4)];
                    buffer.pos(point[0], point[1], point[2]).tex(tex[0] / model.textureWidth, tex[1] / model.textureHeight).endVertex();
                }
            }
        }
        tessellator.draw();
    }
}
