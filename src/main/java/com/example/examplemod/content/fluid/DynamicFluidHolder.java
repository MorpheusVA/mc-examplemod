package com.example.examplemod.content.fluid;

import com.example.examplemod.content.data.FluidDefinition;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class DynamicFluidHolder {
    private final FluidDefinition definition;
    private DynamicFluidType fluidType;
    private FlowingFluid sourceFluid;
    private FlowingFluid flowingFluid;
    private DynamicLiquidBlock block;
    private BucketItem bucketItem;
    private BaseFlowingFluid.Properties properties;

    public DynamicFluidHolder(FluidDefinition definition) {
        this.definition = definition;
    }

    public DynamicFluidType getOrCreateFluidType() {
        if (this.fluidType == null) {
            this.fluidType = new DynamicFluidType(definition);
        }
        return this.fluidType;
    }

    public void initFluids() {
        if (this.sourceFluid != null) return;

        getOrCreateFluidType();

        this.properties = new BaseFlowingFluid.Properties(
                () -> fluidType,
                () -> sourceFluid,
                () -> flowingFluid
        );

        // Configure flow speed (tickRate) and flow distance
        this.properties.tickRate(Math.max(1, definition.physics.flow_speed_ticks));

        int dist = Math.max(1, Math.min(8, definition.physics.flow_distance));
        int dropOff = Math.max(1, 8 / dist);
        this.properties.levelDecreasePerBlock(dropOff);
        this.properties.slopeFindDistance(Math.min(16, dist + 2));

        this.properties.block(() -> this.block);
        if (definition.bucket.has_bucket) {
            this.properties.bucket(() -> this.bucketItem);
        }

        this.sourceFluid = new BaseFlowingFluid.Source(this.properties);
        this.flowingFluid = new BaseFlowingFluid.Flowing(this.properties);
        this.block = new DynamicLiquidBlock(this.sourceFluid, this.definition);

        if (definition.bucket.has_bucket) {
            this.bucketItem = new DynamicBucketItem(this.sourceFluid, this.block, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
        }
    }

    public FluidDefinition getDefinition() {
        return definition;
    }

    public void updateDefinition(FluidDefinition newDef) {
        if (newDef == null) return;
        if (this.fluidType != null) {
            this.fluidType.updateDefinition(newDef);
        }
        if (this.block != null) {
            this.block.updateDefinition(newDef);
        }
    }

    public DynamicFluidType getFluidType() {
        return getOrCreateFluidType();
    }

    public FlowingFluid getSourceFluid() {
        initFluids();
        return sourceFluid;
    }

    public FlowingFluid getFlowingFluid() {
        initFluids();
        return flowingFluid;
    }

    public DynamicLiquidBlock getBlock() {
        initFluids();
        return block;
    }

    public BucketItem getBucketItem() {
        initFluids();
        return bucketItem;
    }

    public BaseFlowingFluid.Properties getProperties() {
        return properties;
    }
}
