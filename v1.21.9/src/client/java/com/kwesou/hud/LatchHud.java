package com.kwesou.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class LatchHud {

    private static final int PAD_X = 3;
    private static final int PAD_Y = 2;
    private static final int DEFAULT_BOTTOM_MARGIN = 74;

    private LatchHud() {}

    public static String label(String keys) {
        return "§fLatched: §e[" + keys + "]";
    }

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    public static Bounds bounds(TextRenderer textRenderer, String label, int screenWidth, int screenHeight) {
        HudConfig config = HudConfig.get();
        float scale = config.scale;

        int width  = Math.round((textRenderer.getWidth(label) + PAD_X * 2) * scale);
        int height = Math.round((textRenderer.fontHeight + PAD_Y * 2) * scale);

        int x, y;
        if (config.isDefaultPosition()) {
            x = (screenWidth - width) / 2;
            y = screenHeight - DEFAULT_BOTTOM_MARGIN - height;
        } else {
            float anchorX = config.x * screenWidth;
            x = switch (config.anchor) {
                case LEFT -> Math.round(anchorX);
                case RIGHT -> Math.round(anchorX - width);
                case CENTER -> Math.round(anchorX - width / 2f);
            };
            y = Math.round(config.top * screenHeight);
        }

        // Keep the element fully on screen
        x = Math.max(0, Math.min(x, screenWidth - width));
        y = Math.max(0, Math.min(y, screenHeight - height));

        return new Bounds(x, y, width, height);
    }

    public static void render(DrawContext context, TextRenderer textRenderer, String label, Bounds bounds) {
        float scale = HudConfig.get().scale;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(bounds.x(), bounds.y());
        context.getMatrices().scale(scale, scale);

        int textWidth = textRenderer.getWidth(label);
        context.fill(0, 0, textWidth + PAD_X * 2, textRenderer.fontHeight + PAD_Y * 2, 0x88000000);
        context.drawText(textRenderer, Text.literal(label), PAD_X, PAD_Y, 0xFFFFFFFF, true);

        context.getMatrices().popMatrix();
    }
}
