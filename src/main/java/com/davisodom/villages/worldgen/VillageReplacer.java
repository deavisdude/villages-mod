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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        
        if (processedCount > 0) {
            LOGGER.debug("Processed {} chunks for village detection", processedCount);
        }
    }
    
    /**
     * Process pending village replacements
     */
    private static void processReplacementQueue() {
        int replacedCount = 0;
        
        if (!pendingReplacements.isEmpty()) {
            LOGGER.info("Processing {} pending village replacements", pendingReplacements.size());
        }
        
        for (int i = 0; i < MAX_REPLACEMENTS_PER_TICK && !pendingReplacements.isEmpty(); i++) {
            VillageReplacement replacement = pendingReplacements.poll();
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
                        LOGGER.info("REPLACING VILLAGE: Starting replacement of village at {} with blueprint '{}'", 
                            replacement.center, replacement.blueprint.getName());
                        replaceVillage(replacement.level, replacement.center, replacement.blueprint);
                        levelReplaced.put(key, Boolean.TRUE);
                        replacedCount++;
                        LOGGER.info("VILLAGE REPLACED: Successfully replaced village at {} with blueprint '{}'", 
                            replacement.center, replacement.blueprint.getName());
                    } else {
                        LOGGER.debug("Skipping already replaced village at {}", replacement.center);
                    }
                } catch (Exception e) {
                    LOGGER.error("ERROR REPLACING VILLAGE: Failed at {}: {}", replacement.center, e.getMessage());
                    // Print full stack trace for debugging
                    e.printStackTrace();
                }
            }
        }
        
        if (replacedCount > 0) {
            LOGGER.info("REPLACEMENT SUMMARY: Replaced {} villages with blueprints this tick", replacedCount);
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
     * Specifically targets village-specific blocks like bells, beds, crops, and workstations.
     */
    private static void clearVillageArea(ServerLevel level, BlockPos center, int width, int height, int length) {
        LOGGER.info("AREA CLEARING: Beginning clearance of {}x{}x{} area at {}", width, height, length, center);
        
        // Use a more efficient approach by storing a list of positions to clear
        List<BlockPos> toClear = new ArrayList<>();
        
        // First scan the area to collect all village-specific blocks to clear
        int totalScanned = 0;
        int totalToRemove = 0;
        
        // Define village-specific blocks to target - using Set to check quickly
        // Create a set manually for compatibility with older Java versions
        Set<Block> villageSpecificBlocks = new HashSet<>();
        // Add village-specific blocks
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BELL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHITE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ORANGE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MAGENTA_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.YELLOW_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIME_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PINK_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRAY_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CYAN_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PURPLE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLUE_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BROWN_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GREEN_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.RED_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLACK_BED);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHEAT);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CARROTS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.POTATOES);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BEETROOTS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COMPOSTER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BARREL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMITHING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FLETCHING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LOOM);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRINDSTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONECUTTER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLAST_FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOKER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CAMPFIRE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LANTERN);
        // Additional village blocks - structural elements
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_PRESSURE_PLATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_PRESSURE_PLATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_PRESSURE_PLATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BIRCH_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_PRESSURE_PLATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.TORCH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WALL_TORCH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LADDER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CHEST);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GLASS_PANE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GLASS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BOOKSHELF);
        
        // Define wider radius for village-specific blocks - let's make this 64 blocks to catch all
        // village elements that might be outside our normal clearing area
        int villageRadius = 64;
        
        // STEP 1: Extended scan for village-specific blocks only
        LOGGER.info("AREA CLEARING: Performing extended scan for village-specific blocks in a {} block radius", villageRadius);
        int extendedScanned = 0;
        int extendedToRemove = 0;
        
        for (int x = -villageRadius; x < villageRadius; x++) {
            for (int z = -villageRadius; z < villageRadius; z++) {
                // For extended scan, only check a few blocks above and below ground level
                int lowestY = Math.max(center.getY() - 5, 1);
                int highestY = Math.min(center.getY() + 15, 255);
                
                for (int y = lowestY; y < highestY; y++) {
                    extendedScanned++;
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    Block block = level.getBlockState(pos).getBlock();
                    
                    // Only target village-specific blocks in extended area
                    if (villageSpecificBlocks.contains(block)) {
                        toClear.add(pos);
                        extendedToRemove++;
                    }
                }
            }
        }
        
        LOGGER.info("EXTENDED SCAN: Found {} village-specific blocks in extended {} block radius", 
                  extendedToRemove, villageRadius);
        
        // STEP 2: Regular scan for all blocks in the main village area
        for (int x = -width / 2; x < width / 2; x++) {
            for (int z = -length / 2; z < length / 2; z++) {
                // Clear only above the ground level to preserve terrain
                // But remove 1-2 layers below ground level to ensure proper foundation
                int lowestY = Math.max(center.getY() - 2, 1);
                
                // Clear from below ground level up to the height limit
                for (int y = lowestY; y < center.getY() + height; y++) {
                    totalScanned++;
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    
                    // Only clear non-air blocks to improve performance
                    // Also preserve bedrock and any blocks we want to keep
                    if (!level.getBlockState(pos).isAir() && 
                        !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                        // Avoid adding duplicates from extended scan
                        if (!toClear.contains(pos)) {
                            toClear.add(pos);
                            totalToRemove++;
                        }
                    }
                }
                
                // Log progress for each "chunk" of blocks scanned
                if (totalScanned % 10000 == 0) {
                    LOGGER.debug("AREA CLEARING: Scanning progress - checked {} blocks, found {} to remove", 
                              totalScanned, totalToRemove);
                }
            }
        }
        
        LOGGER.info("AREA CLEARING: Scan complete - found {} blocks in main area + {} village-specific blocks in extended area", 
                  totalToRemove, extendedToRemove);
        
        // Now remove the blocks in batches
        int removed = 0;
        int batchSize = 1000; // Process in smaller batches for better performance
        
        for (int i = 0; i < toClear.size(); i += batchSize) {
            int endIdx = Math.min(i + batchSize, toClear.size());
            List<BlockPos> batch = toClear.subList(i, endIdx);
            
            for (BlockPos pos : batch) {
                level.removeBlock(pos, false);
                removed++;
            }
            
            // Log progress at batch intervals
            LOGGER.debug("AREA CLEARING: Progress - removed {}/{} blocks ({}%)", 
                       removed, toClear.size(), (removed * 100 / toClear.size()));
        }
        
        LOGGER.info("AREA CLEARING: Completed - cleared {} blocks for village replacement", removed);
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
            BlockPos belowPos = checkPos.below();
            
            // Check if this block is air and the one below is solid
            boolean isAir = level.getBlockState(checkPos).isAir();
            boolean belowIsSolid = !level.getBlockState(belowPos).isAir() && 
                                  level.getBlockState(belowPos).isSolid();
            
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
}