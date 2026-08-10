package com.example.examplemod.content.creature;

import com.example.examplemod.content.data.CreatureDefinition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class DynamicCreatureEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_CROSSBOW = SynchedEntityData.defineId(DynamicCreatureEntity.class, EntityDataSerializers.BOOLEAN);

    private final CreatureDefinition definition;

    public DynamicCreatureEntity(EntityType<? extends Monster> type, Level level, CreatureDefinition definition) {
        super(type, level);
        this.definition = definition;
    }

    public CreatureDefinition getDefinition() {
        return definition;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING_CROSSBOW, false);
    }

    public boolean isChargingCrossbow() {
        return this.entityData.get(DATA_CHARGING_CROSSBOW);
    }

    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_CROSSBOW, charging);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount == 1) {
            applyDefaultEquipment();
        }
    }

    public void applyDefaultEquipment() {
        if (definition != null && definition.equipment != null) {
            CreatureDefinition.Equipment eq = definition.equipment;
            if (getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && eq.mainhand != null) {
                setItemFromConfig(EquipmentSlot.MAINHAND, eq.mainhand);
            }
            if (getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && eq.offhand != null) {
                setItemFromConfig(EquipmentSlot.OFFHAND, eq.offhand);
            }
            if (getItemBySlot(EquipmentSlot.HEAD).isEmpty() && eq.helmet != null) {
                setItemFromConfig(EquipmentSlot.HEAD, eq.helmet);
            }
            if (getItemBySlot(EquipmentSlot.CHEST).isEmpty() && eq.chestplate != null) {
                setItemFromConfig(EquipmentSlot.CHEST, eq.chestplate);
            }
            if (getItemBySlot(EquipmentSlot.LEGS).isEmpty() && eq.leggings != null) {
                setItemFromConfig(EquipmentSlot.LEGS, eq.leggings);
            }
            if (getItemBySlot(EquipmentSlot.FEET).isEmpty() && eq.boots != null) {
                setItemFromConfig(EquipmentSlot.FEET, eq.boots);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        String base = definition != null && definition.base_entity != null ? definition.base_entity.toLowerCase() : "zombie";
        boolean isRanged = "pillager".equals(base) || "illager".equals(base) || "ranged".equals(base)
                || (definition != null && definition.ranged_attack != null && definition.ranged_attack.enabled);
        if (isRanged) {
            return false; // Ranged entities do not deal melee damage on touch!
        }
        return super.doHurtTarget(target);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        String base = definition != null && definition.base_entity != null ? definition.base_entity.toLowerCase() : "zombie";
        boolean isRanged = "pillager".equals(base) || "illager".equals(base) || "ranged".equals(base)
                || (definition != null && definition.ranged_attack != null && definition.ranged_attack.enabled);

        if (isRanged) {
            CreatureDefinition.RangedAttack config = (definition != null && definition.ranged_attack != null) ? definition.ranged_attack : new CreatureDefinition.RangedAttack();
            this.goalSelector.addGoal(1, new DynamicRangedAttackGoal(this, config));
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else if ("cow".equals(base) || "pig".equals(base) || "animal".equals(base) || "passive".equals(base)) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
            this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        } else {
            // Aggressive / Melee Monster AI
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

    public static class DynamicRangedAttackGoal extends Goal {
        private final DynamicCreatureEntity mob;
        private final CreatureDefinition.RangedAttack config;
        private int attackTime = -1;
        private int aimTimer = 0;
        private int seeTime = 0;

        public DynamicRangedAttackGoal(DynamicCreatureEntity mob, CreatureDefinition.RangedAttack config) {
            this.mob = mob;
            this.config = config;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.mob.setAggressive(false);
            this.mob.setChargingCrossbow(false);
            this.aimTimer = 0;
            this.attackTime = -1;
            this.seeTime = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;

            double distSq = mob.distanceToSqr(target);
            double maxDistSq = config.attack_radius * config.attack_radius;
            boolean canSee = mob.getSensing().hasLineOfSight(target);

            if (canSee) {
                this.seeTime++;
            } else {
                this.seeTime = 0;
            }

            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            boolean canWalk = config.can_shoot_while_walking;

            if (distSq <= maxDistSq && this.seeTime >= 5) {
                if (!canWalk) {
                    mob.getNavigation().stop();
                } else {
                    if (distSq < 25.0D) {
                        mob.getNavigation().stop();
                    } else {
                        mob.getNavigation().moveTo(target, 0.8D);
                    }
                }

                if (attackTime <= 0) {
                    mob.setChargingCrossbow(true);
                    aimTimer++;
                    if (aimTimer >= config.aim_ticks) {
                        shootProjectile(target);
                        mob.setChargingCrossbow(false);
                        aimTimer = 0;
                        attackTime = config.reload_time_ticks;
                    }
                } else {
                    attackTime--;
                }
            } else {
                mob.setChargingCrossbow(false);
                aimTimer = 0;
                mob.getNavigation().moveTo(target, 1.0D);
            }
        }

        private void shootProjectile(LivingEntity target) {
            Level level = mob.level();
            float speed = config.projectile_speed;
            float inaccuracy = config.projectile_inaccuracy;

            String projId = config.projectile != null ? config.projectile.toLowerCase() : "minecraft:arrow";

            double spawnX = mob.getX();
            double spawnY = mob.getEyeY() - 0.15D;
            double spawnZ = mob.getZ();

            double d0 = target.getX() - spawnX;
            double d1 = target.getY(0.333D) - spawnY;
            double d2 = target.getZ() - spawnZ;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);

            if ("minecraft:firework_rocket".equals(projId)) {
                ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                FireworkRocketEntity firework = new FireworkRocketEntity(level, fireworkStack, mob, spawnX, spawnY, spawnZ, true);
                firework.shoot(d0, d1 + d3 * 0.15D, d2, speed, inaccuracy);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
                level.addFreshEntity(firework);
            } else if ("minecraft:spectral_arrow".equals(projId)) {
                SpectralArrow arrow = new SpectralArrow(level, spawnX, spawnY, spawnZ, new ItemStack(Items.SPECTRAL_ARROW), new ItemStack(Items.CROSSBOW));
                arrow.setOwner(mob);
                arrow.shoot(d0, d1 + d3 * 0.2D, d2, speed, inaccuracy);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
                level.addFreshEntity(arrow);
            } else {
                Arrow arrow = new Arrow(level, spawnX, spawnY, spawnZ, new ItemStack(Items.ARROW), new ItemStack(Items.CROSSBOW));
                arrow.setOwner(mob);
                arrow.shoot(d0, d1 + d3 * 0.2D, d2, speed, inaccuracy);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
                level.addFreshEntity(arrow);
            }
        }
    }
}
