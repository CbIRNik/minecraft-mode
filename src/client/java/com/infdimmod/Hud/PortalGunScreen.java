package com.infdimmod.Hud;

import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.network.PortalCodePayload;
import com.infdimmod.network.PortalCoordsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PortalGunScreen extends Screen {
    // Основные поля ввода
    private TextFieldWidget codeInput;
    private TextFieldWidget xInput, yInput, zInput;
    private String displayedCode = "";

    // Данные для истории и избранного
    private static final List<PortalEntry> HISTORY = new ArrayList<>();
    private static final List<PortalEntry> FAVORITES = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    public PortalGunScreen() {
        super(Text.literal("Portal Gun"));
    }

    // Параметры скроллинга
    private double favScroll = 0;   // Смещение для избранного
    private double histScroll = 0;  // Смещение для истории
    private final int ENTRY_HEIGHT = 22;      // Высота одной кнопки
    private final int MAX_VISIBLE_FAV = 5;    // Сколько кнопок избранного влезает в окно
    private final int MAX_VISIBLE_HIST = 5;   // Сколько кнопок истории влезает в окно
    private final List<ButtonWidget> scrollableButtons = new ArrayList<>();

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int centerY = this.height / 2;

        if (mouseY < centerY) { // Мышь в зоне "Избранного"
            favScroll = Math.max(0, favScroll - verticalAmount * 10); // 15 - скорость прокрутки
            int maxScroll = Math.max(0, (FAVORITES.size() - MAX_VISIBLE_FAV) * ENTRY_HEIGHT);
            favScroll = Math.min(favScroll, maxScroll);
        } else { // Мышь в зоне "Истории"
            histScroll = Math.max(0, histScroll - verticalAmount * 10);
            int maxScroll = Math.max(0, (HISTORY.size() - MAX_VISIBLE_HIST) * ENTRY_HEIGHT);
            histScroll = Math.min(histScroll, maxScroll);
        }

        refreshScrollablePanels(); // Перерисовываем кнопки с учетом нового сдвига
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawScrollBar(DrawContext context, int x, int y, int height, double scroll, int totalElements, int visibleElements) {
        if (totalElements <= visibleElements) return; // Если всё влезает, ползунок не нужен

        int barHeight = (int) ((double) visibleElements / totalElements * height);
        int barPos = (int) (scroll / (totalElements * ENTRY_HEIGHT) * height);

        // Отрисовка серого фона и белого ползунка
        context.fill(x, y, x + 2, y + height, 0x80000000); // Фон
        context.fill(x, y + barPos, x + 2, y + barPos + barHeight, 0xFFFFFFFF); // Ползунок
    }

    private void refreshScrollablePanels() {
        // 1. Удаляем ТОЛЬКО кнопки из списков, не трогая TextFieldWidget
        scrollableButtons.forEach(this::remove);
        scrollableButtons.clear();

        int rightX = (int) (this.width * 0.6);
        int listWidth = (int) (this.width * 0.35);
        int centerY = this.height / 2;

        // 2. Отрисовка Избранного
        int favStartY = 25;
        for (int i = 0; i < FAVORITES.size(); i++) {
            int yPos = favStartY + (i * ENTRY_HEIGHT) - (int) favScroll;
            // Проверка: рисуем только если кнопка внутри "окна" видимости
            if (yPos >= favStartY - 10 && yPos < favStartY + (MAX_VISIBLE_FAV * ENTRY_HEIGHT)) {
                addScrollableEntry(rightX, yPos, listWidth, FAVORITES.get(i));
            }
        }

        // 3. Отрисовка Истории
        int histStartY = centerY + 15;
        for (int i = 0; i < HISTORY.size(); i++) {
            int yPos = histStartY + (i * ENTRY_HEIGHT) - (int) histScroll;
            if (yPos >= histStartY - 10 && yPos < histStartY + (MAX_VISIBLE_HIST * ENTRY_HEIGHT)) {
                addScrollableEntry(rightX, yPos, listWidth, HISTORY.get(i));
            }
        }
    }

    private void addScrollableEntry(int x, int y, int width, PortalEntry entry) {
        // Создаем кнопку кода
        ButtonWidget btn = ButtonWidget.builder(Text.literal(entry.code()), b -> {
            this.codeInput.setText(entry.code());
            this.xInput.setText(String.valueOf(entry.x()));
            this.yInput.setText(String.valueOf(entry.y()));
            this.zInput.setText(String.valueOf(entry.z()));
        }).dimensions(x, y, width - 25, 20).build();

        // Создаем кнопку звезды
        boolean isFav = FAVORITES.stream().anyMatch(e -> e.code().equals(entry.code()));
        ButtonWidget starBtn = ButtonWidget.builder(Text.literal(isFav ? "★" : "☆"), b -> {
            if (isFav) FAVORITES.removeIf(e -> e.code().equals(entry.code()));
            else FAVORITES.add(0, entry);
            refreshScrollablePanels();
        }).dimensions(x + width - 22, y, 20, 20).build();

        // Добавляем в список отслеживания и на экран
        this.addDrawableChild(btn);
        this.addDrawableChild(starBtn);
        scrollableButtons.add(btn);
        scrollableButtons.add(starBtn);
    }

    // Вспомогательный класс для хранения записи
    private record PortalEntry(String code, double x, double y, double z) {}

    @Override
    protected void init() {
        super.init();
        // Сдвигаем центр влево для основных элементов (на 1/4 ширины экрана влево)
        int leftPanelX = this.width / 4;
        int centerY = this.height / 2;

        setupMainUI(leftPanelX, centerY);
        refreshScrollablePanels();
    }

    private void setupMainUI(int centerX, int centerY) {
        // Загрузка текущих данных
        PortalGunComponents.PortalCoords currentCoords = null;
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                this.displayedCode = PortalGun.getPortalCode(stack);
                currentCoords = PortalGun.getTargetCoords(stack);
            }
        }

        // Поля ввода (сдвинуты влево)
        this.codeInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 65, 150, 20, Text.literal("Code"));
        this.codeInput.setMaxLength(12);
        this.codeInput.setText(this.displayedCode);
        this.addDrawableChild(this.codeInput);

        this.xInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 25, 45, 20, Text.literal("X"));
        this.yInput = new TextFieldWidget(this.textRenderer, centerX - 22, centerY - 25, 45, 20, Text.literal("Y"));
        this.zInput = new TextFieldWidget(this.textRenderer, centerX + 31, centerY - 25, 45, 20, Text.literal("Z"));

        if (currentCoords != null) {
            this.xInput.setText(String.valueOf(currentCoords.x()));
            this.yInput.setText(String.valueOf(currentCoords.y()));
            this.zInput.setText(String.valueOf(currentCoords.z()));
        }

        this.xInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));
        this.yInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));
        this.zInput.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));

        this.addDrawableChild(this.xInput);
        this.addDrawableChild(this.yInput);
        this.addDrawableChild(this.zInput);

        // Кнопки управления фокусом
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ввести код"), button -> {
            this.setFocused(this.codeInput);
            this.codeInput.setFocused(true);
        }).dimensions(centerX - 115, centerY + 5, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ввести координаты"), button -> {
            this.setFocused(this.xInput);
            this.xInput.setFocused(true);
        }).dimensions(centerX + 5, centerY + 5, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Телепорт"), button -> submitData())
                .dimensions(centerX - 40, centerY + 35, 80, 20)
                .build());
    }


    private void submitData() {
        String code = this.codeInput.getText();
        double x = parseDouble(xInput.getText());
        double y = parseDouble(yInput.getText());
        double z = parseDouble(zInput.getText());

        if (!code.isEmpty() && client.player != null) {
            // Добавляем в историю
            PortalEntry newEntry = new PortalEntry(code, x, y, z);
            HISTORY.removeIf(e -> e.code.equals(code)); // Удаляем дубликат, если есть
            HISTORY.add(0, newEntry);
            if (HISTORY.size() > MAX_HISTORY) HISTORY.remove(HISTORY.size() - 1);

            // Сохранение в предмет
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                ClientPlayNetworking.send(new PortalCodePayload(code));
                ClientPlayNetworking.send(new PortalCoordsPayload(x, y, z));
                PortalGun.setPortalCode(stack, code);
                PortalGun.setTargetCoords(stack, x, y, z);
                this.close();
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int leftCenterX = this.width / 4;
        int rightX = (int) (this.width * 0.6);
        int listWidth = (int) (this.width * 0.35);
        int centerY = this.height / 2;
        int grayColor = 0xAAAAAA;

        // --- ЛЕВАЯ ПАНЕЛЬ ---
        context.drawCenteredTextWithShadow(this.textRenderer, "Настройка портала", leftCenterX, centerY - 85, 0x00FFFF);
        context.drawText(this.textRenderer, "X", leftCenterX - 75, centerY - 35, grayColor, false);
        context.drawText(this.textRenderer, "Y", leftCenterX - 22, centerY - 35, grayColor, false);
        context.drawText(this.textRenderer, "Z", leftCenterX + 31, centerY - 35, grayColor, false);

        // --- ПРАВАЯ ПАНЕЛЬ (Заголовки) ---
        context.drawTextWithShadow(this.textRenderer, "Избранное", rightX, 10, grayColor);
        context.drawTextWithShadow(this.textRenderer, "История", rightX, centerY, grayColor);

        // --- ОТРИСОВКА ПОЛЗУНКОВ (SCROLLBARS) ---
        // Для избранного (сверху)
        drawScrollBar(context, rightX + listWidth + 2, 25, MAX_VISIBLE_FAV * ENTRY_HEIGHT,
                favScroll, FAVORITES.size(), MAX_VISIBLE_FAV);

        // Для истории (снизу)
        drawScrollBar(context, rightX + listWidth + 2, centerY + 15, MAX_VISIBLE_HIST * ENTRY_HEIGHT,
                histScroll, HISTORY.size(), MAX_VISIBLE_HIST);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.codeInput.isFocused()) {
                this.codeInput.setFocused(false);
                this.xInput.setFocused(true);
                this.setFocused(this.xInput);
            } else if (this.xInput.isFocused()) {
                this.xInput.setFocused(false);
                this.yInput.setFocused(true);
                this.setFocused(this.yInput);
            } else if (this.yInput.isFocused()) {
                this.yInput.setFocused(false);
                this.zInput.setFocused(true);
                this.setFocused(this.zInput);
            } else if (this.zInput.isFocused()) {
                submitData();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}