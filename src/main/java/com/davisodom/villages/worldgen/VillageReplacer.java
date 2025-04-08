package com.davisodom.villages.worldgen;

import com.davisodom.villages.Blueprint;
import com.davisodom.villages.Villages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

/**
 * Handles the replacement of vanilla villages with our custom blueprint designs.
 * Uses a queued approach to avoid blocking world generation.
 */
@Mod.EventBusSubscriber(modid = Villages.MODID)
public class VillageReplacer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Keep reference to our blueprints
    private static List<Blueprint> activeBlueprints = new ArrayList<>();
    
    // Queue of chunks to check for villages
    private static final Map<ServerLevel, Queue<ChunkPos>> pendingChunks = new ConcurrentHashMap<>();
    
    // Cache of processed chunks to avoid repeated processing
    private static final Map<ServerLevel, Map<ChunkPos, Boolean>> processedChunks = new ConcurrentHashMap<>();
    
    // Queue of village replacements to perform
    private static final Queue<VillageReplacement> pendingReplacements = new ConcurrentLinkedQueue<>();
    
    // Dedicated thread pool for processing village clearing operations
    private static final Executor VILLAGE_CLEARING_EXECUTOR = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Village-Clearer-" + threadNumber.getAndIncrement());
                thread.setDaemon(true); // Mark as daemon so it doesn't prevent app shutdown
                return thread;
            }
        }
    );
    
    // Track ongoing clearing operations
    private static final Map<BlockPos, CompletableFuture<List<BlockPos>>> ongoingClearingOperations = new ConcurrentHashMap<>();
    
    // Limit how many chunks we process per tick to avoid lag
    private static final int MAX_CHUNKS_PER_TICK = 5;
    
    // Limit how many replacements we do per tick to avoid lag
    private static final int MAX_REPLACEMENTS_PER_TICK = 2;
    
    // Track if initialization is complete to avoid premature processing
    private static boolean worldInitialized = false;
    
    // Track villages we've already processed to avoid duplicate replacements
    private static final Map<ServerLevel, Map<BlockPos, Boolean>> replacedVillages = new ConcurrentHashMap<>();
    
    /**
     * Represents a village that needs to be replaced with a blueprint
     */
    private static class VillageReplacement {
        private final ServerLevel level;
        private final BlockPos center;
        private final Blueprint blueprint;
        private boolean clearingStarted = false;
        private boolean clearingComplete = false;
        
        public VillageReplacement(ServerLevel level, BlockPos center, Blueprint blueprint) {
            this.level = level;
            this.center = center;
            this.blueprint = blueprint;
        }
    }
    
    /**
     * Register the blueprints that will be used to replace villages
     */
    public static void registerBlueprints(List<Blueprint> blueprints) {
        if (blueprints != null && !blueprints.isEmpty()) {
            activeBlueprints = new ArrayList<>(blueprints);
            LOGGER.info("Registered {} blueprints for village replacement", activeBlueprints.size());
        } else {
            LOGGER.warn("No blueprints registered for village replacement");
        }
    }
    
    /**
     * Handles server tick events to process our queues without blocking world generation
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Mark world as initialized after 100 ticks (5 seconds) to allow for startup
        if (!worldInitialized && event.getServer().getTickCount() > 100) {
            worldInitialized = true;
            LOGGER.info("World initialization complete, village replacement active");
        }
        
        // Don't process anything until world is considered initialized
        if (!worldInitialized) {
            return;
        }
        
        // Process pending chunks
        processChunkQueue();
        
        // Process village replacements
        processReplacementQueue();
    }
    
    /**
     * Called when a chunk is loaded, queue it for village detection
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!worldInitialized) {
            return; // Skip during world initialization
        }
        
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Skip if we don't have any blueprints
        if (activeBlueprints.isEmpty()) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();
        
        // Skip if already processed
        Map<ChunkPos, Boolean> levelProcessed = processedChunks.computeIfAbsent(serverLevel, k -> new ConcurrentHashMap<>());
        if (levelProcessed.containsKey(chunkPos)) {
            return;
        }
        
        // Add to pending queue
        Queue<ChunkPos> levelQueue = pendingChunks.computeIfAbsent(serverLevel, k -> new ConcurrentLinkedQueue<>());
        levelQueue.add(chunkPos);
    }
    
    /**
     * Process pending chunks in the queue
     */
    private static void processChunkQueue() {
        int processedCount = 0;
        
        for (Map.Entry<ServerLevel, Queue<ChunkPos>> entry : pendingChunks.entrySet()) {
            ServerLevel level = entry.getKey();
            Queue<ChunkPos> queue = entry.getValue();
            
            for (int i = 0; i < MAX_CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
                ChunkPos pos = queue.poll();
                if (pos != null) {
                    try {
                        checkChunkForVillage(level, pos);
                        processedCount++;
                    } catch (Exception e) {
                        LOGGER.error("Error processing chunk at {}, {}: {}", pos.x, pos.z, e.getMessage());
                    }
                }
            }
        }
        
        // if (processedCount > 0) {
        //     LOGGER.debug("Processed {} chunks for village detection", processedCount);
        // }
    }
    
    /**
     * Process pending village replacements
     */
    private static void processReplacementQueue() {
        // if (!pendingReplacements.isEmpty()) {
        //     LOGGER.debug("Processing {} pending village replacements", pendingReplacements.size());
        // }
        
        // Process ongoing clearing operations first
        processOngoingClearingOperations();
        
        // Only start new replacements if we're under the limit
        int activeReplacements = ongoingClearingOperations.size();
        int availableSlots = MAX_REPLACEMENTS_PER_TICK - activeReplacements;
        
        // If we have any available slots and enough villages to process, sort them by player proximity
        if (availableSlots > 0 && pendingReplacements.size() > 1) {
            sortVillageReplacementsByPlayerProximity();
        }
        
        // Process new replacements if we have available slots
        for (int i = 0; i < availableSlots && !pendingReplacements.isEmpty(); i++) {
            VillageReplacement replacement = pendingReplacements.peek(); // Just peek, don't remove yet
            if (replacement != null) {
                try {
                    // Skip if already replaced
                    Map<BlockPos, Boolean> levelReplaced = replacedVillages.computeIfAbsent(replacement.level, k -> new ConcurrentHashMap<>());
                    BlockPos key = new BlockPos(
                        replacement.center.getX() / 8 * 8, // Normalize to chunk grid to avoid multiple detections
                        0,
                        replacement.center.getZ() / 8 * 8
                    );
                    
                    if (!levelReplaced.getOrDefault(key, Boolean.FALSE)) {
                        if (!replacement.clearingStarted) {
                            // Start asynchronous clearing process
                            startAsyncVillageClearingAndReplacement(replacement);
                            replacement.clearingStarted = true;
                            // Keep in queue until clearing is complete
                        } else if (replacement.clearingComplete) {
                            // Clearing is done, finalize replacement
                            pendingReplacements.poll(); // Now remove from queue
                            levelReplaced.put(key, Boolean.TRUE);
                        }
                    } else {
                        // Already replaced, remove from queue
                        pendingReplacements.poll();
                        LOGGER.debug("Skipping already replaced village at {}", replacement.center);
                    }
                } catch (Exception e) {
                    // Remove from queue on error
                    pendingReplacements.poll();
                    LOGGER.error("ERROR REPLACING VILLAGE: Failed at {}: {}", replacement.center, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Process ongoing clearing operations and apply the results when done
     */
    private static void processOngoingClearingOperations() {
        if (ongoingClearingOperations.isEmpty()) {
            return;
        }
        
        // Check for completed futures
        Iterator<Map.Entry<BlockPos, CompletableFuture<List<BlockPos>>>> iterator = 
            ongoingClearingOperations.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, CompletableFuture<List<BlockPos>>> entry = iterator.next();
            BlockPos villageCenter = entry.getKey();
            CompletableFuture<List<BlockPos>> future = entry.getValue();
            
            if (future.isDone()) {
                try {
                    // Get the blocks to clear
                    List<BlockPos> blocksToRemove = future.get();
                    
                    // Find the corresponding replacement in the queue
                    for (VillageReplacement replacement : pendingReplacements) {
                        if (replacement.center.equals(villageCenter)) {
                            // Apply the clearing on the main thread using small batches
                            int batchSize = 100; // Reduce batch size since we're on main thread
                            int removed = 0;
                            boolean allRemoved = false;
                            
                            // Create a thread-safe list of positions to process
                            List<BlockPos> remainingBlocks = new ArrayList<>(blocksToRemove);
                            
                            // Process a small batch for this tick
                            int endIdx = Math.min(batchSize, remainingBlocks.size());
                            if (endIdx > 0) {
                                List<BlockPos> batch = remainingBlocks.subList(0, endIdx);
                                
                                // Remove blocks in this batch
                                for (BlockPos pos : batch) {
                                    replacement.level.removeBlock(pos, false);
                                    removed++;
                                }
                                
                                // Update the list for next tick
                                remainingBlocks.subList(0, endIdx).clear();
                            }
                            
                            if (remainingBlocks.isEmpty()) {
                                // All done, mark as complete
                                LOGGER.info("ASYNC CLEARING: Complete - Removed all {} blocks for village at {}", 
                                    blocksToRemove.size(), villageCenter);
                                
                                // Now place the blueprint
                                placeBlueprint(replacement);
                                
                                replacement.clearingComplete = true;
                                allRemoved = true;
                            } else {
                                // Still have more blocks to remove, update the future with remaining blocks
                                LOGGER.debug("ASYNC CLEARING: Progress - Removed {}/{} blocks for village at {} ({}%)",
                                    removed, blocksToRemove.size(),
                                    villageCenter, (removed * 100 / blocksToRemove.size()));
                                
                                // Create a new future for the remaining blocks
                                CompletableFuture<List<BlockPos>> newFuture = CompletableFuture.completedFuture(remainingBlocks);
                                ongoingClearingOperations.put(villageCenter, newFuture);
                            }
                            
                            if (allRemoved) {
                                // Remove the entry from the map
                                iterator.remove();
                            }
                            
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing async clearing for village at {}: {}", villageCenter, e.getMessage());
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Start async village clearing and replacement process
     */
    private static void startAsyncVillageClearingAndReplacement(VillageReplacement replacement) {
        LOGGER.info("ASYNC CLEARING: Starting async clearing for village at {}", replacement.center);
        
        // Kill all villagers in this area first
        killVillagersInArea(replacement.level, replacement.center, 128);
        
        // Use sea level instead of detected ground level to handle varied terrain better
        int seaLevel = 63; // Standard Minecraft sea level
        BlockPos seaLevelCenter = new BlockPos(replacement.center.getX(), seaLevel, replacement.center.getZ());
        
        LOGGER.info("ASYNC CLEARING: Using sea level (Y={}) as reference for village at {}", seaLevel, replacement.center);
        
        // Define clearing area dimensions with increased radius (128 blocks) as requested
        int padding = 25;
        int width = replacement.blueprint.getWidth() + padding * 2;
        int height = 50;
        int length = replacement.blueprint.getLength() + padding * 2;
        
        // Start the async scanning process
        CompletableFuture<List<BlockPos>> clearingFuture = CompletableFuture.supplyAsync(() -> {
            return scanVillageAreaAsync(replacement.level, seaLevelCenter, width, height, length);
        }, VILLAGE_CLEARING_EXECUTOR);
        
        // Store the future for later processing
        ongoingClearingOperations.put(replacement.center, clearingFuture);
    }
    
    /**
     * Kill all villagers in the given area
     * This ensures no villagers remain after village replacement
     */
    private static void killVillagersInArea(ServerLevel level, BlockPos center, int radius) {
        LOGGER.info("VILLAGER REMOVAL: Removing all villagers in {} block radius around {}", radius, center);
        
        try {
            // Use AABB (axis-aligned bounding box) to select all entities in the area
            net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
                center.getX() - radius, center.getY() - 30, center.getZ() - radius,
                center.getX() + radius, center.getY() + 30, center.getZ() + radius
            );
            
            // Get all villager entities in the area
            List<net.minecraft.world.entity.npc.Villager> villagers = level.getEntitiesOfClass(
                net.minecraft.world.entity.npc.Villager.class, searchArea
            );
            
            if (!villagers.isEmpty()) {
                LOGGER.info("VILLAGER REMOVAL: Found {} villagers to remove", villagers.size());
                
                // Remove all villagers
                for (net.minecraft.world.entity.npc.Villager villager : villagers) {
                    villager.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
                
                LOGGER.info("VILLAGER REMOVAL: Successfully removed {} villagers", villagers.size());
            } else {
                LOGGER.info("VILLAGER REMOVAL: No villagers found in the area");
            }
            
            // Remove Iron Golems in the area
            List<net.minecraft.world.entity.animal.IronGolem> ironGolems = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.IronGolem.class, searchArea
            );
            
            if (!ironGolems.isEmpty()) {
                LOGGER.info("VILLAGER REMOVAL: Found {} iron golems to remove", ironGolems.size());
                
                // Remove all iron golems
                for (net.minecraft.world.entity.animal.IronGolem golem : ironGolems) {
                    golem.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
                
                LOGGER.info("VILLAGER REMOVAL: Successfully removed {} iron golems", ironGolems.size());
            } else {
                LOGGER.info("VILLAGER REMOVAL: No iron golems found in the area");
            }
            
            // Also remove wandering traders in the area
            List<net.minecraft.world.entity.npc.WanderingTrader> traders = level.getEntitiesOfClass(
                net.minecraft.world.entity.npc.WanderingTrader.class, searchArea
            );
            
            if (!traders.isEmpty()) {
                LOGGER.info("VILLAGER REMOVAL: Found {} wandering traders to remove", traders.size());
                
                // Remove all traders
                for (net.minecraft.world.entity.npc.WanderingTrader trader : traders) {
                    trader.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        } catch (Exception e) {
            LOGGER.error("VILLAGER REMOVAL: Error removing villagers: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Asynchronously scan the village area to identify blocks to clear
     * This runs on a background thread and doesn't modify the world directly
     */
    private static List<BlockPos> scanVillageAreaAsync(ServerLevel level, BlockPos center, int width, int height, int length) {
        List<BlockPos> toClear = new ArrayList<>();
        
        // This operation must be thread-safe and not directly modify the world
        LOGGER.info("ASYNC SCAN: Starting scan of {}x{}x{} area at {}", width, height, length, center);        // Define village-specific blocks to target - using Set to check quickly
        Set<Block> villageSpecificBlocks = BlockSets.getVillageBlocks();
        
        // Define NATURAL blocks that should be preserved - never remove these
        Set<Block> naturalBlocks = BlockSets.getNaturalBlocks();
        
        // Define extended radius for village-specific blocks - reduced to 64 as requested
        int villageRadius = 64;
        // STEP 1: Extended scan for village-specific blocks only
        int extendedToRemove = 0;
        
        try {
            LOGGER.info("ASYNC SCAN: Beginning extended scan with radius {} blocks from center {}", villageRadius, center);
            
            // Use Minecraft world height limits - scan all the way to the top of the world
            int seaLevel = 63; // Standard Minecraft sea level
            int lowestY = Math.max(seaLevel - 15, 1); // Extend lower to catch basements/foundations
            int highestY = 320; // Maximum world height in modern Minecraft (will automatically be capped to actual world height)
            
            LOGGER.info("ASYNC SCAN: Using extended vertical range from Y={} to Y={}", lowestY, highestY);
            
            for (int x = -villageRadius; x < villageRadius; x++) {
                for (int z = -villageRadius; z < villageRadius; z++) {
                    // Scan from well below sea level to max world height to catch all structures                    
                    for (int y = lowestY; y < highestY; y++) {
                        BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                        
                        // Thread-safe check: only add blocks that are village-specific
                        try {
                            Block block = level.getBlockState(pos).getBlock();
                            boolean isVillageBlock = villageSpecificBlocks.contains(block);
                            boolean isNaturalBlock = naturalBlocks.contains(block);
                            boolean isWaterBlock = block == net.minecraft.world.level.block.Blocks.WATER;
                            
                            // Remove village blocks and artificial water (handled by improved detection)
                            if (isVillageBlock || (isWaterBlock && BlockSets.isArtificialWater(level, pos))) {
                                // Don't remove if it's a natural block we want to preserve
                                if (!isNaturalBlock || (isWaterBlock && BlockSets.isArtificialWater(level, pos))) {
                                    toClear.add(pos);
                                    extendedToRemove++;
                                    
                                    // Log every 1000 village-specific blocks found
                                    if (extendedToRemove % 1000 == 0) {
                                        LOGGER.info("ASYNC SCAN: Found {} village-specific blocks so far in extended scan", 
                                            extendedToRemove);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip positions that cause errors (might be unloaded chunks)
                        }
                    }
                }
            }
            
            LOGGER.info("ASYNC SCAN: Extended scan complete - found {} village-specific blocks in {} block radius", 
                extendedToRemove, villageRadius);
        } catch (Exception e) {
            LOGGER.error("ASYNC SCAN: Error during extended scan: {}", e.getMessage());
            e.printStackTrace();
        }
        
        // STEP 2: Regular scan for man-made blocks in the main village area
        int totalToRemove = 0;
        
        try {
            LOGGER.info("ASYNC SCAN: Beginning main area scan with dimensions {}x{}x{}", width, length, height);
            
            for (int x = -width / 2; x < width / 2; x++) {
                for (int z = -length / 2; z < length / 2; z++) {
                    // Use sea level as reference but scan all the way to world height
                    int seaLevel = 63;
                    int lowestY = Math.max(seaLevel - 15, 1);
                    int highestY = 320; // Max world height to catch all structures
                    
                    // Clear from below sea level up to world height
                    for (int y = lowestY; y < highestY; y++) {
                        BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                        
                        try {
                            Block block = level.getBlockState(pos).getBlock();
                            boolean isAirBlock = level.getBlockState(pos).isAir();
                            boolean isBedrockBlock = level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK);
                            boolean isNaturalBlock = naturalBlocks.contains(block);
                            boolean isVillageBlock = villageSpecificBlocks.contains(block);
                            boolean isWaterBlock = block == net.minecraft.world.level.block.Blocks.WATER;
                            
                            // Only remove blocks that are either:
                            // 1. Village-specific blocks
                            // 2. Artificial water (using improved detection)
                            // And make sure we're NOT removing:
                            // 1. Air blocks
                            // 2. Bedrock
                            // 3. Natural terrain blocks
                            // 4. Natural water sources
                            if (!isAirBlock && !isBedrockBlock && 
                                (!isNaturalBlock || (isWaterBlock && BlockSets.isArtificialWater(level, pos))) && 
                                (isVillageBlock || (isWaterBlock && BlockSets.isArtificialWater(level, pos)))) {
                                
                                // Avoid adding duplicates
                                if (!toClear.contains(pos)) {
                                    toClear.add(pos);
                                    totalToRemove++;
                                    
                                    // Log progress at intervals
                                    if (totalToRemove % 5000 == 0) {
                                        LOGGER.info("ASYNC SCAN: Found {} blocks in main area scan so far", 
                                            totalToRemove);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip positions that cause errors
                        }
                    }
                }
            }
            
            LOGGER.info("ASYNC SCAN: Main area scan complete - found {} blocks to remove", totalToRemove);
        } catch (Exception e) {
            LOGGER.error("ASYNC SCAN: Error during main area scan: {}", e.getMessage());
            e.printStackTrace();
        }
        
        // Final summary log
        LOGGER.info("ASYNC SCAN: Complete scan summary - found {} blocks in main area + {} village-specific blocks in extended area ({} total)", 
            totalToRemove, extendedToRemove, toClear.size());
        
        return toClear;
    }
    
    /**
     * Place blueprint blocks after the area has been cleared
     */
    private static void placeBlueprint(VillageReplacement replacement) {
        LOGGER.info("BLUEPRINT PLACEMENT: Starting placement for village at {}", replacement.center);
        
        try {
            // Find ground level for proper placement
            int groundLevel = findGroundLevelForPlacement(replacement.level, replacement.center);
            
            // Create a centered position at ground level for replacement
            BlockPos groundCenter = new BlockPos(replacement.center.getX(), groundLevel, replacement.center.getZ());
            
            // Calculate offsets to center the blueprint on the village center
            int xOffset = groundCenter.getX() - replacement.blueprint.getWidth() / 2;
            int zOffset = groundCenter.getZ() - replacement.blueprint.getLength() / 2;
            
            LOGGER.info("BLUEPRINT PLACEMENT: Using offset X={}, Z={} at ground level Y={}", 
                xOffset, zOffset, groundLevel);
            
            // Place the blueprint blocks
            int blockCount = 0;
            int airBlockCount = 0;
            
            // Log blueprint data for debugging
            LOGGER.info("BLUEPRINT PLACEMENT: Blueprint '{}' contains {} blocks to place", 
                replacement.blueprint.getName(), replacement.blueprint.getBlockData().size());
            
            // Place all blocks from the blueprint
            for (var info : replacement.blueprint.getBlockData()) {
                BlockPos target = new BlockPos(
                    xOffset + info.pos().getX(),
                    groundLevel + info.pos().getY(), // Use ground level for Y placement
                    zOffset + info.pos().getZ()
                );
                
                // Skip air blocks for performance unless they're replacing something
                if (info.state().isAir() && replacement.level.getBlockState(target).isAir()) {
                    airBlockCount++;
                    continue;
                }
                
                replacement.level.setBlock(target, info.state(), 3);
                blockCount++;
                
                // Log progress at intervals
                if (blockCount % 1000 == 0) {
                    LOGGER.info("BLUEPRINT PLACEMENT: Progress - placed {} of {} blocks", 
                        blockCount, replacement.blueprint.getBlockData().size() - airBlockCount);
                }
            }
            
            LOGGER.info("BLUEPRINT PLACEMENT: Complete - placed {} blocks (skipped {} air blocks)", 
                blockCount, airBlockCount);
            
        } catch (Exception e) {
            LOGGER.error("BLUEPRINT PLACEMENT FAILED: Could not place blueprint at {}: {}", 
                replacement.center, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the chunk contains a village structure
     */
    private static void checkChunkForVillage(ServerLevel level, ChunkPos pos) {
        // Mark as processed to avoid rechecking
        processedChunks.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(pos, Boolean.TRUE);
        
        try {
            // Check if the chunk is actually loaded
            if (!level.hasChunk(pos.x, pos.z)) {
                return;
            }
            
            // Get the structure references in this chunk
            Map<Structure, StructureStart> structureStartMap = level.getChunk(pos.x, pos.z).getAllStarts();
            if (structureStartMap == null || structureStartMap.isEmpty()) {
                return;
            }
            
            LOGGER.debug("Checking chunk {},{} for villages - found {} structures", pos.x, pos.z, structureStartMap.size());
            
            // Look for village structures
            for (Map.Entry<Structure, StructureStart> entry : structureStartMap.entrySet()) {
                Structure structure = entry.getKey();
                StructureStart start = entry.getValue();
                
                // Detailed logging for each structure
                LOGGER.debug("Structure in chunk {},{}: {}, valid: {}", 
                    pos.x, pos.z, structure.toString(), 
                    (start != null ? start.isValid() : "null start"));
                
                // Check if this is a village structure 
                if (isVillageStructure(structure)) {
                    if (start != null && start.isValid()) {
                        // Queue the village for replacement
                        BlockPos center = start.getBoundingBox().getCenter();
                        
                        // Skip if already in a replaced area
                        Map<BlockPos, Boolean> levelReplaced = replacedVillages.computeIfAbsent(level, k -> new ConcurrentHashMap<>());
                        BlockPos key = new BlockPos(
                            center.getX() / 8 * 8, // Normalize to chunk grid to avoid multiple detections
                            0,
                            center.getZ() / 8 * 8
                        );
                        
                        if (!levelReplaced.getOrDefault(key, Boolean.FALSE)) {
                            LOGGER.info("VILLAGE FOUND: Detected village at {} in chunk {},{}", center, pos.x, pos.z);
                            
                            // Choose a blueprint for this village
                            if (!activeBlueprints.isEmpty()) {
                                // For now just use the first one. Could be randomized later
                                Blueprint blueprint = activeBlueprints.get(0);
                                LOGGER.info("QUEUEING VILLAGE: Adding village at {} for replacement with blueprint '{}'", 
                                    center, blueprint.getName());
                                pendingReplacements.add(new VillageReplacement(level, center, blueprint));
                            } else {
                                LOGGER.error("VILLAGE FOUND BUT NO BLUEPRINTS AVAILABLE: Cannot replace village at {}", center);
                            }
                        } else {
                            LOGGER.debug("Skipping already processed village at {}", center);
                        }
                    } else {
                        LOGGER.debug("Found invalid village structure at chunk {},{}", pos.x, pos.z);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking for village in chunk {}, {}: {}", pos.x, pos.z, e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a structure is a village
     */
    private static boolean isVillageStructure(Structure structure) {
        if (structure == null) {
            return false;
        }
        
        // Get the string representation which usually contains the registry name
        String structureString = structure.toString();
        // Get the class name for additional identification
        String className = structure.getClass().getSimpleName();
        
        // Debug log the structure details to help diagnose village detection issues
        LOGGER.debug("Structure details - class: {}, toString: {}", className, structureString);
        
        // Enhanced detection logic for Minecraft's village structures
        // Check different possible village identifiers since registration methods vary by MC version
        boolean isVillage = false;
        
        // Check for "village" in the structure name
        if (structureString.toLowerCase().contains("village")) {
            isVillage = true;
        }
        // Modern villages are often implemented as Jigsaw structures
        else if (className.equals("JigsawStructure") && 
                (structureString.toLowerCase().contains("jigsaw") || 
                 structureString.toLowerCase().contains("plains") ||
                 structureString.toLowerCase().contains("desert") ||
                 structureString.toLowerCase().contains("taiga") ||
                 structureString.toLowerCase().contains("savanna") ||
                 structureString.toLowerCase().contains("snowy"))) {
            isVillage = true;
        }
        
        if (isVillage) {
            LOGGER.info("VILLAGE STRUCTURE FOUND: {} (class: {})", structureString, className);
        } else {
            LOGGER.debug("Structure not a village: {} (class: {})", structureString, className);
        }
        
        return isVillage;
    }
    
    /**
     * Replace a village with our blueprint
     */
    private static void replaceVillage(ServerLevel level, BlockPos center, Blueprint blueprint) {
        LOGGER.info("REPLACEMENT DETAIL: Starting village replacement at {} with blueprint '{}'", center, blueprint.getName());
        
        try {
            // Get the ground level at the village center for proper Y alignment
            int groundY = findGroundLevel(level, center);
            LOGGER.info("REPLACEMENT DETAIL: Found ground level at Y={} for village at {}", groundY, center);
            
            // Create a centered position at ground level for replacement
            BlockPos groundCenter = new BlockPos(center.getX(), groundY, center.getZ());
            
            // Calculate offsets to center the blueprint on the village center
            int xOffset = groundCenter.getX() - blueprint.getWidth() / 2;
            int zOffset = groundCenter.getZ() - blueprint.getLength() / 2;
            LOGGER.info("REPLACEMENT DETAIL: Blueprint placement offset: X={}, Z={}, dimensions={}x{}", 
                xOffset, zOffset, blueprint.getWidth(), blueprint.getLength());
            
            // Define the village area to clear based on blueprint dimensions
            // Add padding to ensure we clear the entire village
            int padding = 25; // Increased padding to ensure complete village removal
            int width = blueprint.getWidth() + padding * 2;
            int height = 50; // Clear more blocks up to catch tall structures
            int length = blueprint.getLength() + padding * 2;
            
            // Clear the entire village area first (including buildings and decorations)
            LOGGER.info("REPLACEMENT DETAIL: Clearing village area at {} with dimensions {}x{}x{}", 
                groundCenter, width, height, length);
            clearVillageArea(level, groundCenter, width, height, length);
            
            // Now place the blueprint, using the ground level as the base
            int blockCount = 0;
            int airBlockCount = 0; // Count air blocks for debugging
            
            // Log blueprint data for debugging
            LOGGER.info("REPLACEMENT DETAIL: Blueprint '{}' contains {} blocks to place", 
                blueprint.getName(), blueprint.getBlockData().size());
            
            // Place all blocks from the blueprint
            for (var info : blueprint.getBlockData()) {
                BlockPos target = new BlockPos(
                    xOffset + info.pos().getX(),
                    groundY + info.pos().getY(), // Use the detected ground Y
                    zOffset + info.pos().getZ()
                );
                
                // Skip air blocks for performance unless they're replacing something
                if (info.state().isAir() && level.getBlockState(target).isAir()) {
                    airBlockCount++;
                    continue;
                }
                
                level.setBlock(target, info.state(), 3);
                blockCount++;
                
                // Log progress at intervals to avoid excessive logging
                if (blockCount % 1000 == 0) {
                    LOGGER.info("REPLACEMENT PROGRESS: Placed {} of {} blocks for village at {}", 
                        blockCount, blueprint.getBlockData().size() - airBlockCount, center);
                }
            }
            
            LOGGER.info("REPLACEMENT COMPLETE: Successfully replaced village at {} with blueprint '{}', placed {} blocks (skipped {} air blocks)", 
                groundCenter, blueprint.getName(), blockCount, airBlockCount);
        } catch (Exception e) {
            LOGGER.error("REPLACEMENT FAILED: Could not replace village at {}: {}", center, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clears the area around a village center to prepare for replacement
     * Specifically targets village-specific blocks and ensures complete removal of all structures.
     */
    private static void clearVillageArea(ServerLevel level, BlockPos center, int width, int height, int length) {
        LOGGER.info("AREA CLEARING: Starting enhanced clearance of {}x{}x{} area at {}", width, height, length, center);

        // Use a list to store positions to clear
        List<BlockPos> toClear = new ArrayList<>();

        // Define village-specific blocks to target
        Set<Block> villageSpecificBlocks = new HashSet<>();
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BELL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHITE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.DIRT_PATH); // Replacing GRASS_PATH
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE); // Replacing FENCE
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE); // Replacing FENCE_GATE
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.TORCH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LANTERN);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CHEST);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BARREL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOKER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLAST_FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COMPOSTER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHEAT);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CARROTS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.POTATOES);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BEETROOTS);

        // Scan the area to collect all blocks to clear
        for (int x = -width / 2; x <= width / 2; x++) {
            for (int z = -length / 2; z <= length / 2; z++) {
                for (int y = center.getY() - 10; y <= center.getY() + height; y++) {
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    Block block = level.getBlockState(pos).getBlock();

                    // Add village-specific blocks or any non-air blocks to the list
                    if (villageSpecificBlocks.contains(block) || !level.getBlockState(pos).isAir()) {
                        toClear.add(pos);
                    }
                }
            }
        }

        LOGGER.info("AREA CLEARING: Found {} blocks to remove in the enhanced scan", toClear.size());

        // Remove the blocks in batches
        int removed = 0;
        int batchSize = 500;
        for (int i = 0; i < toClear.size(); i += batchSize) {
            int endIdx = Math.min(i + batchSize, toClear.size());
            List<BlockPos> batch = toClear.subList(i, endIdx);

            for (BlockPos pos : batch) {
                level.removeBlock(pos, false);
                removed++;
            }

            LOGGER.debug("AREA CLEARING: Progress - removed {}/{} blocks", removed, toClear.size());
        }

        LOGGER.info("AREA CLEARING: Completed - removed {} blocks for village replacement", removed);
    }
    
    /**
     * Find the ground level (first solid block from top to bottom) at a given position
     */
    private static int findGroundLevel(ServerLevel level, BlockPos center) {
        LOGGER.debug("GROUND DETECTION: Finding ground level at {}", center);
        
        // Start from 10 blocks above the center and scan down to find the ground
        int startY = center.getY() + 10;
        int endY = center.getY() - 10; // Don't go too far below in case of caves
        
        // Limit Y ranges to world bounds using compatible constants
        startY = Math.min(startY, 255); // Standard Minecraft height limit
        endY = Math.max(endY, 1);       // Just above bedrock
        
        LOGGER.debug("GROUND DETECTION: Searching from Y={} to Y={} (world height limits: 0 to 255)", 
                   startY, endY);
        
        // Scan downward to find the ground
        for (int y = startY; y >= endY; y--) {
            BlockPos checkPos = new BlockPos(center.getX(), y, center.getZ());
            BlockPos belowPos = checkPos.below();            // Check if this block is air and the one below is solid
            boolean isAir = level.getBlockState(checkPos).isAir();
            boolean belowIsSolid = !level.getBlockState(belowPos).isAir() && 
                                  level.getBlockState(belowPos).canOcclude();
            
            if (y % 5 == 0 || (isAir && belowIsSolid)) { // Log only every 5 blocks or when we find ground
                LOGGER.debug("GROUND DETECTION: At Y={}: isAir={}, belowIsSolid={}, block={}", 
                           y, isAir, belowIsSolid, 
                           level.getBlockState(belowPos).getBlock().getDescriptionId());
            }
            
            if (isAir && belowIsSolid) {
                LOGGER.info("GROUND DETECTION: Found ground level at Y={} for village at {}, block type: {}", 
                          y, center, level.getBlockState(belowPos).getBlock().getDescriptionId());
                return y;
            }
        }
        
        // Fallback to the center Y if we couldn't find a good ground level
        LOGGER.warn("GROUND DETECTION FAILED: Couldn't find ground level for village at {}, using center Y={}", center, center.getY());
        return center.getY();
    }
    
    /**
     * Find the most suitable ground level for blueprint placement
     * Ensures blueprints sit atop naturally occurring grass blocks
     */
    private static int findGroundLevelForPlacement(ServerLevel level, BlockPos center) {
        LOGGER.info("GROUND PLACEMENT: Finding optimal ground level for blueprint at {}", center);
        
        // Sample ground levels at multiple points to find the best placement height
        // with preference for grass blocks
        int sampleRadius = 15; // Sample in a 30x30 area
        int sampleCount = 0;
        int totalHeight = 0;
        int minHeight = 255;
        int maxHeight = 0;
        
        // Track grass block heights specifically
        int grassBlockCount = 0;
        int grassBlockTotalHeight = 0;
        
        // Collect ground level samples across the blueprint placement area
        for (int x = -sampleRadius; x <= sampleRadius; x += 3) { // More samples than before
            for (int z = -sampleRadius; z <= sampleRadius; z += 3) {
                BlockPos samplePos = new BlockPos(center.getX() + x, center.getY(), center.getZ() + z);
                int groundY = findLocalGroundLevel(level, samplePos);
                
                if (groundY > 0) {
                    // Add to general samples
                    totalHeight += groundY;
                    sampleCount++;
                    minHeight = Math.min(minHeight, groundY);
                    maxHeight = Math.max(maxHeight, groundY);
                    
                    // Check if this position has a grass block directly below it
                    BlockPos belowPos = new BlockPos(samplePos.getX(), groundY - 1, samplePos.getZ());
                    if (level.getBlockState(belowPos).getBlock() == net.minecraft.world.level.block.Blocks.GRASS_BLOCK) {
                        grassBlockCount++;
                        grassBlockTotalHeight += groundY;
                        LOGGER.debug("GROUND PLACEMENT: Found grass block at Y={}", groundY);
                    }
                }
            }
        }
        
        // If we found some grass blocks, prioritize them for placement height
        if (grassBlockCount > 0) {
            int grassAvgHeight = grassBlockTotalHeight / grassBlockCount;
            LOGGER.info("GROUND PLACEMENT: Found {} grass blocks with average height Y={}", 
                grassBlockCount, grassAvgHeight);
            
            // Use grass blocks as the reference ground level
            return grassAvgHeight;
        }
        
        // If no grass blocks found, use regular ground level logic
        if (sampleCount > 0) {
            int averageHeight = totalHeight / sampleCount;
            int heightVariation = maxHeight - minHeight;
            
            LOGGER.info("GROUND PLACEMENT: Found average ground level Y={} (min={}, max={}, variation={})", 
                averageHeight, minHeight, maxHeight, heightVariation);
            
            // If terrain is very uneven (large variation), prefer higher ground
            // to avoid burying parts of the blueprint
            if (heightVariation > 4) {
                // Use a height closer to the maximum to avoid burying the structure
                int adjustedHeight = maxHeight - 2;
                LOGGER.info("GROUND PLACEMENT: Terrain is uneven, adjusted placement height to Y={}", adjustedHeight);
                return adjustedHeight;
            }
            
            return averageHeight;
        }
        
        // If we failed to find a good average, try a direct ground level check
        int directGroundLevel = findGroundLevel(level, center);
        LOGGER.info("GROUND PLACEMENT: Using direct ground level Y={} as fallback", directGroundLevel);
        return directGroundLevel;
    }
    
    /**
     * Find ground level at a specific position
     * This is a helper for the multi-sample ground level detection
     */
    private static int findLocalGroundLevel(ServerLevel level, BlockPos pos) {
        // Start from well above the position
        int startY = 120; // Start higher to handle mountains
        
        // Scan downward to find ground
        for (int y = startY; y > 40; y--) { // Don't go below Y=40 to avoid caves
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockPos belowPos = checkPos.below();
            
            // Find air block with solid block below (ground surface)
            boolean isAir = level.getBlockState(checkPos).isAir();
            boolean belowIsSolid = !level.getBlockState(belowPos).isAir() && 
                                   level.getBlockState(belowPos).canOcclude() &&
                                   level.getBlockState(belowPos).getFluidState().isEmpty();
            
            if (isAir && belowIsSolid) {
                return y; // Found ground level
            }
        }
        
        // If we couldn't find proper ground, use sea level as fallback
        return 63;
    }
    
    /**
     * Called when a level is unloaded, cleans up our cache
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            pendingChunks.remove(serverLevel);
            processedChunks.remove(serverLevel);
            replacedVillages.remove(serverLevel);            
            LOGGER.debug("Cleared caches for unloaded level: {}", serverLevel.dimension().location());
        }
    }
    
    /**
     * Sorts the pending village replacements based on proximity to players
     * This ensures villages closest to players are processed first
     */
    private static void sortVillageReplacementsByPlayerProximity() {
        if (pendingReplacements.size() <= 1) {
            return; // Nothing to sort
        }
        
        List<VillageReplacement> villages = new ArrayList<>(pendingReplacements);
        Map<VillageReplacement, Double> distanceMap = new HashMap<>();
        
        // Find all online players for this level
        for (VillageReplacement village : villages) {
            ServerLevel level = village.level;
            
            // Find minimum distance to any player in this level
            double minDistance = Double.MAX_VALUE;
            List<net.minecraft.server.level.ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
            
            for (net.minecraft.server.level.ServerPlayer player : players) {
                // Only consider players in the same dimension/level
                if (player.level() == level) {
                    double distSq = player.blockPosition().distSqr(village.center);
                    minDistance = Math.min(minDistance, distSq);
                }
            }
            
            // Store the minimum distance to any player
            distanceMap.put(village, minDistance);
        }
        
        // Sort villages by distance to nearest player (closest first)
        villages.sort(Comparator.comparingDouble(distanceMap::get));
        
        // Clear the queue and add back in sorted order
        pendingReplacements.clear();
        pendingReplacements.addAll(villages);
        
        // Log that we've sorted villages by player proximity
        if (!villages.isEmpty()) {
            LOGGER.info("VILLAGE PRIORITY: Sorted {} villages by player proximity", villages.size());
        }
    }
}