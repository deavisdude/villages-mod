package com.davisodom.villages.command;

import com.davisodom.villages.Blueprint;
import com.davisodom.villages.BlueprintSelection;
import com.davisodom.villages.BlueprintSelectionHandler;
import com.davisodom.villages.BlueprintSelectionStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapterFactory;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.registries.ForgeRegistries;

public class BlueprintSaveCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintSaveCommand.class);

    private static class BlockStateAdapter extends TypeAdapter<BlockState> {
        @Override
        public void write(JsonWriter out, BlockState value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            try {
                // Write just the registry name
                String registryName = ForgeRegistries.BLOCKS.getKey(value.getBlock()).toString();
                out.value(registryName);
            } catch (Exception e) {
                LOGGER.error("Failed to serialize BlockState: {}", value.getBlock(), e);
                throw e;
            }
        }

        @Override
        public BlockState read(JsonReader in) throws IOException {
            return null;
        }
    }

    private static class StructureBlockInfoAdapter extends TypeAdapter<StructureBlockInfo> {
        private final TypeAdapter<BlockState> blockStateAdapter;

        StructureBlockInfoAdapter(Gson gson) {
            this.blockStateAdapter = new BlockStateAdapter();
        }

        @Override
        public void write(JsonWriter out, StructureBlockInfo value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            try {
                out.beginObject();
                // Write coordinates as separate x, y, z fields
                out.name("x").value(value.pos().getX());
                out.name("y").value(value.pos().getY());
                out.name("z").value(value.pos().getZ());
                
                out.name("block");
                blockStateAdapter.write(out, value.state());
                
                out.endObject();
            } catch (Exception e) {
                LOGGER.error("Failed to serialize StructureBlockInfo at {}: {}", value.pos(), e.getMessage());
                throw e;
            }
        }

        @Override
        public StructureBlockInfo read(JsonReader in) throws IOException {
            return null;
        }
    }

    private static class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {
        private final TypeAdapter<T> adapter;

        OptionalTypeAdapter(TypeAdapter<T> adapter) {
            this.adapter = adapter;
        }

        @Override
        public void write(JsonWriter out, Optional<T> value) throws IOException {
            if (value.isPresent()) {
                adapter.write(out, value.get());
            } else {
                out.nullValue();
            }
        }

        @Override
        public Optional<T> read(JsonReader in) throws IOException {
            return Optional.ofNullable(adapter.read(in));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("saveblueprint")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    try {
                        LOGGER.info("Starting blueprint save command execution");
                        UUID playerId = source.getPlayerOrException().getUUID();
                        LOGGER.debug("Processing blueprint save for player: {}", playerId);
                        
                        BlueprintSelection selection = BlueprintSelectionStorage.getSelection(playerId);
                        if (selection == null) {
                            LOGGER.warn("No blueprint selection found for player: {}", playerId);
                            source.sendFailure(Component.literal(ChatFormatting.RED + "No blueprint selection found. " +
                                "Use the wooden axe to select two corners first."));
                            return 0;
                        }

                        LOGGER.debug("Selection found - Corner1: {}, Corner2: {}", 
                            selection.getCorner1(), selection.getCorner2());

                        // Create a blueprint object based on the selection
                        int width = Math.abs(selection.getCorner2().getX() - selection.getCorner1().getX()) + 1;
                        int height = Math.abs(selection.getCorner2().getY() - selection.getCorner1().getY()) + 1;
                        int length = Math.abs(selection.getCorner2().getZ() - selection.getCorner1().getZ()) + 1;
                        
                        LOGGER.debug("Blueprint dimensions - Width: {}, Height: {}, Length: {}", width, height, length);

                        try {
                            List<StructureBlockInfo> blockData = extractBlockData(selection, source);
                            LOGGER.debug("Extracted {} blocks for blueprint", blockData.size());
                            
                            Blueprint blueprint = new Blueprint("player_blueprint", width, height, length, blockData);
                            
                            GsonBuilder gsonBuilder = new GsonBuilder()
                                .setPrettyPrinting()
                                .registerTypeAdapter(BlockState.class, new BlockStateAdapter())
                                .registerTypeAdapter(StructureBlockInfo.class, new StructureBlockInfoAdapter(new Gson()));

                            // Add the Optional type adapter factory
                            gsonBuilder.registerTypeAdapterFactory(new TypeAdapterFactory() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                                    if (!Optional.class.isAssignableFrom(type.getRawType())) {
                                        return null;
                                    }
                                    Type innerType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
                                    TypeAdapter<?> innerAdapter = gson.getAdapter(TypeToken.get(innerType));
                                    return (TypeAdapter<T>) new OptionalTypeAdapter<>(innerAdapter);
                                }
                            });

                            Gson gson = gsonBuilder.create();
                            LOGGER.debug("Starting JSON serialization");
                            String json = gson.toJson(blueprint);
                            LOGGER.debug("JSON serialization completed successfully");
                            
                            // Save file to a blueprints folder inside the screenshots directory
                            Path gameDir = source.getServer().getServerDirectory();
                            Path blueprintDir = gameDir.resolve("screenshots").resolve("blueprints");
                            LOGGER.debug("Creating blueprint directory at: {}", blueprintDir);
                            
                            if (!blueprintDir.toFile().exists() && !blueprintDir.toFile().mkdirs()) {
                                throw new IOException("Failed to create blueprints directory");
                            }

                            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                            File outputFile = blueprintDir.resolve("blueprint_" + timestamp + ".json").toFile();
                            LOGGER.debug("Saving blueprint to: {}", outputFile.getAbsolutePath());

                            try (FileWriter writer = new FileWriter(outputFile)) {
                                writer.write(json);
                                LOGGER.info("Successfully saved blueprint to: {}", outputFile.getAbsolutePath());
                                source.sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Blueprint saved to: " +
                                    outputFile.getAbsolutePath()), false);
                                BlueprintSelectionHandler.clearPlayerSelections(playerId);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed to save blueprint", e);
                            source.sendFailure(Component.literal(ChatFormatting.RED + "Failed to save blueprint: " + e.getMessage()));
                            return 0;
                        }
                        return 1;
                    } catch (Exception e) {
                        LOGGER.error("Unexpected error during blueprint save", e);
                        source.sendFailure(Component.literal(ChatFormatting.RED + "An unexpected error occurred: " + e.getMessage()));
                        return 0;
                    }
                })
        );
    }

    private static List<StructureBlockInfo> extractBlockData(BlueprintSelection selection, CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        BlockPos corner1 = selection.getCorner1();
        BlockPos corner2 = selection.getCorner2();
        List<StructureBlockInfo> blockData = new ArrayList<>();
        
        LOGGER.debug("Starting block data extraction from {} to {}", corner1, corner2);
        int blockCount = 0;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());

        try {
            for (int x = minX; x <= Math.max(corner1.getX(), corner2.getX()); x++) {
                for (int y = minY; y <= Math.max(corner1.getY(), corner2.getY()); y++) {
                    for (int z = minZ; z <= Math.max(corner1.getZ(), corner2.getZ()); z++) {
                        BlockPos worldPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(worldPos);
                        // Store positions relative to the minimum corner
                        BlockPos relativePos = new BlockPos(x - minX, y - minY, z - minZ);
                        blockData.add(new StructureBlockInfo(relativePos, state, null));
                        blockCount++;
                    }
                }
            }
            LOGGER.debug("Successfully extracted {} blocks", blockCount);
        } catch (Exception e) {
            LOGGER.error("Error during block data extraction at count {}", blockCount, e);
            throw e;
        }
        return blockData;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}
