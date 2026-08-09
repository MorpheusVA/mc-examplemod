package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.ContentManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                SyncConfigPayload.TYPE,
                SyncConfigPayload.STREAM_CODEC,
                SyncConfigPayload::handleClient
        );
    }
}

@EventBusSubscriber(modid = ExampleMod.MODID)
class GameNetworkEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            String itemsJson = ContentManager.getInstance().getItemDefinitionsJson();
            String blocksJson = ContentManager.getInstance().getBlockDefinitionsJson();
            String fluidsJson = ContentManager.getInstance().getFluidDefinitionsJson();

            PacketDistributor.sendToPlayer(serverPlayer, new SyncConfigPayload(itemsJson, blocksJson, fluidsJson));
        }
    }
}
