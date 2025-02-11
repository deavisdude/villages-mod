package com.davisodom.villages.network;

import com.davisodom.villages.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.List;

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
            List<BlockPos> villages = new ArrayList<>();
            
            // Get all village structures within radius
            int chunkRadius = (Config.villageSearchRadius >> 4) + 1; // Convert blocks to chunks
            int playerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
            int playerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());

            // Search in a square area around the player
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;
                    
                    BlockPos reference = new BlockPos(chunkX << 4, playerPos.getY(), chunkZ << 4);
                    BlockPos nearest = level.findNearestMapStructure(StructureTags.VILLAGE, reference, 16, false);
                    
                    if (nearest != null) {
                        double distance = Math.sqrt(nearest.distSqr(playerPos));
                        if (distance <= Config.villageSearchRadius) {
                            if (!villages.contains(nearest)) {
                                villages.add(nearest);
                            }
                        }
                    }
                }
            }
            
            // Send the results back to the client
            NetworkHandler.sendToPlayer(new VillageResponsePacket(villages), player);
        }
    }
}