package com.example.examplemod.content.data;

import java.util.List;

public class ItemDefinition {
    public String id;
    public LocalizedText name;
    public LocalizedText description;
    public String type = "basic"; // "basic", "food"
    public int max_stack_size = 64;
    public String rarity = "common"; // "common", "uncommon", "rare", "epic"
    public FoodProperties food;
    public CrossbowProperties crossbow;
    public String creative_tab = "example_tab";
    public List<String> extra_tooltips;
    public List<String> tags;

    public static class FoodProperties {
        public int nutrition = 4;
        public float saturation = 0.6f;
        public boolean always_edible = false;
        public boolean fast_food = false;
    }

    public static class CrossbowProperties {
        public String reload_sound = "minecraft:item.crossbow.loading_start";
        public String shoot_sound = "minecraft:item.crossbow.shoot";
    }
}
