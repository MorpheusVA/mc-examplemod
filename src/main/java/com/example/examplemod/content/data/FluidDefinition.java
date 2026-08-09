package com.example.examplemod.content.data;

import java.util.List;

public class FluidDefinition {
    public String id;
    public LocalizedText name;
    public LocalizedText description;

    public Rendering rendering = new Rendering();
    public Physics physics = new Physics();
    public Behaviors behaviors = new Behaviors();
    public Bucket bucket = new Bucket();

    public static class Rendering {
        public String tint_color = "#FF40FF40"; // ARGB hex (e.g. #8040FF50)
        public boolean is_translucent = true;
        public Float opacity;     // 0.0 to 1.0 (or 0 to 100)
        public Float transparency; // 0.0 to 1.0 (or 0 to 100)
        public int luminosity = 0; // 0 to 15
        public String use_vanilla_texture; // "lava" or "water"
        public String still_texture;
        public String flow_texture;

        public Integer frametime;       // Ticks per frame (1 = 20fps, 2 = 10fps, 4 = 5fps)
        public Integer still_frametime; // Ticks per frame for still state
        public Integer flow_frametime;  // Ticks per frame for flow state
        public Boolean interpolate = true; // Smooth blending between frames
    }

    public static class Physics {
        public int flow_speed_ticks = 10; // Ticks per block moved (Water = 5, Lava = 30)
        public int flow_distance = 6;     // Max flow distance from source (1 to 8)
        public boolean infinite_source = false; // Whether 2 adjacent sources form a 3rd source
        public int density = 1000;
        public int viscosity = 1000;
        public int temperature = 300;
    }

    public static class Behaviors {
        public boolean catch_fire = false;
        public int fire_seconds = 0;
        public float fire_damage = 0.0f;

        public boolean drowns_player = true;

        public List<StatusEffectConfig> status_effects;
    }

    public List<InteractionConfig> interactions;

    public static class InteractionConfig {
        public String when;        // "hit_by" or "touches"
        public String fluid;       // e.g. "minecraft:water", "minecraft:lava", "examplemod:acid"
        public String this_state = "any";   // "source", "flowing", "any"
        public String target_state = "any"; // "source", "flowing", "any"
        public String replace = "this";    // "this" (default) or "target" / "neighbor"
        public String result = "minecraft:air"; // Block ID to spawn, e.g. "minecraft:sand", "minecraft:air"
    }

    public static class StatusEffectConfig {
        public String effect; // e.g. "minecraft:poison", "minecraft:blindness"
        public int duration_seconds = 1;
        public int amplifier = 0;
        public float linger_seconds = 1.0f; // Remains for X seconds after leaving the fluid
    }

    public List<String> tags;

    public static class Bucket {
        public boolean has_bucket = true;
        public String creative_tab = "example_tab";
        public String texture;
        public Boolean animated_bucket = true; // Toggle animated fluid inside bucket
    }
}
