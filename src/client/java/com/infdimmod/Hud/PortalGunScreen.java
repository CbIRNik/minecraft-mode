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

    // Параметры скроллинга
    private double favScroll = 0;
    private double histScroll = 0;
    private final int ENTRY_HEIGHT = 22;
    private final int MAX_VISIBLE_FAV = 5;
    private final int MAX_VISIBLE_HIST = 5;
    private final List<ButtonWidget> scrollableButtons = new ArrayList<>();

    public PortalGunScreen() {
        super(Text.literal("Portal Gun"));
    }

    @Override
    protected void init() {
        super.init();
        if (client.player != null && client.world != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (!stack.isEmpty()) {
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
        }
        int leftPanelX = this.width / 4;
        int centerY = this.height / 2;
        setupMainUI(leftPanelX, centerY);
        refreshScrollablePanels();
    }

    private void setupMainUI(int centerX, int centerY) {
        PortalGunComponents.PortalCoords currentCoords = null;
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                this.displayedCode = PortalGun.getPortalCode(stack);
                currentCoords = PortalGun.getTargetCoords(stack);
            }
        }

        // Поле кода
        this.codeInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 65, 125, 20, Text.translatable("Code"));
        this.codeInput.setMaxLength(12);
        this.codeInput.setText(this.displayedCode);
        this.addDrawableChild(this.codeInput);

        // Кнопка рандома
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("🎲"), b -> {
            this.codeInput.setText(generateRandomCode(8));
        }).dimensions(centerX + 55, centerY - 65, 20, 20).build());

        // Поля координат
        this.xInput = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 25, 40, 20, Text.translatable("X"));
        this.yInput = new TextFieldWidget(this.textRenderer, centerX - 30, centerY - 25, 40, 20, Text.translatable("Y"));
        this.zInput = new TextFieldWidget(this.textRenderer, centerX + 15, centerY - 25, 40, 20, Text.translatable("Z"));

        applyNumericFilter(xInput);
        applyNumericFilter(yInput);
        applyNumericFilter(zInput);

        if (currentCoords != null) {
            this.xInput.setText(String.format("%.1f", currentCoords.x()).replace(',', '.'));
            this.yInput.setText(String.format("%.1f", currentCoords.y()).replace(',', '.'));
            this.zInput.setText(String.format("%.1f", currentCoords.z()).replace(',', '.'));
        }

        this.addDrawableChild(this.xInput);
        this.addDrawableChild(this.yInput);
        this.addDrawableChild(this.zInput);

        // Кнопка "Текущие координаты"
        this.addDrawableChild(ButtonWidget.builder(Text.literal("📍"), b -> {
            if (client.player != null) {
                this.xInput.setText(String.format(java.util.Locale.ROOT, "%.1f", client.player.getX()));
                this.yInput.setText(String.format(java.util.Locale.ROOT, "%.1f", client.player.getY()));
                this.zInput.setText(String.format(java.util.Locale.ROOT, "%.1f", client.player.getZ()));
            }
        }).dimensions(centerX + 55, centerY - 25, 20, 20).build());

        // Кнопка ввод
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("Ввод"), button -> submitData())
                .dimensions(centerX - 40, centerY + 20, 80, 20)
                .build());
    }

    private void submitData() {
        String code = this.codeInput.getText();
        if (code.isEmpty()) return;
        double x = parseDouble(xInput.getText());
        double y = parseDouble(yInput.getText());
        double z = parseDouble(zInput.getText());
        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof PortalGun) {
                PortalGunComponents.PortalEntry newEntry = new PortalGunComponents.PortalEntry(code, x, y, z);
                HISTORY.removeIf(e -> e.code().equals(code));
                HISTORY.add(0, newEntry);
                if (HISTORY.size() > MAX_HISTORY) {
                    HISTORY.remove(HISTORY.size() - 1);
                }
                ClientPlayNetworking.send(new PortalCodePayload(code));
                ClientPlayNetworking.send(new PortalCoordsPayload(x, y, z));
                ClientPlayNetworking.send(new UpdatePortalHistoryPayload(new ArrayList<>(HISTORY)));
                ClientPlayNetworking.send(new UpdatePortalFavoritesPayload(new ArrayList<>(FAVORITES)));
                this.close();
            }
        }
    }

    private void refreshScrollablePanels() {
        scrollableButtons.forEach(this::remove);
        scrollableButtons.clear();

        int rightX = (int) (this.width * 0.55);
        int listWidth = (int) (this.width * 0.42);
        int centerY = this.height / 2;

        renderListWithScissor(FAVORITES, rightX, 25, (int) favScroll, MAX_VISIBLE_FAV, listWidth);
        renderListWithScissor(HISTORY, rightX, centerY + 15, (int) histScroll, MAX_VISIBLE_HIST, listWidth);
    }

    private void renderListWithScissor(List<PortalGunComponents.PortalEntry> list, int x, int startY, int scroll, int maxVisible, int width) {
        int listHeight = maxVisible * ENTRY_HEIGHT;

        for (int i = 0; i < list.size(); i++) {
            int yPos = startY + (i * ENTRY_HEIGHT) - scroll;
            if (yPos >= startY && (yPos + 20) <= (startY + listHeight)) {
                addScrollableEntry(x, yPos, width, list.get(i));
            }
        }
    }

    private void renderList(List<PortalGunComponents.PortalEntry> list, int x, int startY, int scroll, int maxVisible, int width) {
        for (int i = 0; i < list.size(); i++) {
            int yPos = startY + (i * ENTRY_HEIGHT) - scroll;
            if (yPos >= startY - 10 && yPos < startY + (maxVisible * ENTRY_HEIGHT)) {
                addScrollableEntry(x, yPos, width, list.get(i));
            }
        }
    }

    private void addScrollableEntry(int x, int y, int width, PortalGunComponents.PortalEntry entry) {
        // Форматируем текст для отображения и кода, и координат
        String displayString = String.format("%s | %.0f, %.0f, %.0f", entry.code(), entry.x(), entry.y(), entry.z());

        ButtonWidget btn = ButtonWidget.builder(Text.translatable(displayString), b -> {
            this.codeInput.setText(entry.code());
            this.xInput.setText(String.valueOf(entry.x()));
            this.yInput.setText(String.valueOf(entry.y()));
            this.zInput.setText(String.valueOf(entry.z()));
        }).dimensions(x, y, width - 25, 20).build();

        boolean isFav = FAVORITES.stream().anyMatch(e -> e.code().equals(entry.code()));
        ButtonWidget starBtn = ButtonWidget.builder(Text.translatable(isFav ? "★" : "☆"), b -> {
            if (isFav) FAVORITES.removeIf(e -> e.code().equals(entry.code()));
            else FAVORITES.add(0, entry);
            refreshScrollablePanels();
        }).dimensions(x + width - 22, y, 20, 20).build();

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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int leftCenterX = this.width / 4;
        int rightX = (int) (this.width * 0.55);
        int listWidth = (int) (this.width * 0.42);
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "Настройка портала", leftCenterX, centerY - 85, 0x00FFFF);
        context.drawTextWithShadow(this.textRenderer, "Избранное", rightX, 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "История", rightX, centerY, 0xAAAAAA);

        drawScrollBar(context, rightX + listWidth + 2, 25, MAX_VISIBLE_FAV * ENTRY_HEIGHT, favScroll, FAVORITES.size(), MAX_VISIBLE_FAV);
        drawScrollBar(context, rightX + listWidth + 2, centerY + 15, MAX_VISIBLE_HIST * ENTRY_HEIGHT, histScroll, HISTORY.size(), MAX_VISIBLE_HIST);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double scrollDelta = verticalAmount * 7;
        if (mouseY < this.height / 2) {
            favScroll = clampScroll(favScroll - scrollDelta, FAVORITES.size(), MAX_VISIBLE_FAV);
        } else {
            histScroll = clampScroll(histScroll - scrollDelta, HISTORY.size(), MAX_VISIBLE_HIST);
        }
        refreshScrollablePanels();
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private double clampScroll(double current, int size, int visible) {
        int max = Math.max(0, (size - visible) * ENTRY_HEIGHT);
        return Math.max(0, Math.min(current, max));
    }

    private void drawScrollBar(DrawContext context, int x, int y, int height, double scroll, int total, int visible) {
        if (total <= visible) return;
        int barHeight = (int) ((double) visible / total * height);
        int barPos = (int) (scroll / (total * ENTRY_HEIGHT) * height);
        context.fill(x, y, x + 2, y + height, 0x80000000);
        context.fill(x, y + barPos, x + 2, y + barPos + barHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}