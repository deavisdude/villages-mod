package com.davisodom.villages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Blueprint {
    private String name;
    private int width;
    private int height;
    private int length;
    private List<StructureBlockInfo> blockData;

    public Blueprint(String name, int width, int height, int length, List<StructureBlockInfo> blockData) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blockData = blockData;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public List<StructureBlockInfo> getBlockData() {
        return blockData;
    }

    public static Blueprint loadFromJson(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return loadFromJson(reader);
        }
    }

    public static Blueprint loadFromJson(Reader reader) throws IOException {
        Gson gson = new Gson();
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

        String name = jsonObject.get("name").getAsString();
        int width = jsonObject.get("width").getAsInt();
        int height = jsonObject.get("height").getAsInt();
        int length = jsonObject.get("length").getAsInt();

        List<StructureBlockInfo> blockData = new ArrayList<>();
        jsonObject.getAsJsonArray("blockData").forEach(element -> {
            JsonObject blockInfoJson = element.getAsJsonObject();
            BlockPos pos = new BlockPos(blockInfoJson.get("x").getAsInt(), 
                                        blockInfoJson.get("y").getAsInt(), 
                                        blockInfoJson.get("z").getAsInt());
            String blockName = blockInfoJson.get("block").getAsString();
            if (!blockName.contains(":")) {
                blockName = "minecraft:" + blockName;
            }
            ResourceLocation resourceLocation = ResourceLocation.tryParse(blockName);
            if (resourceLocation == null) {
                System.err.println("Invalid block identifier: " + blockName + " at " + pos);
                resourceLocation = ResourceLocation.tryParse("minecraft:air");
            }
            Block block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
            BlockState state = block.defaultBlockState();
            blockData.add(new StructureBlockInfo(pos, state, null));
        });

        return new Blueprint(name, width, height, length, blockData);
    }

    public void saveToJson(String filePath) throws IOException {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", name);
        jsonObject.addProperty("width", width);
        jsonObject.addProperty("height", height);
        jsonObject.addProperty("length", length);

        jsonObject.add("blockData", gson.toJsonTree(blockData));

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(jsonObject, writer);
        }
    }
}
