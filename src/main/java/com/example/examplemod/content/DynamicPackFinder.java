package com.example.examplemod.content;

import java.nio.file.Path;
import java.util.Optional;

import com.example.examplemod.ExampleMod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class DynamicPackFinder {
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path packPath = ContentManager.getInstance().getGeneratedPackPath();
            if (packPath != null) {
                PackLocationInfo info = new PackLocationInfo(
                        "examplemod_dynamic_resources",
                        Component.literal("ExampleMod Dynamic Resources"),
                        PackSource.BUILT_IN,
                        Optional.empty()
                );
                Pack.ResourcesSupplier supplier = new PathPackResources.PathResourcesSupplier(packPath);
                PackSelectionConfig config = new PackSelectionConfig(true, Pack.Position.TOP, false);
                Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, config);
                if (pack != null) {
                    event.addRepositorySource((consumer) -> consumer.accept(pack));
                }
            }
        }
    }
}
