package com.infdimmod.Hud;

import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.network.PortalCodePayload;
import com.infdimmod.network.PortalCoordsPayload; // Не забудь импортировать свой новый Payload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PortalGunScreen extends Screen {
    private TextFieldWidget codeInput;
    private TextFieldWidget xInput, yInput, zInput; // Поля для координат
    private String displayedCode = "";

    public PortalGunScreen() {
        super(Text.literal("Portal Gun"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Инициализация текущих данных из предмета
        PortalGunComponents.PortalCoords currentCoords = null;
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                this.displayedCode = PortalGun.getPortalCode(stack);
                currentCoords = PortalGun.getTargetCoords(stack);
            }
        }

        // Поле для КОДА (смещаем чуть выше)
        this.codeInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 60, 150, 20, Text.literal("Code"));
        this.codeInput.setMaxLength(12);
        this.codeInput.setText(this.displayedCode);
        this.addDrawableChild(this.codeInput);

        // Поля для КООРДИНАТ (X, Y, Z в ряд)
        this.xInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 25, 45, 20, Text.literal("X"));
        this.yInput = new TextFieldWidget(this.textRenderer, centerX - 22, centerY - 25, 45, 20, Text.literal("Y"));
        this.zInput = new TextFieldWidget(this.textRenderer, centerX + 31, centerY - 25, 45, 20, Text.literal("Z"));

        // Устанавливаем текущие значения координат в поля
        if (currentCoords != null) {
            this.xInput.setText(String.valueOf(currentCoords.x()));
            this.yInput.setText(String.valueOf(currentCoords.y()));
            this.zInput.setText(String.valueOf(currentCoords.z()));
        }

        // Фильтр: разрешаем только цифры, минус и точку
        this.xInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));
        this.yInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));
        this.zInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));

        this.addDrawableChild(this.xInput);
        this.addDrawableChild(this.yInput);
        this.addDrawableChild(this.zInput);

        // Кнопка SUBMIT
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("hud.infdimmod.submitcode"), button -> submitData())
                .dimensions(centerX - 50, centerY + 10, 100, 20)
                .build());

        this.setFocused(this.codeInput);
    }

    private void submitData() {
        String code = this.codeInput.getText();

        // Парсинг координат с проверкой лимитов
        double x = parseDouble(xInput.getText());
        double y = parseDouble(yInput.getText());
        double z = parseDouble(zInput.getText());

        if (code.matches("^[a-zA-Z0-9]*$") && !code.isEmpty()) {
            if (client.player != null) {
                ItemStack stack = client.player.getMainHandStack();
                if (stack.getItem() instanceof PortalGun) {
                    this.displayedCode = code;

                    // Отправляем ПЕРВЫЙ пакет (код)
                    ClientPlayNetworking.send(new PortalCodePayload(code));

                    // Отправляем ВТОРОЙ пакет (координаты)
                    ClientPlayNetworking.send(new PortalCoordsPayload(x, y, z));

                    // Обновляем локально (для тултипа)
                    PortalGun.setPortalCode(stack, code);
                    PortalGun.setTargetCoords(stack, x, y, z);

                    this.close(); // Закрываем экран после ввода
                }
            }
        }
    }

    private double parseDouble(String value) {
        try {
            double d = Double.parseDouble(value);
            double limit = getWorldBorderLimit(); // Получаем актуальный лимит

            // Ограничиваем введенное число текущей границей мира
            return Math.max(-limit, Math.min(limit, d));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("hud.infdimmod.currentcode").getString() + " " + this.displayedCode, centerX, centerY - 80, 0x00FFFF);

        // Подписи над полями X Y Z
        context.drawText(this.textRenderer, "X", centerX - 75, centerY - 35, 0xAAAAAA, false);
        context.drawText(this.textRenderer, "Y", centerX - 22, centerY - 35, 0xAAAAAA, false);
        context.drawText(this.textRenderer, "Z", centerX + 31, centerY - 35, 0xAAAAAA, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private double getWorldBorderLimit() {
        if (this.client != null && this.client.world != null) {
            // getSize() возвращает общую длину стороны (например, 60 000 000)
            // Нам нужен радиус (расстояние от центра до края)
            return this.client.world.getWorldBorder().getSize() / 2.0;
        }
        return 30000000.0; // Фолбэк, если мир еще не прогружен
    }
}

//проверочка намбер ту