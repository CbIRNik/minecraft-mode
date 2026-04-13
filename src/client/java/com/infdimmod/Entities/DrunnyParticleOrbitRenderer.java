package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.DrunnyParticleOrbitEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

public class DrunnyParticleOrbitRenderer extends EntityRenderer<DrunnyParticleOrbitEntity> {
    private static final Identifier TEXTURE = Identifier.of("infdimmod", "textures/entity/drunny_particle_orbit.png");

    public DrunnyParticleOrbitRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(DrunnyParticleOrbitEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(0.55F, 0.55F, 0.55F);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));

        vertex(vertexConsumer, matrix, entry, -0.5F, -0.5F, 0.0F, 1.0F, light);
        vertex(vertexConsumer, matrix, entry, 0.5F, -0.5F, 1.0F, 1.0F, light);
        vertex(vertexConsumer, matrix, entry, 0.5F, 0.5F, 1.0F, 0.0F, light);
        vertex(vertexConsumer, matrix, entry, -0.5F, 0.5F, 0.0F, 0.0F, light);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, MatrixStack.Entry entry, float x, float y, float u, float v, int light) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 230)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public Identifier getTexture(DrunnyParticleOrbitEntity entity) {
        return TEXTURE;
    }

    @Override
    public int getBlockLight(DrunnyParticleOrbitEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public int getSkyLight(DrunnyParticleOrbitEntity entity, BlockPos pos) {
        return 15;
    }
}
