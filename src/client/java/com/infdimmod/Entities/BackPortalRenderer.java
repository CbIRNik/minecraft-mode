package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.BackPortal;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;


public class BackPortalRenderer extends EntityRenderer<BackPortal> {
    private static final Identifier TEXTURE = Identifier.of("infdimmod", "textures/entity/green_portal.png");

    public BackPortalRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(BackPortal entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

        matrices.push();
        //портал
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));

        float scale = entity.getVisualScale(tickDelta);
        matrices.scale(scale, scale, scale);

        int frameCount = 8;
        int ticksPerFrame = 6;
        int currentFrame = (entity.getAge() / ticksPerFrame) % frameCount;

        float vMin = (float) currentFrame / frameCount;
        float vMax = (float) (currentFrame + 1) / frameCount;

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));

        drawRect(matrices.peek(), buffer, 0.5f, light, 255, vMin, vMax);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawRect(MatrixStack.Entry entry, VertexConsumer buffer, float size, int light, int alpha, float vMin, float vMax) {
        float halfW = size * 1.125f; //18 пикселей
        float halfH = size * 1.875f;// 30 пикселей

        // лицевая
        drawVertex(entry, buffer, -halfW, -halfH, light, 0, vMax, alpha);
        drawVertex(entry, buffer,  halfW, -halfH, light, 1, vMax, alpha);
        drawVertex(entry, buffer,  halfW,  halfH, light, 1, vMin, alpha);
        drawVertex(entry, buffer, -halfW,  halfH, light, 0, vMin, alpha);

        // задняя
        drawVertex(entry, buffer, -halfW,  halfH, light, 0, vMin, alpha);
        drawVertex(entry, buffer,  halfW,  halfH, light, 1, vMin, alpha);
        drawVertex(entry, buffer,  halfW, -halfH, light, 1, vMax, alpha);
        drawVertex(entry, buffer, -halfW, -halfH, light, 0, vMax, alpha);
    }

    private void drawVertex(MatrixStack.Entry entry, VertexConsumer buffer, float x, float y, int light, float u, float v, int alpha) {
        buffer.vertex(entry.getPositionMatrix(), x, y, 0.0f)
                .color(255, 255, 255, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);
    }

    @Override
    public Identifier getTexture(BackPortal entity) {
        return TEXTURE;
    }

    @Override
    public int getBlockLight(BackPortal entity, BlockPos pos) {
        return 15;
    }

    @Override
    public int getSkyLight(BackPortal entity, BlockPos pos) {
        return 15;
    }
}