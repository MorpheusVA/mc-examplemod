package com.example.examplemod.content;

import java.util.List;

import com.example.examplemod.content.data.ItemDefinition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

public class DynamicItem extends Item {
    private final ItemDefinition definition;

    public DynamicItem(ItemDefinition definition) {
        super(createProperties(definition));
        this.definition = definition;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    private static Item.Properties createProperties(ItemDefinition def) {
        Item.Properties props = new Item.Properties();
        if (def.max_stack_size > 0) {
            props.stacksTo(def.max_stack_size);
        }

        if (def.rarity != null) {
            props.rarity(parseRarity(def.rarity));
        }

        if ("food".equalsIgnoreCase(def.type) && def.food != null) {
            FoodProperties.Builder foodBuilder = new FoodProperties.Builder()
                    .nutrition(def.food.nutrition)
                    .saturationModifier(def.food.saturation);
            if (def.food.always_edible) {
                foodBuilder.alwaysEdible();
            }
            if (def.food.fast_food) {
                foodBuilder.fast();
            }
            props.food(foodBuilder.build());
        }

        return props;
    }

    private static Rarity parseRarity(String rarity) {
        if (rarity == null) return Rarity.COMMON;
        return switch (rarity.toLowerCase()) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (definition.description != null && !definition.description.isBlank()) {
            tooltipComponents.add(Component.translatable("jei." + com.example.examplemod.ExampleMod.MODID + "." + definition.id + ".description").withStyle(ChatFormatting.GRAY));
        }

        if (definition.extra_tooltips != null) {
            for (String extra : definition.extra_tooltips) {
                tooltipComponents.add(Component.literal(extra).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
