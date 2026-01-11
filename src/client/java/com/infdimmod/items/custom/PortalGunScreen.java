package com.infdimmod.items.custom;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PortalGunScreen extends Screen {

    public PortalGunScreen() {
        super(Text.literal("Portal Gun"));
    }

    @Override
    protected void init() {
        super.init();
        // Add a single button that closes the screen
        int buttonWidth = 100;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = (this.height - buttonHeight) / 2 + 20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
