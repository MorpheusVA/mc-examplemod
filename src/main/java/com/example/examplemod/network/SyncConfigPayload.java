package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.ContentManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncConfigPayload(String itemsJson, String blocksJson, String fluidsJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncConfigPayload::itemsJson,
            ByteBufCodecs.STRING_UTF8, SyncConfigPayload::blocksJson,
            ByteBufCodecs.STRING_UTF8, SyncConfigPayload::fluidsJson,
            SyncConfigPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient(IPayloadContext context) {
        context.enqueueWork(() -> {
            ContentManager.getInstance().applyServerSyncedConfig(itemsJson, blocksJson, fluidsJson);
        });
    }
}
