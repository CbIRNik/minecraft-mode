package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.GreenPortal;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class GreenPortalRenderer extends EntityRenderer<GreenPortal> {
    private static final Identifier TEXTURE = Identifier.of("infdimmod", "textures/entity/green_portal.png");

    private static final List<Identifier> TRAIL_TEXTURES = new ArrayList<>();

    static {
        TRAIL_TEXTURES.add(Identifier.of("infdimmod", "textures/entity/greenportal_trail_1.png")); // Ближе к порталу
        TRAIL_TEXTURES.add(Identifier.of("infdimmod", "textures/entity/greenportal_trail_2.png"));
        TRAIL_TEXTURES.add(Identifier.of("infdimmod", "textures/entity/greenportal_trail_3.png")); // Ближе к игроку
    }

    public GreenPortalRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(GreenPortal entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

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

        // шлейф
        int maxAge = entity.getMaxAge();
        int currentAge = entity.getAge();

        if (currentAge < maxAge) {
            Vector3f startVec = entity.getStartVec();
            Vec3d currentPos = entity.getLerpedPos(tickDelta);

            double diffX = (double)startVec.x - currentPos.x;
            double diffY = (double)startVec.y - currentPos.y;
            double diffZ = (double)startVec.z - currentPos.z;

            float alphaFactor = 1.0f - (1.0f - 2.0f * currentAge / (float)maxAge) * (1.0f - 2.0f * currentAge / (float)maxAge);

            if (alphaFactor > 0.001f) {
                int numTrails = TRAIL_TEXTURES.size();

                for (int i = 0; i < numTrails; i++) {
                    Identifier trailTexture = TRAIL_TEXTURES.get(i);
                    VertexConsumer trailBuffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(trailTexture));

                    float segmentT = (float)(i + 1) / (numTrails + 1);

                    matrices.push();


                    matrices.translate(diffX * segmentT, diffY * segmentT, diffZ * segmentT);

                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));

                    float trailIntensity = (float)(numTrails - i) / (numTrails + 1);
                    int finalAlpha = (int) (255 * alphaFactor * trailIntensity);

                    if (finalAlpha > 1) {
                        drawQuad(matrices.peek(), trailBuffer, 0.3f, light, finalAlpha);
                    }

                    matrices.pop();
                }
            }
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawQuad(MatrixStack.Entry entry, VertexConsumer buffer, float size, int light, int alpha) {
        // лицевая
        drawVertex(entry, buffer, -size, -size, light, 0, 1, alpha);
        drawVertex(entry, buffer, size, -size, light, 1, 1, alpha);
        drawVertex(entry, buffer, size, size, light, 1, 0, alpha);
        drawVertex(entry, buffer, -size, size, light, 0, 0, alpha);

        // задняя
        drawVertex(entry, buffer, -size, size, light, 0, 0, alpha);
        drawVertex(entry, buffer, size, size, light, 1, 0, alpha);
        drawVertex(entry, buffer, size, -size, light, 1, 1, alpha);
        drawVertex(entry, buffer, -size, -size, light, 0, 1, alpha);
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
    public Identifier getTexture(GreenPortal entity) {
        return TEXTURE;
    }

    @Override
    public int getBlockLight(GreenPortal entity, BlockPos pos) {
        return 15;
    }

    @Override
    public int getSkyLight(GreenPortal entity, BlockPos pos) {
        return 15;
    }
}