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
import com.infdimmod.network.UpdatePortalHistoryPayload;
import com.infdimmod.network.UpdatePortalFavoritesPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PortalGunScreen extends Screen {
    private TextFieldWidget codeInput;
    private TextFieldWidget xInput, yInput, zInput;
    private String displayedCode = "";

    private final List<PortalGunComponents.PortalEntry> HISTORY = new ArrayList<>();
    private final List<PortalGunComponents.PortalEntry> FAVORITES = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    private final int ENTRY_HEIGHT = 28;
    private final int MAX_VISIBLE = 5;
    private final int PANEL_WIDTH = 230;
    private final int GAPE = 15;
    private final int INNER_GAPE = 4;
    private double favScroll = 0;
    private double histScroll = 0;
    private final List<ButtonWidget> scrollableButtons = new ArrayList<>();

    public PortalGunScreen() {
        super(Text.translatable("gui.infdimmod.portal_gun.title"));
    }

    @Override
    protected void init() {
        super.init();
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            var historyComp = stack.get(PortalGunComponents.PORTAL_HISTORY);
            if (historyComp != null) {
                this.HISTORY.clear();
                this.HISTORY.addAll(historyComp);
            }
            var favComp = stack.get(PortalGunComponents.PORTAL_FAVORITES);
            if (favComp != null) {
                this.FAVORITES.clear();
                this.FAVORITES.addAll(favComp);
            }
        }
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        setupMainUI(centerX - PANEL_WIDTH - (GAPE / 2), centerY);
        refreshScrollablePanels();
    }

    private void setupMainUI(int leftX, int centerY) {
        PortalGunComponents.PortalCoords currentCoords = null;
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                this.displayedCode = PortalGun.getPortalCode(stack);
                currentCoords = PortalGun.getTargetCoords(stack);
            }
        }

        int buttonSize = 20;
        int textFieldWidth = PANEL_WIDTH - buttonSize - INNER_GAPE;

        // Поле кода
        this.codeInput = new TextFieldWidget(this.textRenderer, leftX, centerY - 65, textFieldWidth, 20, Text.empty());
        this.codeInput.setMaxLength(12);
        this.codeInput.setText(displayedCode);
        this.codeInput.setTextPredicate(s -> s.matches("^[a-zA-Z0-9]*$"));
        this.addDrawableChild(this.codeInput);

        // Кнопка рандома (зазор зафиксирован через INNER_GAPE)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("🎲"), b -> {
            this.codeInput.setText(generateRandomCode(12));
        }).dimensions(leftX + textFieldWidth + INNER_GAPE, centerY - 65, buttonSize, buttonSize).build());

        // Поля координат (распределяем оставшееся место после вычета кнопки и зазора)
        int coordsTotalWidth = textFieldWidth;
        int fieldW = (coordsTotalWidth - 10) / 3; // 10 - это сумма мелких промежутков между X, Y и Z

        this.xInput = new TextFieldWidget(this.textRenderer, leftX, centerY - 25, fieldW, 20, Text.literal("X"));
        this.yInput = new TextFieldWidget(this.textRenderer, leftX + fieldW + 5, centerY - 25, fieldW, 20, Text.literal("Y"));
        this.zInput = new TextFieldWidget(this.textRenderer, leftX + (fieldW + 5) * 2, centerY - 25, fieldW, 20, Text.literal("Z"));

        applyNumericFilter(xInput);
        applyNumericFilter(yInput);
        applyNumericFilter(zInput);

        if (currentCoords != null) {
            this.xInput.setText(formatCoord(currentCoords.x()));
            this.yInput.setText(formatCoord(currentCoords.y()));
            this.zInput.setText(formatCoord(currentCoords.z()));
        }

        this.addDrawableChild(this.xInput);
        this.addDrawableChild(this.yInput);
        this.addDrawableChild(this.zInput);

        // Кнопка "Текущие координаты" (теперь зазор такой же, как у кнопки кода)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("📍"), b -> {
            if (client.player != null) {
                this.xInput.setText(formatCoord(client.player.getX()));
                this.yInput.setText(formatCoord(client.player.getY()));
                this.zInput.setText(formatCoord(client.player.getZ()));
            }
        }).dimensions(leftX + textFieldWidth + INNER_GAPE, centerY - 25, buttonSize, buttonSize).build());

        // Кнопка ввод (теперь точно соответствует общей ширине полей и кнопок выше)
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.infdimmod.portal_gun.submit"), button -> submitData())
                .dimensions(leftX, centerY + 15, PANEL_WIDTH, 20)
                .build());
    }

    private void submitData() {
        String code = this.codeInput.getText();
        if (code.isEmpty()) return;
        double x = parseDouble(xInput.getText());
        double y = parseDouble(yInput.getText());
        double z = parseDouble(zInput.getText());
        if (client.player != null) {
            PortalGunComponents.PortalEntry newEntry = new PortalGunComponents.PortalEntry(code, x, y, z);
            // Исправленная логика истории: удаляем только если совпадает ВСЁ (код и координаты)
            HISTORY.removeIf(e -> e.code().equals(code) &&
                    Math.abs(e.x() - x) < 0.1 &&
                    Math.abs(e.y() - y) < 0.1 &&
                    Math.abs(e.z() - z) < 0.1);
            HISTORY.add(0, newEntry);
            if (HISTORY.size() > MAX_HISTORY) HISTORY.remove(HISTORY.size() - 1);
            ClientPlayNetworking.send(new PortalCodePayload(code));
            ClientPlayNetworking.send(new PortalCoordsPayload(x, y, z));
            ClientPlayNetworking.send(new UpdatePortalHistoryPayload(new ArrayList<>(HISTORY)));
            ClientPlayNetworking.send(new UpdatePortalFavoritesPayload(new ArrayList<>(FAVORITES)));
            this.close();
        }
    }

    private void refreshScrollablePanels() {
        scrollableButtons.forEach(this::remove);
        scrollableButtons.clear();

        int rightX = this.width / 2 + (GAPE / 2);
        int centerY = this.height / 2;

        renderList(FAVORITES, rightX, 30, (int) favScroll, MAX_VISIBLE);
        renderList(HISTORY, rightX, centerY + 20, (int) histScroll, MAX_VISIBLE);
    }

    private void renderList(List<PortalGunComponents.PortalEntry> list, int x, int startY, int scroll, int maxVisible) {
        for (int i = 0; i < list.size(); i++) {
            int yPos = startY + (i * ENTRY_HEIGHT) - scroll;
            if (yPos >= startY && (yPos + ENTRY_HEIGHT - 2) <= (startY + maxVisible * ENTRY_HEIGHT)) {
                addScrollableEntry(x, yPos, list.get(i));
            }
        }
    }

    private void addScrollableEntry(int x, int y, PortalGunComponents.PortalEntry entry) {
        // Основная кнопка (пустая, текст рисуем вручную в render для контроля шрифта)
        ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
            this.codeInput.setText(entry.code());
            this.xInput.setText(formatCoord(entry.x()));
            this.yInput.setText(formatCoord(entry.y()));
            this.zInput.setText(formatCoord(entry.z()));
        }).dimensions(x, y, PANEL_WIDTH - 25, ENTRY_HEIGHT - 2).build();

        boolean isFav = FAVORITES.stream().anyMatch(e -> e.code().equals(entry.code()) && e.x() == entry.x());
        ButtonWidget starBtn = ButtonWidget.builder(Text.literal(isFav ? "★" : "☆"), b -> {
            if (isFav) FAVORITES.removeIf(e -> e.code().equals(entry.code()) && e.x() == entry.x());
            else FAVORITES.add(0, entry);
            refreshScrollablePanels();
        }).dimensions(x + PANEL_WIDTH - 22, y, 20, ENTRY_HEIGHT - 2).build();

        this.addDrawableChild(btn);
        this.addDrawableChild(starBtn);
        scrollableButtons.add(btn);
        scrollableButtons.add(starBtn);
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private void applyNumericFilter(TextFieldWidget widget) {
        widget.setTextPredicate(s -> s.matches("^-?\\d*(\\.\\d{0,2})?$"));
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value.replace(',', '.')); } catch (Exception e) { return 0.0; }
    }

    private String formatCoord(double val) {
        return String.format(java.util.Locale.ROOT, "%.1f", val);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int leftX = centerX - PANEL_WIDTH - (GAPE / 2);
        int rightX = centerX + (GAPE / 2);
        int centerY = this.height / 2;

        // Заголовки
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.infdimmod.portal_gun.settings"), leftX + PANEL_WIDTH/2, centerY - 85, 0x00FFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.infdimmod.portal_gun.favorites"), rightX, 15, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.infdimmod.portal_gun.history"), rightX, centerY + 5, 0xAAAAAA);

        // Ручная отрисовка текста в кнопках истории (для двух строчек и разного размера)
        renderEntryLabels(context, FAVORITES, rightX, 30, (int) favScroll);
        renderEntryLabels(context, HISTORY, rightX, centerY + 20, (int) histScroll);
    }

    private void renderEntryLabels(DrawContext context, List<PortalGunComponents.PortalEntry> list, int x, int startY, int scroll) {
        for (int i = 0; i < list.size(); i++) {
            int yPos = startY + (i * ENTRY_HEIGHT) - scroll;
            if (yPos >= startY && (yPos + ENTRY_HEIGHT - 5) <= (startY + MAX_VISIBLE * ENTRY_HEIGHT)) {
                PortalGunComponents.PortalEntry e = list.get(i);
                // Код (Обычный шрифт)
                context.drawText(this.textRenderer, e.code(), x + 5, yPos + 3, 0xFFFFFF, false);

                // Координаты (Мелкий шрифт)
                context.getMatrices().push();
                context.getMatrices().translate(x + 5, yPos + 14, 0);
                context.getMatrices().scale(0.75f, 0.75f, 1.0f);
                String coords = String.format("X: %.1f Y: %.1f Z: %.1f", e.x(), e.y(), e.z());
                context.drawText(this.textRenderer, coords, 0, 0, 0xAAAAAA, false);
                context.getMatrices().pop();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double delta = verticalAmount * 10;
        if (mouseY < this.height / 2) favScroll = clampScroll(favScroll - delta, FAVORITES.size());
        else histScroll = clampScroll(histScroll - delta, HISTORY.size());
        refreshScrollablePanels();
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private double clampScroll(double current, int size) {
        int max = Math.max(0, (size - MAX_VISIBLE) * ENTRY_HEIGHT);
        return Math.max(0, Math.min(current, max));
    }

    @Override
    public boolean shouldPause() { return false; }
}