package com.davisodom.villages.network;

import com.davisodom.villages.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;

public class FindVillagesRequestPacket {
    public FindVillagesRequestPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static FindVillagesRequestPacket decode(FriendlyByteBuf buf) {
        return new FindVillagesRequestPacket();
    }

    public void handle(ServerPlayer player) {
        if (player != null) {
            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();
            
            // Find the nearest village
            BlockPos nearestVillage = level.findNearestMapStructure(
                StructureTags.VILLAGE,
                playerPos,
                Config.villageSearchRadius,
                false
            );
            
            // Send the result back to the client
            NetworkHandler.sendToPlayer(new VillageResponsePacket(nearestVillage), player);
        }
    }
}