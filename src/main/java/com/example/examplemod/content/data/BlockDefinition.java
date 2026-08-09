package com.example.examplemod.content.data;

import java.util.List;

public class BlockDefinition {
    public String id;
    public String name;
    public String description;
    public float destroy_time = 1.5f;
    public float explosion_resistance = 6.0f;
    public int light_emission = 0;
    public String sound_type = "stone"; // "stone", "wood", "metal", "glass", "grass", "gravel", "sand", "wool", "deepslate"
    public String map_color = "stone";
    public boolean requires_tool = true;
    public String waila_info;
    public boolean has_item = true;
    public String creative_tab = "example_tab";
    public List<String> extra_tooltips;

    // Rotation / Placement constraint:
    // "none" (static, no rotation)
    // "horizontal" (rotates horizontally facing player, cannot be placed upside-down)
    // "directional" / "all" (can face all 6 directions: up, down, north, south, east, west)
    // "axis" / "pillar" (rotates on X, Y, Z axis like logs/pillars)
    public String rotation = "none";

    // Optional single texture shorthand (if omitted, defaults to block id)
    public String texture;

    // Multi-face textures configuration (all 6 faces completely independent)
    public BlockTextures textures;

    public static class BlockTextures {
        // Universal (all 6 faces same)
        public String all;

        // 6 Exact individual Minecraft faces
        public String up;     // Top face (Y+)
        public String down;   // Bottom face (Y-)
        public String north;  // North face (Z-)
        public String south;  // South face (Z+)
        public String east;   // East face (X+)
        public String west;   // West face (X-)
        public String particle;

        // Optional friendly aliases
        public String top;    // alias for up
        public String bottom; // alias for down
        public String front;  // alias for north
        public String back;   // alias for south
        public String right;  // alias for east
        public String left;   // alias for west
        public String side;   // alias for north, south, east, west
    }
}
