package com.infdimmod.Entities;

import net.minecraft.client.render.*;
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

        // поворот по горизонтали
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));

        // поворот по вертикали
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));

        // масштаб
        float scale = entity.getVisualScale(tickDelta);
        matrices.scale(scale, scale, scale);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        float size = 0.5f;

        // лицевая сторона (нормаль 0, 0, 1)
        drawVertex(entry, buffer, -size, -size, light, 0, 1);
        drawVertex(entry, buffer, size, -size, light, 1, 1);
        drawVertex(entry, buffer, size, size, light, 1, 0);
        drawVertex(entry, buffer, -size, size, light, 0, 0);

        // задняя сторона
        drawVertex(entry, buffer, -size, size, light, 0, 0);
        drawVertex(entry, buffer, size, size, light, 1, 0);
        drawVertex(entry, buffer, size, -size, light, 1, 1);
        drawVertex(entry, buffer, -size, -size, light, 0, 1);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawVertex(MatrixStack.Entry entry, VertexConsumer buffer, float x, float y, int light, float u, float v) {
        buffer.vertex(entry.getPositionMatrix(), x, y, 0.0f)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);
    }

    @Override
    public Identifier getTexture(GreenPortal entity) {
        return TEXTURE;
    }
}