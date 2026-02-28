package com.infdimmod.Entities;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GreenPortalRenderer extends EntityRenderer<GreenPortal> {
    private static final Identifier TEXTURE = Identifier.of("infdimmod", "textures/entity/greenportal.png");

    public GreenPortalRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(GreenPortal entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getRoll()));

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        // Рисуем плоскость
        drawVertex(entry, buffer, -0.5f, -0.5f, light, 0, 1);
        drawVertex(entry, buffer, 0.5f, -0.5f, light, 1, 1);
        drawVertex(entry, buffer, 0.5f, 0.5f, light, 1, 0);
        drawVertex(entry, buffer, -0.5f, 0.5f, light, 0, 0);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawVertex(MatrixStack.Entry entry, VertexConsumer buffer, float x, float y, int light, float u, float v) {
        buffer.vertex(entry.getPositionMatrix(), x, y, 0.0f)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0, 1, 0);
    }

    @Override
    public Identifier getTexture(GreenPortal entity) {
        return TEXTURE;
    }
}