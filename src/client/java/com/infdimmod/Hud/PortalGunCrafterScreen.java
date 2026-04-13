package com.infdimmod.Hud;

import com.infdimmod.InfDimMod;
import com.infdimmod.util.PortalGunCrafterScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PortalGunCrafterScreen extends HandledScreen<PortalGunCrafterScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(InfDimMod.MOD_ID, "textures/gui/portal_gun_crafter.png");
    //окно высота 166 ширина 176!
    public PortalGunCrafterScreen(PortalGunCrafterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        int rotationIndex = handler.getRotationIndexHint();
        int rotationVersion = handler.getRotationVersionHint();
        int secondsLeft = handler.getSecondsUntilNextRotationHint();
        context.drawText(textRenderer,
                Text.translatable("gui.infdimmod.crafter.rotation_hint", rotationIndex + 1),
                8, 6, 0x606060, false);
        context.drawText(textRenderer,
                Text.translatable("gui.infdimmod.crafter.rotation_version", rotationVersion),
                8, 16, 0x606060, false);
        context.drawText(textRenderer,
                Text.translatable("gui.infdimmod.crafter.next_rotation_seconds", secondsLeft),
                8, 26, 0x606060, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
