package com.gberguy.rei.client.visual;

import net.minecraft.client.particle.ParticleSimpleAnimated;
import net.minecraft.world.World;

public final class FairyDustParticle extends ParticleSimpleAnimated {
    public FairyDustParticle(World world, double x, double y, double z, double companionHeight) {
        super(world, x, y, z, 176, 8, (float) (-companionHeight * 0.0005));
        particleScale = (float) (companionHeight * 0.1);
        motionX = 0;
        motionY = -companionHeight * 0.01;
        motionZ = 0;
        particleMaxAge = 60;
        canCollide = false;
        setColorFade(15916745);
    }

    @Override
    public boolean shouldDisableDepth() {
        return false;
    }
}
