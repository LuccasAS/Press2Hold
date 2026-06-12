package com.kwesou;

import com.kwesou.common.HoldManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Press2HoldClient implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static final HoldManager manager = new HoldManager();

    private final Map<String, Boolean> prevPhysicalState = new HashMap<>();

    @Override
    public void onInitializeClient() {

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.press2hold.latch",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_G,
                KeyBinding.Category.create(Identifier.of("press2hold:press2hold"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onTick(MinecraftClient client) {
        long windowHandle = client.getWindow().getHandle();

        if (manager.isLatched()) {
            for (KeyBinding key : client.options.allKeys) {
                String keyName = key.getBoundKeyLocalizedText().getString();
                if (manager.getKeys().contains(keyName)) {
                    boolean nowPressed = isPhysicallyPressed(windowHandle, key);
                    boolean wasPressed = prevPhysicalState.getOrDefault(keyName, false);

                    if (nowPressed && !wasPressed) {
                        releaseKeys(client);
                        manager.toggle();
                        manager.clear();
                        prevPhysicalState.clear();
                        return;
                    }
                }
            }
        }

        if (manager.isLatched()) {
            for (KeyBinding key : client.options.allKeys) {
                String keyName = key.getBoundKeyLocalizedText().getString();
                if (manager.getKeys().contains(keyName)) {
                    prevPhysicalState.put(keyName, isPhysicallyPressed(windowHandle, key));
                }
            }
        }

        while (toggleKey.wasPressed()) {

            boolean latched = manager.toggle();

            if (latched) {
                Set<String> keys = capturePressedKeys(client);
                manager.setKeys(keys);

                if (manager.isEmpty()) {
                    manager.toggle();
                    sendMessage(client, "No valid inputs pressed");
                } else {
                    for (KeyBinding key : client.options.allKeys) {
                        String keyName = key.getBoundKeyLocalizedText().getString();
                        if (manager.getKeys().contains(keyName)) {
                            prevPhysicalState.put(keyName, true);
                        }
                    }
                }

            } else {
                releaseKeys(client);
                manager.clear();
                prevPhysicalState.clear();
            }
        }

        if (manager.isLatched()) {
            pressKeys(client);
        }
    }

    private boolean isPhysicallyPressed(long windowHandle, KeyBinding key) {
        InputUtil.Key boundKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
        int code = boundKey.getCode();

        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, code) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS;
        }
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!manager.isLatched() || manager.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        String label = "§fLatched: §e[" + manager.formatKeys() + "]";
        int textWidth = textRenderer.getWidth(label);

        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 49;

        context.fill(x - 3, y - 2, x + textWidth + 3, y + 10, 0x88000000);
        context.drawText(textRenderer, Text.literal(label), x, y, 0xFFFFFFFF, true);
    }

    private Set<String> capturePressedKeys(MinecraftClient client) {
        Set<String> keys = new HashSet<>();

        for (KeyBinding key : client.options.allKeys) {
            if (key.isPressed() && !key.equals(toggleKey)) {
                keys.add(key.getBoundKeyLocalizedText().getString());
            }
        }

        return keys;
    }

    private void releaseKeys(MinecraftClient client) {
        long win = client.getWindow().getHandle();
        for (KeyBinding key : client.options.allKeys) {
            if (!isPhysicallyPressed(win, key)) {
                key.setPressed(false);
            }
        }
    }

    private void pressKeys(MinecraftClient client) {
        for (KeyBinding key : client.options.allKeys) {
            if (manager.getKeys().contains(key.getBoundKeyLocalizedText().getString())) {
                key.setPressed(true);
            }
        }
    }

    private void sendMessage(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.of(msg), false);
        }
    }
}