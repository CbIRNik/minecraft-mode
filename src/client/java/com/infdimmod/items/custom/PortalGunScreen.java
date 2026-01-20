package com.infdimmod.items.custom;

import com.infdimmod.items.custom.portalgun.PortalGun;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import static com.infdimmod.items.custom.PortalGunHudRenderer.setCurrentCode;

public class PortalGunScreen extends Screen {
    private TextFieldWidget codeInput;
    private String displayedCode = "";

    public PortalGunScreen() {
        super(Text.literal("Portal Gun"));
    }

    @Override
    protected void init() {
        super.init();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            ItemStack currentStack = client.player.getMainHandStack().copy();
            if (currentStack.getItem() instanceof PortalGun) {
                this.displayedCode = PortalGun.getPortalCode(currentStack);
            }
        }

        // Создаем текстовое поле для ввода кода
        this.codeInput = new TextFieldWidget(this.textRenderer, this.width / 2 - 75, this.height / 2 - 30, 150, 20, Text.literal("Code"));
        this.codeInput.setMaxLength(8);
        this.addDrawableChild(this.codeInput);
        this.setFocused(this.codeInput);

        // Кнопка для сохранения кода
        int buttonWidth = 100;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Submit Code"), button -> submitCode())
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build());

        // Кнопка для закрытия экрана
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(x, y + 25, buttonWidth, buttonHeight)
                .build());
    }

    private void submitCode() {
        String input = this.codeInput.getText();
        // Проверяем, что строка содержит только цифры и английские буквы
        if (input.matches("^[a-zA-Z0-9]*$") && !input.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                ItemStack currentStack = client.player.getMainHandStack().copy();
                if (currentStack.getItem() instanceof PortalGun) {
                this.displayedCode = input;
                setCurrentCode(input);

                // Обновляем локальную копию
                PortalGun.setPortalCode(currentStack, input);

                //отправляем на сервер СДЕЛАТЬ СДЕЛАТЬ СДЕЛАТЬ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                // Обновляем стек в руке игрока (клиентская сторона)
                client.player.getInventory().main.set(
                        client.player.getInventory().selectedSlot,
                        currentStack.copy()
                );
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Отображаем текущий код
        context.drawText(this.textRenderer, "Current Code: " + this.displayedCode, this.width / 2 - 75, this.height / 2 - 60, 0xFFFFFF, false);
        context.drawText(this.textRenderer, "Enter Code:", this.width / 2 - 75, this.height / 2 - 45, 0xFFFFFF, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}


