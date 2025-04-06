package com.davisodom.villages.network;

import com.davisodom.villages.Villages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.lang.reflect.Constructor;

public class NetworkHandler {
    // Helper method using reflection to access the non-public two-argument constructor.
    private static ResourceLocation createResourceLocation(String namespace, String path) {
        try {
            Constructor<ResourceLocation> constructor = ResourceLocation.class.getDeclaredConstructor(String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(namespace, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ResourceLocation", e);
        }
    }
    
    public static final SimpleChannel INSTANCE = ChannelBuilder
        .named(createResourceLocation(Villages.MODID, "main"))
        .networkProtocolVersion(1)
        .simpleChannel();

    public static void register() {
        INSTANCE.messageBuilder(FindVillagesRequestPacket.class)
            .encoder(FindVillagesRequestPacket::encode)
            .decoder(FindVillagesRequestPacket::decode)
            .consumerMainThread((packet, context) -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    packet.handle(player);
                }
            })
            .add();
        
        INSTANCE.messageBuilder(VillageResponsePacket.class)
            .encoder(VillageResponsePacket::encode)
            .decoder(VillageResponsePacket::decode)
            .consumerMainThread((packet, context) -> packet.handle())
            .add();
    }

    public static void sendToServer(Object packet) {
        INSTANCE.send(packet, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
    }
}