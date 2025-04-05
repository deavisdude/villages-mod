package com.davisodom.villages.network;

import com.davisodom.villages.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FindVillagesRequestPacket {
    // Cache for village positions (server-side)
    private static final Map<ServerLevel, Map<ChunkPos, List<BlockPos>>> villageCache = new HashMap<>();
    private static final Map<ServerLevel, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_DURATION = 60000; // Cache valid for 1 minute
    
    // Represents a chunk position
    private static class ChunkPos {
        final int x;
        final int z;
        
        ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChunkPos other) {
                return this.x == other.x && this.z == other.z;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }

    public FindVillagesRequestPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static FindVillagesRequestPacket decode(FriendlyByteBuf buf) {
        return new FindVillagesRequestPacket();
    }

    public void handle(ServerPlayer player) {
        if (player != null) {
            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();
            
            // Quick check for generated chunks
            boolean hasGeneratedChunks = isWorldGenerated(level, playerPos);
            
            if (!hasGeneratedChunks) {
                // World isn't generated yet, send immediate response
                NetworkHandler.sendToPlayer(new VillageResponsePacket(new ArrayList<>(), true), player);
                return;
            }
            
            // Process in the background to avoid server lag
            CompletableFuture.runAsync(() -> {
                List<BlockPos> villages = findNearbyVillages(level, playerPos);
                
                // Schedule response to be sent on the main thread
                level.getServer().execute(() -> {
                    NetworkHandler.sendToPlayer(new VillageResponsePacket(villages, false), player);
                });
            });
        }
    }
    
    // Check if the world has generated enough to search for villages
    private boolean isWorldGenerated(ServerLevel level, BlockPos playerPos) {
        // Check if chunks around player are generated
        int playerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
        int playerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        
        // Just check a few chunks to see if world generation has started
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private List<BlockPos> findNearbyVillages(ServerLevel level, BlockPos playerPos) {
        List<BlockPos> villages = new ArrayList<>();
        
        // Check if we have a valid cache for this level
        boolean cacheValid = false;
        
        Long lastUpdate = cacheTimestamps.get(level);
        if (lastUpdate != null && (System.currentTimeMillis() - lastUpdate) < CACHE_DURATION) {
            cacheValid = true;
        }
        
        // Create cache entry if needed
        if (!villageCache.containsKey(level)) {
            villageCache.put(level, new HashMap<>());
        }
        
        // Convert search radius to chunks, but limit to a smaller radius for speed
        int chunkRadius = Math.min((Config.villageSearchRadius >> 4) + 1, 8);
        int playerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
        int playerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        
        // Prioritize nearest chunks first - start with smaller radius
        for (int radius = 1; radius <= chunkRadius; radius++) {
            List<BlockPos> foundInRadius = searchChunkRadius(level, playerPos, playerChunkX, playerChunkZ, radius, cacheValid);
            villages.addAll(foundInRadius);
            
            // If we've found at least a few villages, we can return early for better responsiveness
            if (villages.size() >= 3) {
                break;
            }
        }
        
        // Update cache timestamp
        cacheTimestamps.put(level, System.currentTimeMillis());
        
        return villages;
    }
    
    private List<BlockPos> searchChunkRadius(ServerLevel level, BlockPos playerPos, int playerChunkX, int playerChunkZ, 
                                            int radius, boolean cacheValid) {
        List<BlockPos> villages = new ArrayList<>();
        
        // Only search the perimeter at this radius (not the inner area which we've already searched)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Skip chunks that aren't on the perimeter
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                    continue;
                }
                
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                
                List<BlockPos> cachedVillages = null;
                
                // Check if we have a valid cache for this chunk
                if (cacheValid) {
                    cachedVillages = villageCache.get(level).get(chunkPos);
                }
                
                if (cachedVillages != null) {
                    // Use cached data
                    for (BlockPos pos : cachedVillages) {
                        double distance = Math.sqrt(pos.distSqr(playerPos));
                        if (distance <= Config.villageSearchRadius && !villages.contains(pos)) {
                            villages.add(pos);
                        }
                    }
                } else {
                    // Need to query this chunk
                    BlockPos reference = new BlockPos(chunkX << 4, playerPos.getY(), chunkZ << 4);
                    BlockPos nearest = level.findNearestMapStructure(StructureTags.VILLAGE, reference, 16, false);
                    
                    if (nearest != null) {
                        double distance = Math.sqrt(nearest.distSqr(playerPos));
                        if (distance <= Config.villageSearchRadius && !villages.contains(nearest)) {
                            villages.add(nearest);
                            
                            // Store in cache
                            List<BlockPos> chunkVillages = new ArrayList<>();
                            chunkVillages.add(nearest);
                            villageCache.get(level).put(chunkPos, chunkVillages);
                        }
                    } else {
                        // Cache negative result too
                        villageCache.get(level).put(chunkPos, new ArrayList<>());
                    }
                }
            }
        }
        
        return villages;
    }
}