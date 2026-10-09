package com.kwesou.hud;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class HudEditorScreen extends Screen {

    private static final int SNAP_DISTANCE = 4;
    private static final float SCALE_STEP = 0.1f;

    private final Supplier<String> keysSupplier;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;
    private boolean snappedToCenter;

    public HudEditorScreen(Supplier<String> keysSupplier) {
        super(Text.translatable("press2hold.hud_editor.title"));
        this.keysSupplier = keysSupplier;
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int y = height / 2 + 30;

        addDrawableChild(ButtonWidget.builder(Text.translatable("press2hold.hud_editor.reset"), button -> {
            HudConfig.get().reset();
            HudConfig.save();
        }).dimensions(width / 2 - buttonWidth - 2, y, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 + 2, y, buttonWidth, 20).build());
    }

    private String currentLabel() {
        String keys = keysSupplier.get();
        return LatchHud.label(keys.isEmpty() ? "W, Left Shift" : keys);
    }

    private LatchHud.Bounds currentBounds() {
        return LatchHud.bounds(textRenderer, currentLabel(), width, height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("press2hold.hud_editor.hint"),
                width / 2, height / 2 - 6, 0xFFAAAAAA);

        if (dragging && snappedToCenter) {
            context.drawVerticalLine(width / 2, 0, height, 0x8800FFFF);
        }

        String label = currentLabel();
        LatchHud.Bounds bounds = currentBounds();
        LatchHud.render(context, textRenderer, label, bounds);

        boolean hovered = bounds.contains(mouseX, mouseY);
        int outline = dragging ? 0xFFFFFF55 : hovered ? 0xFFFFFFFF : 0x88FFFFFF;
        context.drawStrokedRectangle(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, outline);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;

        LatchHud.Bounds bounds = currentBounds();
        if (click.button() == 0 && bounds.contains(click.x(), click.y())) {
            dragging = true;
            grabOffsetX = click.x() - bounds.x();
            grabOffsetY = click.y() - bounds.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (!dragging) return super.mouseDragged(click, offsetX, offsetY);

        LatchHud.Bounds bounds = currentBounds();
        double left = click.x() - grabOffsetX;
        double top = click.y() - grabOffsetY;

        // Clamp so the element can't leave the screen
        left = Math.max(0, Math.min(left, width - bounds.width()));
        top = Math.max(0, Math.min(top, height - bounds.height()));

        double centerOffset = left + bounds.width() / 2.0 - width / 2.0;
        snappedToCenter = Math.abs(centerOffset) <= SNAP_DISTANCE;
        if (snappedToCenter) left -= centerOffset;

        HudConfig.get().place(left, top, bounds.width(), width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging && click.button() == 0) {
            dragging = false;
            snappedToCenter = false;
            HudConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        LatchHud.Bounds bounds = currentBounds();
        if (verticalAmount == 0 || !bounds.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        HudConfig config = HudConfig.get();
        if (config.isDefaultPosition()) {
            // Pin the current position so resizing doesn't jump back to the default anchor
            config.place(bounds.x(), bounds.y(), bounds.width(), width, height);
        }

        float newScale = config.scale + (verticalAmount > 0 ? SCALE_STEP : -SCALE_STEP);
        newScale = Math.round(newScale * 10f) / 10f;
        config.scale = Math.max(HudConfig.MIN_SCALE, Math.min(HudConfig.MAX_SCALE, newScale));
        HudConfig.save();
        return true;
    }

    @Override
    public void close() {
        HudConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
