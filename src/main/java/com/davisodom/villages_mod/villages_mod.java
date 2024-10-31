package com.davisodom.villages_mod;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;
import net.minecraft.util.Direction;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("villages_mod")
public class villages_mod
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public villages_mod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // Register the player join event
        MinecraftForge.EVENT_BUS.register(new PlayerJoinWorldHandler());
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().options);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("villages_mod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }

    public static class PlayerJoinWorldHandler {
        @SubscribeEvent
        public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
            World world = event.getPlayer().level;
            BlockPos playerPos = event.getPlayer().blockPosition();
            Direction facing = event.getPlayer().getDirection();
            BlockPos buildingPos = playerPos.relative(facing, 5); // Adjust the offset as needed

            // Create a larger building with 2 stories, multiple rooms, lighting, and gold floors
            BlockState wallState = Blocks.STONE.defaultBlockState();
            BlockState floorState = Blocks.GOLD_BLOCK.defaultBlockState();
            BlockState lightState = Blocks.TORCH.defaultBlockState();
            BlockState doorState = Blocks.OAK_DOOR.defaultBlockState();

            int width = 10;
            int height = 6;
            int depth = 10;

            // Create the structure
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < depth; z++) {
                        BlockPos pos = buildingPos.offset(x, y, z);
                        if (y == 0 || y == height - 1 || x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
                            // Create walls, floor, and ceiling
                            world.setBlock(pos, wallState, 3);
                        } else if (y == 1 || y == height - 2) {
                            // Create floors
                            world.setBlock(pos, floorState, 3);
                        }
                    }
                }
            }

            // Add a door at the front of the building
            BlockPos doorPos = buildingPos.offset(width / 2, 1, 0); // Adjust the position as needed
            world.setBlock(doorPos, doorState, 3);
            world.setBlock(doorPos.above(), doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);

            // Add lighting
            for (int x = 1; x < width - 1; x += 3) {
                for (int z = 1; z < depth - 1; z += 3) {
                    BlockPos lightPos = buildingPos.offset(x, 2, z);
                    world.setBlock(lightPos, lightState, 3);
                    lightPos = buildingPos.offset(x, height - 3, z);
                    world.setBlock(lightPos, lightState, 3);
                }
            }

            // Create rooms by adding interior walls
            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < depth - 1; z++) {
                    if (x % 4 == 0 || z % 4 == 0) {
                        for (int y = 1; y < height - 1; y++) {
                            BlockPos pos = buildingPos.offset(x, y, z);
                            world.setBlock(pos, wallState, 3);
                        }
                    }
                }
            }
        }
    }
}
