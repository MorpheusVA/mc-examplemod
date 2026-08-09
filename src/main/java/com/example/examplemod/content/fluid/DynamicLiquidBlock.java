package com.example.examplemod.content.fluid;

import com.example.examplemod.content.data.FluidDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class DynamicLiquidBlock extends LiquidBlock {
    private final FluidDefinition definition;

    public DynamicLiquidBlock(FlowingFluid fluid, FluidDefinition definition) {
        super(fluid, BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(net.minecraft.world.level.block.SoundType.EMPTY)
                .lightLevel(state -> definition.rendering.luminosity));
        this.definition = definition;
    }

    public FluidDefinition getDefinition() {
        return definition;
    }

    public void updateDefinition(FluidDefinition newDef) {
        if (newDef != null) {
            this.definition.rendering = newDef.rendering;
            this.definition.physics = newDef.physics;
            this.definition.behaviors = newDef.behaviors;
            this.definition.interactions = newDef.interactions;
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!checkFluidInteractions(level, pos, state)) {
            super.onPlace(state, level, pos, oldState, isMoving);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        if (!checkFluidInteractions(level, pos, state)) {
            super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        }
    }

    private boolean checkFluidInteractions(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || definition.interactions == null || definition.interactions.isEmpty()) {
            return false;
        }

        boolean thisIsSource = state.getFluidState().isSource();

        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            net.minecraft.world.level.material.FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.isEmpty()) continue;

            ResourceLocation neighborFluidId = BuiltInRegistries.FLUID.getKey(neighborFluidState.getType());
            if (neighborFluidId == null) continue;

            String neighborFluidStr = neighborFluidId.toString();
            boolean neighborIsSource = neighborFluidState.isSource();

            for (FluidDefinition.InteractionConfig rule : definition.interactions) {
                if (rule.fluid == null || rule.fluid.isBlank()) continue;

                if (!matchesFluid(neighborFluidStr, rule.fluid)) continue;

                if ("source".equalsIgnoreCase(rule.this_state) && !thisIsSource) continue;
                if ("flowing".equalsIgnoreCase(rule.this_state) && thisIsSource) continue;

                if ("source".equalsIgnoreCase(rule.target_state) && !neighborIsSource) continue;
                if ("flowing".equalsIgnoreCase(rule.target_state) && neighborIsSource) continue;

                // Match found! Determine target position to place the result block
                BlockPos replacePos = pos; // Default to replacing this fluid block
                if ("target".equalsIgnoreCase(rule.replace) || "neighbor".equalsIgnoreCase(rule.replace)) {
                    replacePos = neighborPos;
                }

                BlockState resultState = getResultBlockState(rule.result);
                level.setBlock(replacePos, resultState, 3);
                level.levelEvent(1501, replacePos, 0); // Sound & fizz particles
                return true;
            }
        }
        return false;
    }

    private int getRulePriority(FluidDefinition.InteractionConfig rule) {
        int score = 0;
        if (rule.this_state != null && !"any".equalsIgnoreCase(rule.this_state)) score += 2;
        if (rule.target_state != null && !"any".equalsIgnoreCase(rule.target_state)) score += 2;
        if (rule.when != null && !"any".equalsIgnoreCase(rule.when)) score += 1;
        return score;
    }

    private boolean matchesFluid(String actualFluidId, String ruleFluidId) {
        if (actualFluidId.equalsIgnoreCase(ruleFluidId)) return true;
        String normActual = actualFluidId.replace(":flowing_", ":");
        String normRule = ruleFluidId.replace(":flowing_", ":");
        return normActual.equalsIgnoreCase(normRule);
    }

    private BlockState getResultBlockState(String resultId) {
        if (resultId == null || resultId.isBlank() || "minecraft:air".equalsIgnoreCase(resultId) || "air".equalsIgnoreCase(resultId)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        try {
            ResourceLocation loc = ResourceLocation.parse(resultId);
            net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(loc);
            if (block != null) {
                return block.defaultBlockState();
            }
        } catch (Exception ignored) {}
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide) return;

        // 1. Fire / Ignite Behavior
        if (definition.behaviors.catch_fire) {
            int fireSecs = definition.behaviors.fire_seconds > 0 ? definition.behaviors.fire_seconds : 5;
            entity.igniteForSeconds(fireSecs);
            if (definition.behaviors.fire_damage > 0) {
                entity.hurt(level.damageSources().inFire(), definition.behaviors.fire_damage);
            }
        }

        // 2. Status / Potion Effects with Linger Duration
        if (definition.behaviors.status_effects != null && entity instanceof LivingEntity living) {
            for (FluidDefinition.StatusEffectConfig cfg : definition.behaviors.status_effects) {
                if (cfg.effect != null && !cfg.effect.isBlank()) {
                    try {
                        ResourceLocation effLoc = ResourceLocation.parse(cfg.effect);
                        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(effLoc).orElse(null);
                        if (holder != null) {
                            int totalTicks = (int) ((cfg.duration_seconds + cfg.linger_seconds) * 20);
                            living.addEffect(new MobEffectInstance(holder, totalTicks, cfg.amplifier, false, true, true));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
