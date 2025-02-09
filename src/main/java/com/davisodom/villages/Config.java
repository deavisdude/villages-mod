package com.davisodom.villages;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Villages.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue ENABLE_BLUEPRINT_SAVING = BUILDER
            .comment("Whether to enable blueprint saving")
            .define("enableBlueprintSaving", true);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.BooleanValue LOG_BLUEPRINTS = BUILDER
            .comment("Whether to log blueprints on common setup")
            .define("logBlueprints", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static boolean logBlueprints;
    public static boolean enableBlueprintSaving;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        updateConfigValues();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        updateConfigValues();
    }

    private static void updateConfigValues() {
        LOGGER.info("Updating Villages mod configuration values...");
        
        logDirtBlock = LOG_DIRT_BLOCK.get();
        LOGGER.info("Log dirt block set to: {}", logDirtBlock);
        
        magicNumber = MAGIC_NUMBER.get();
        LOGGER.info("Magic number set to: {}", magicNumber);
        
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        LOGGER.info("Magic number introduction set to: {}", magicNumberIntroduction);
        
        logBlueprints = LOG_BLUEPRINTS.get();
        LOGGER.info("Log blueprints set to: {}", logBlueprints);
        
        enableBlueprintSaving = ENABLE_BLUEPRINT_SAVING.get();
        LOGGER.info("Blueprint saving enabled set to: {}", enableBlueprintSaving);

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                .collect(Collectors.toSet());
        LOGGER.info("Loaded {} items from config", items.size());
    }
}
