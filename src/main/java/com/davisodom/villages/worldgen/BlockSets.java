package com.davisodom.villages.worldgen;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.Block;

/**
 * Utility class containing predefined sets of blocks used for village replacement.
 * This class centralizes the block categorization to make the codebase more maintainable.
 */
public class BlockSets {
    
    /**
     * Creates a set of village-specific blocks for identifying village structures.
     * @return A set of blocks commonly found in villages
     */
    public static Set<Block> getVillageBlocks() {
        Set<Block> villageSpecificBlocks = new HashSet<>();
        
        // Village-specific functional blocks
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
        
        // Workstation blocks
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMITHING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FLETCHING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BREWING_STAND);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CAULDRON);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COMPOSTER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BARREL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOKER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLAST_FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRINDSTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LECTERN);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LOOM);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONECUTTER);
        
        // Crop blocks
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHEAT);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CARROTS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.POTATOES);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BEETROOTS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MELON_STEM);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PUMPKIN_STEM);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH);
        
        // Common village structural blocks for various biomes
        // Oak village elements
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.OAK_PRESSURE_PLATE);
        
        // Spruce village elements (taiga villages)
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SPRUCE_PRESSURE_PLATE);
        
        // Acacia village elements (savanna villages)
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_PLANKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_FENCE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_FENCE_GATE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_DOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_TRAPDOOR);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ACACIA_PRESSURE_PLATE);
        
        // Sandstone village elements (desert villages)
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SANDSTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CUT_SANDSTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SANDSTONE_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SANDSTONE_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SANDSTONE_WALL);
        
        // Stone village elements
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONE_BRICKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONE_BRICK_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STONE_BRICK_WALL);
        
        // Common village infrastructure
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.DIRT_PATH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.TORCH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WALL_TORCH);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LANTERN);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CAMPFIRE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FARMLAND);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GLASS_PANE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GLASS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LADDER);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CHEST);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.FURNACE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BOOKSHELF);
        
        // Additional blocks commonly found in villages
        // Stripped logs
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_BIRCH_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_ACACIA_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_JUNGLE_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_LOG);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_BIRCH_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_ACACIA_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_JUNGLE_WOOD);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_WOOD);
        
        // Wool blocks (commonly used in village decorations)
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHITE_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ORANGE_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MAGENTA_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.YELLOW_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIME_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PINK_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRAY_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CYAN_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PURPLE_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLUE_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BROWN_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GREEN_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.RED_WOOL);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLACK_WOOL);
        
        // Stone variants used in village structures
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOOTH_STONE);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.SMOOTH_STONE_SLAB);
        
        // Brick variants
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BRICKS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BRICK_STAIRS);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BRICK_SLAB);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BRICK_WALL);
        
        // Terracotta blocks (commonly used in desert and savanna villages)
        // Regular terracotta
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.TERRACOTTA);
        
        // Colored terracotta
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHITE_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ORANGE_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MAGENTA_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.YELLOW_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIME_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PINK_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRAY_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CYAN_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PURPLE_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLUE_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BROWN_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GREEN_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.RED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLACK_TERRACOTTA);
        
        // Glazed terracotta (commonly found in desert villages)
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.WHITE_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.ORANGE_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.MAGENTA_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.YELLOW_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIME_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PINK_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GRAY_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.CYAN_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.PURPLE_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLUE_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BROWN_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.GREEN_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.RED_GLAZED_TERRACOTTA);
        villageSpecificBlocks.add(net.minecraft.world.level.block.Blocks.BLACK_GLAZED_TERRACOTTA);
        
        return villageSpecificBlocks;
    }
    
    /**
     * Creates a set of natural blocks that should be preserved during village replacement.
     * @return A set of blocks that are considered natural terrain
     */
    public static Set<Block> getNaturalBlocks() {
        Set<Block> naturalBlocks = new HashSet<>();
        
        // Natural terrain blocks
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.DIRT);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.COARSE_DIRT);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.PODZOL);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.SAND);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.RED_SAND);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.GRAVEL);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.STONE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.ANDESITE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.DIORITE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.GRANITE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.CLAY);
        
        // Water should generally be preserved except above sea level
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.WATER);
        
        // Other natural blocks
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.BEDROCK);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.LAVA);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.SNOW);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.ICE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.PACKED_ICE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.BLUE_ICE);
        
        // Underground/cave natural blocks
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.DEEPSLATE);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.TUFF);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.CALCITE);
        
        // Natural vegetation
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.TALL_GRASS);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.FERN);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.LARGE_FERN);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.DANDELION);
        naturalBlocks.add(net.minecraft.world.level.block.Blocks.POPPY);
        
        return naturalBlocks;
    }
    
    /**
     * Determines if a water block is artificial (man-made) or natural.
     * @param level The server level containing the water block
     * @param pos The position of the water block
     * @return true if the water appears to be artificial (part of village construction)
     */
    public static boolean isArtificialWater(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        // Check surrounding blocks to determine if the water is part of a natural feature
        // or if it is enclosed by man-made blocks.
        Set<Block> villageSpecificBlocks = getVillageBlocks();
        int surroundingVillageBlocks = 0;
        int surroundingNaturalBlocks = 0;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                net.minecraft.core.BlockPos neighborPos = pos.offset(dx, 0, dz);
                Block neighborBlock = level.getBlockState(neighborPos).getBlock();
                
                if (villageSpecificBlocks.contains(neighborBlock)) {
                    surroundingVillageBlocks++;
                } else {
                    surroundingNaturalBlocks++;
                }
            }
        }
        
        // If the majority of surrounding blocks are village-specific, consider the water artificial.
        return surroundingVillageBlocks > surroundingNaturalBlocks;
    }
}
