package com.example.examplemod.content.creature;

import com.example.examplemod.content.data.CreatureDefinition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class DynamicCreatureEntity extends Monster {
    private final CreatureDefinition definition;

    public DynamicCreatureEntity(EntityType<? extends Monster> type, Level level, CreatureDefinition definition) {
        super(type, level);
        this.definition = definition;
    }

    public CreatureDefinition getDefinition() {
        return definition;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        String base = definition != null && definition.base_entity != null ? definition.base_entity.toLowerCase() : "zombie";

        if ("cow".equals(base) || "pig".equals(base) || "animal".equals(base) || "passive".equals(base)) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
            this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        } else {
            // Aggressive / Monster AI (Zombie, Skeleton, Creeper, Spider, Iron Golem, etc.)
            this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    public static AttributeSupplier.Builder createAttributes(CreatureDefinition def) {
        double hp = def.attributes != null ? Math.max(1.0, def.attributes.max_health) : 20.0;
        double dmg = def.attributes != null ? Math.max(0.0, def.attributes.attack_damage) : 3.0;
        double spd = def.attributes != null ? Math.max(0.01, def.attributes.movement_speed) : 0.23;
        double armor = def.attributes != null ? Math.max(0.0, def.attributes.armor) : 0.0;
        double kb = def.attributes != null ? def.attributes.knockback_resistance : 0.0;
        double follow = def.attributes != null ? Math.max(4.0, def.attributes.follow_range) : 32.0;

        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, hp)
                .add(Attributes.ATTACK_DAMAGE, dmg)
                .add(Attributes.MOVEMENT_SPEED, spd)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, kb)
                .add(Attributes.FOLLOW_RANGE, follow);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (definition != null && definition.equipment != null) {
            CreatureDefinition.Equipment eq = definition.equipment;

            setItemFromConfig(EquipmentSlot.MAINHAND, eq.mainhand);
            setItemFromConfig(EquipmentSlot.OFFHAND, eq.offhand);
            setItemFromConfig(EquipmentSlot.HEAD, eq.helmet);
            setItemFromConfig(EquipmentSlot.CHEST, eq.chestplate);
            setItemFromConfig(EquipmentSlot.LEGS, eq.leggings);
            setItemFromConfig(EquipmentSlot.FEET, eq.boots);

            float dropChance = eq.drop_chance > 0 ? eq.drop_chance : 0.085f;
            this.setDropChance(EquipmentSlot.MAINHAND, dropChance);
            this.setDropChance(EquipmentSlot.OFFHAND, dropChance);
            this.setDropChance(EquipmentSlot.HEAD, dropChance);
            this.setDropChance(EquipmentSlot.CHEST, dropChance);
            this.setDropChance(EquipmentSlot.LEGS, dropChance);
            this.setDropChance(EquipmentSlot.FEET, dropChance);
        }
        return spawnGroupData;
    }

    private void setItemFromConfig(EquipmentSlot slot, String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        try {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != null && item != Items.AIR) {
                this.setItemSlot(slot, new ItemStack(item));
            }
        } catch (Exception ignored) {}
    }
}
