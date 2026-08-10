package com.example.examplemod.content.data;

public class CreatureDefinition {
    public String id;
    public LocalizedText name;
    public String base_entity = "zombie"; // "zombie", "skeleton", "creeper", "cow", "pig", "spider", "enderman", "iron_golem"
    public String texture; // Texture filename without extension in textures/entity/

    public Attributes attributes = new Attributes();
    public RangedAttack ranged_attack = new RangedAttack();
    public Equipment equipment = new Equipment();
    public SpawnEgg spawn_egg = new SpawnEgg();

    public static class RangedAttack {
        public boolean enabled = false;
        public String weapon = "minecraft:crossbow";
        public String projectile = "minecraft:arrow";
        public int aim_ticks = 20;
        public int reload_time_ticks = 30;
        public boolean can_shoot_while_walking = false;
        public float attack_radius = 16.0f;
        public float projectile_speed = 1.6f;
        public float projectile_inaccuracy = 1.0f;
    }

    public static class Attributes {
        public double max_health = 20.0;
        public double attack_damage = 3.0;
        public double movement_speed = 0.23;
        public double armor = 0.0;
        public double knockback_resistance = 0.0;
        public double follow_range = 32.0;
    }

    public static class Equipment {
        public String mainhand;
        public String offhand;
        public String helmet;
        public String chestplate;
        public String leggings;
        public String boots;
        public float drop_chance = 0.085f;
    }

    public static class SpawnEgg {
        public boolean has_egg = true;
        public String primary_color = "#8B0000";
        public String secondary_color = "#FF4500";
        public String creative_tab = "example_tab";
    }
}
