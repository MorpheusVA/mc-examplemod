package com.example.examplemod.content;

import com.example.examplemod.content.data.ItemDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class DynamicCrossbowItem extends CrossbowItem implements IDynamicItem {
    private final ItemDefinition definition;

    public DynamicCrossbowItem(ItemDefinition definition) {
        super(createProperties(definition));
        this.definition = definition;
    }

    @Override
    public ItemDefinition getDefinition() {
        return definition;
    }

    private static Properties createProperties(ItemDefinition def) {
        Properties props = new Properties();
        if (def.max_stack_size > 0) {
            props.stacksTo(def.max_stack_size);
        }
        if (def.rarity != null) {
            props.rarity(DynamicItem.parseRarity(def.rarity));
        }
        return props;
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
