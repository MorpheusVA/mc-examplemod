package com.example.examplemod.compat.jei;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.ContentManager;
import com.example.examplemod.content.DynamicBlock;
import com.example.examplemod.content.DynamicItem;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class ExampleModJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Register informative descriptions in JEI for all dynamic items
        for (DynamicItem item : ContentManager.getInstance().getDynamicItems().values()) {
            if (item.getDefinition().description != null && !item.getDefinition().description.isBlank()) {
                registration.addIngredientInfo(item, Component.translatable("jei.examplemod." + item.getDefinition().id + ".description"));
            }
        }

        // Register informative descriptions in JEI for all dynamic blocks
        for (DynamicBlock block : ContentManager.getInstance().getDynamicBlocks().values()) {
            if (block.getDefinition().description != null && !block.getDefinition().description.isBlank()) {
                registration.addIngredientInfo(block, Component.translatable("jei.examplemod." + block.getDefinition().id + ".description"));
            }
        }
    }
}
