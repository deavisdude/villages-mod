package com.davisodom.villages;

import com.davisodom.villages.command.BlueprintSaveCommand;
import com.davisodom.villages.network.NetworkHandler;
import com.davisodom.villages.worldgen.VillageReplacer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Villages.MODID)
public class Villages {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "villages";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "villages" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "villages" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "villages" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new food item with the id "villages:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("example_item"))
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build()
            )
        )
    );

    public Villages(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register our mod's ForgeConfigSpec first, before any other registrations
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        BlueprintSaveCommand.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        // Register network handler
        NetworkHandler.register();

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        MinecraftForge.EVENT_BUS.register(new BlueprintSelectionHandler());
        MinecraftForge.EVENT_BUS.register(new KeyBindHandler());

        Config.items = new HashSet<>(); // Ensure this is not null
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        // Load blueprints
        List<Blueprint> blueprints = loadBlueprints();
        
        // Register blueprints with the village replacer
        VillageReplacer.registerBlueprints(blueprints);
    }

    private List<Blueprint> loadBlueprints() {
        List<Blueprint> blueprints = new ArrayList<>();
        // List of blueprint filenames (could be extended to a config-driven list)
        String[] blueprintFiles = { "example_blueprint.json" };
        for (String blueprintFile : blueprintFiles) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("blueprints/" + blueprintFile)) {
                if (is != null) {
                    blueprints.add(Blueprint.loadFromJson(new InputStreamReader(is)));
                    LOGGER.info("Successfully loaded blueprint: {}", blueprintFile);
                } else {
                    LOGGER.error("Blueprint resource not found: {}", blueprintFile);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load blueprint from resource: {}", blueprintFile, e);
            }
        }
        return blueprints;
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            // event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
