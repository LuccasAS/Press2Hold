package com.kwesou.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kwesou.Press2Hold;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;


public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("press2hold.json");

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;

    public enum Anchor { LEFT, CENTER, RIGHT }

    private static HudConfig instance;

    public float x = -1;
    public Anchor anchor = Anchor.CENTER;
    public float top = -1;
    public float scale = 1.0f;

    private Float centerX;

    public static HudConfig get() {
        if (instance == null) load();
        return instance;
    }

    public boolean isDefaultPosition() {
        return x < 0 || top < 0;
    }

    public void reset() {
        x = -1;
        anchor = Anchor.CENTER;
        top = -1;
        scale = 1.0f;
    }

    public void place(double left, double top, int width, int screenWidth, int screenHeight) {
        double center = left + width / 2.0;
        if (center < screenWidth / 3.0) {
            anchor = Anchor.LEFT;
            x = (float) (left / screenWidth);
        } else if (center > screenWidth * 2 / 3.0) {
            anchor = Anchor.RIGHT;
            x = (float) ((left + width) / screenWidth);
        } else {
            anchor = Anchor.CENTER;
            x = (float) (center / screenWidth);
        }
        this.top = (float) (top / screenHeight);
    }

    public static void load() {
        instance = new HudConfig();
        if (!Files.exists(PATH)) return;

        try (Reader reader = Files.newBufferedReader(PATH)) {
            HudConfig loaded = GSON.fromJson(reader, HudConfig.class);
            if (loaded != null) instance = loaded;
        } catch (Exception e) {
            Press2Hold.LOGGER.warn("Could not read {}, using defaults", PATH, e);
        }
        if (instance.centerX != null) {
            if (instance.x < 0) {
                instance.x = instance.centerX;
                instance.anchor = Anchor.CENTER;
            }
            instance.centerX = null;
        }
        if (instance.anchor == null) instance.anchor = Anchor.CENTER;
        instance.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, instance.scale));
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            Press2Hold.LOGGER.warn("Could not write {}", PATH, e);
        }
    }
}
