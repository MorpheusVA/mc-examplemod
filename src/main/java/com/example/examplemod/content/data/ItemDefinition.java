package com.example.examplemod.content.data;

import java.util.List;

public class ItemDefinition {
    public String id;
    public String name;
    public String description;
    public String type = "basic"; // "basic", "food"
    public int max_stack_size = 64;
    public String rarity = "common"; // "common", "uncommon", "rare", "epic"
    public FoodProperties food;
    public String creative_tab = "example_tab";
    public List<String> extra_tooltips;

    public static class FoodProperties {
        public int nutrition = 4;
        public float saturation = 0.6f;
        public boolean always_edible = false;
        public boolean fast_food = false;
    }
}
