package com.example.examplemod.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DynamicBucketItem extends BucketItem {
    private final FlowingFluid fluid;
    private final net.minecraft.world.level.block.LiquidBlock liquidBlock;

    public DynamicBucketItem(FlowingFluid fluid, net.minecraft.world.level.block.LiquidBlock liquidBlock, Properties properties) {
        super(fluid, properties);
        this.fluid = fluid;
        this.liquidBlock = liquidBlock;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        }

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos clickedPos = hit.getBlockPos();
            Direction direction = hit.getDirection();
            BlockState clickedState = level.getBlockState(clickedPos);

            // Determine target placement position
            BlockPos targetPos;
            if (clickedState.canBeReplaced(this.fluid)) {
                targetPos = clickedPos;
            } else if (clickedState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(player, level, clickedPos, clickedState, this.fluid)) {
                targetPos = clickedPos;
            } else {
                targetPos = clickedPos.relative(direction);
            }

            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir() || targetState.canBeReplaced(this.fluid) || (targetState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(player, level, targetPos, targetState, this.fluid))) {
                if (!level.isClientSide) {
                    if (targetState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(player, level, targetPos, targetState, this.fluid)) {
                        container.placeLiquid(level, targetPos, targetState, this.fluid.getSource(false));
                    } else {
                        if (targetState.canBeReplaced(this.fluid) && !targetState.liquid()) {
                            level.destroyBlock(targetPos, true);
                        }
                        BlockState fluidBlockState = this.liquidBlock.defaultBlockState();
                        level.setBlock(targetPos, fluidBlockState, 11);
                    }
                }

                level.playSound(player, targetPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    return InteractionResultHolder.sidedSuccess(new ItemStack(Items.BUCKET), level.isClientSide());
                }
                return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
            }
        }

        return InteractionResultHolder.pass(itemstack);
    }
}
