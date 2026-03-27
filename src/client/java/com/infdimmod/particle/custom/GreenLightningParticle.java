package com.infdimmod.particle.custom;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GreenLightningParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float fixedRoll;

    protected GreenLightningParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z, vx, vy, vz);
        this.spriteProvider = spriteProvider;
        this.maxAge = 4;
        this.scale = 0.1f; // размер
        this.fixedRoll = world.random.nextFloat() * ((float)Math.PI * 2f);

        // случайный кадр при появлении
        int startFrame = world.random.nextInt(8);
        this.setSprite(spriteProvider.getSprite(startFrame, 8));
    }

    @Override
    public void buildGeometry(VertexConsumer buffer, Camera camera, float tickDelta) {
        float lx = (float)(MathHelper.lerp(tickDelta, this.prevPosX, this.x) - camera.getPos().x);
        float ly = (float)(MathHelper.lerp(tickDelta, this.prevPosY, this.y) - camera.getPos().y);
        float lz = (float)(MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - camera.getPos().z);

        // Рисуем две пересекающиеся плоскости для объема
        renderPlane(buffer, lx, ly, lz, fixedRoll, tickDelta);
        renderPlane(buffer, lx, ly, lz, fixedRoll + (float)Math.PI / 2f, tickDelta);
    }

    private void renderPlane(VertexConsumer buffer, float x, float y, float z, float angle, float tickDelta) {
        Vector3f[] vertices = {
                new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0),
                new Vector3f(1, 1, 0), new Vector3f(1, -1, 0)
        };

        Quaternionf rotation = new Quaternionf().rotationY(angle);
        float s = this.getSize(tickDelta);
        int light = this.getBrightness(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        for (int i = 0; i < 4; i++) {
            Vector3f v = vertices[i];
            rotation.transform(v);
            v.mul(s);

            float u = (i == 0 || i == 1) ? maxU : minU;
            float currentV = (i == 1 || i == 2) ? minV : maxV;

            buffer.vertex(x + v.x, y + v.y, z + v.z)
                    .color(this.red, this.green, this.blue, this.alpha)
                    .texture(u, currentV)
                    .light(light);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age < this.maxAge) {
            // случайное движение
            this.velocityX += (this.random.nextFloat() - 0.5f) * 0.02f;
            this.velocityY += (this.random.nextFloat() - 0.5f) * 0.02f;
            this.velocityZ += (this.random.nextFloat() - 0.5f) * 0.02f;

            this.velocityX *= 0.95f;
            this.velocityY *= 0.95f;
            this.velocityZ *= 0.95f;
        }
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            return new GreenLightningParticle(world, x, y, z, vx, vy, vz, this.spriteProvider);
        }
    }
}