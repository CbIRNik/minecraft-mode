package com.infdimmod.particle;

import com.infdimmod.particle.custom.GreenLightningParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class ModParticlesClient {
    public static void registerParticleFactories() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.GREEN_LIGHTNING, GreenLightningParticle.Factory::new);
    }
}