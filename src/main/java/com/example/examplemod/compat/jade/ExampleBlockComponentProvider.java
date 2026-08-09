package com.example.examplemod.compat.jade;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.DynamicBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ExampleBlockComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "dynamic_block_provider");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof DynamicBlock dynamicBlock) {
            String wailaInfo = dynamicBlock.getDefinition().waila_info;
            if (wailaInfo != null && !wailaInfo.isBlank()) {
                tooltip.add(Component.literal(wailaInfo).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
