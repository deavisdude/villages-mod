package com.davisodom.villages.command;

import com.davisodom.villages.Blueprint;
import com.davisodom.villages.BlueprintSelection;
import com.davisodom.villages.BlueprintSelectionStorage;
import com.davisodom.villages.Villages;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlueprintSaveCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("saveblueprint")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                UUID playerId = source.getPlayerOrException().getUUID();
                BlueprintSelection selection = BlueprintSelectionStorage.getSelection(playerId);
                if (selection == null) {
                    source.sendFailure(Component.literal(ChatFormatting.RED + "No blueprint selection found. " +
                        "Use the wooden axe to select two corners first."));
                    return 0;
                }
                // Create a blueprint object based on the selection.
                // Here we use the differences between corners for dimensions.
                int width = Math.abs(selection.getCorner2().getX() - selection.getCorner1().getX()) + 1;
                int height = Math.abs(selection.getCorner2().getY() - selection.getCorner1().getY()) + 1;
                int length = Math.abs(selection.getCorner2().getZ() - selection.getCorner1().getZ()) + 1;
                // Assuming you have a method to extract block data from the selection
                List<StructureBlockInfo> blockData = extractBlockData(selection, source);
                Blueprint blueprint = new Blueprint("player_blueprint", width, height, length, blockData);
                // (Extend: extract block data from the world between the two corners)

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(blueprint);

                // Save file to a blueprints folder inside the screenshots directory.
                // Using the server directory as game directory reference.
                Path gameDir = source.getServer().getServerDirectory();
                Path blueprintDir = gameDir.resolve("screenshots").resolve("blueprints");
                blueprintDir.toFile().mkdirs();
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                File outputFile = blueprintDir.resolve("blueprint_" + timestamp + ".json").toFile();

                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(json);
                    source.sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Blueprint saved to: " +
                        outputFile.getAbsolutePath()), false);
                } catch (IOException e) {
                    source.sendFailure(Component.literal(ChatFormatting.RED + "Failed to save blueprint: " + e.getMessage()));
                }
                return 1;
            });
    }

    private static List<StructureBlockInfo> extractBlockData(BlueprintSelection selection, CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        BlockPos corner1 = selection.getCorner1();
        BlockPos corner2 = selection.getCorner2();
        List<StructureBlockInfo> blockData = new ArrayList<>();

        for (int x = Math.min(corner1.getX(), corner2.getX()); x <= Math.max(corner1.getX(), corner2.getX()); x++) {
            for (int y = Math.min(corner1.getY(), corner2.getY()); y <= Math.max(corner1.getY(), corner2.getY()); y++) {
                for (int z = Math.min(corner1.getZ(), corner2.getZ()); z <= Math.max(corner1.getZ(), corner2.getZ()); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    blockData.add(new StructureBlockInfo(pos, state, null));
                }
            }
        }
        return blockData;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}
