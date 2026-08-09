package com.example.examplemod.content;

import java.util.List;

import com.example.examplemod.content.data.BlockDefinition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DynamicBlock extends Block {
    private final BlockDefinition definition;

    public DynamicBlock(BlockDefinition definition) {
        super(createProperties(definition));
        this.definition = definition;
    }

    public BlockDefinition getDefinition() {
        return definition;
    }

    private static BlockBehaviour.Properties createProperties(BlockDefinition def) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .destroyTime(def.destroy_time)
                .explosionResistance(def.explosion_resistance)
                .sound(parseSoundType(def.sound_type))
                .mapColor(parseMapColor(def.map_color));

        if (def.light_emission > 0) {
            props.lightLevel(state -> def.light_emission);
        }

        if (def.requires_tool) {
            props.requiresCorrectToolForDrops();
        }

        return props;
    }

    private static SoundType parseSoundType(String sound) {
        if (sound == null) return SoundType.STONE;
        return switch (sound.toLowerCase()) {
            case "wood" -> SoundType.WOOD;
            case "metal" -> SoundType.METAL;
            case "glass" -> SoundType.GLASS;
            case "grass" -> SoundType.GRASS;
            case "gravel" -> SoundType.GRAVEL;
            case "sand" -> SoundType.SAND;
            case "wool" -> SoundType.WOOL;
            case "deepslate" -> SoundType.DEEPSLATE;
            default -> SoundType.STONE;
        };
    }

    private static MapColor parseMapColor(String color) {
        if (color == null) return MapColor.STONE;
        return switch (color.toLowerCase()) {
            case "wood" -> MapColor.WOOD;
            case "metal" -> MapColor.METAL;
            case "dirt" -> MapColor.DIRT;
            case "plant" -> MapColor.PLANT;
            case "water" -> MapColor.WATER;
            case "color_cyan" -> MapColor.COLOR_CYAN;
            default -> MapColor.STONE;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (definition.description != null && !definition.description.isBlank()) {
            tooltipComponents.add(Component.literal(definition.description).withStyle(ChatFormatting.GRAY));
        }

        if (definition.extra_tooltips != null) {
            for (String extra : definition.extra_tooltips) {
                tooltipComponents.add(Component.literal(extra).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
