package com.davisodom.villages.network;

import com.davisodom.villages.util.DirectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.ArrayList;

public class VillageResponsePacket {
    private final List<BlockPos> villagePositions;
    private final boolean isNewWorld;  // Flag to indicate if world isn't generated yet

    public VillageResponsePacket(List<BlockPos> villagePositions, boolean isNewWorld) {
        this.villagePositions = villagePositions;
        this.isNewWorld = isNewWorld;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isNewWorld);
        
        if (villagePositions != null) {
            buf.writeBoolean(true);
            buf.writeInt(villagePositions.size());
            for (BlockPos pos : villagePositions) {
                buf.writeBlockPos(pos);
            }
        } else {
            buf.writeBoolean(false);
        }
    }

    public static VillageResponsePacket decode(FriendlyByteBuf buf) {
        boolean isNewWorld = buf.readBoolean();
        
        if (buf.readBoolean()) {
            int count = buf.readInt();
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
            }
            return new VillageResponsePacket(positions, isNewWorld);
        }
        return new VillageResponsePacket(null, isNewWorld);
    }

    public void handle() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) {
                return;
            }
            
            if (isNewWorld) {
                // For new worlds without much generation
                player.displayClientMessage(
                    Component.literal("No villages found yet. The world is still generating.").withStyle(ChatFormatting.YELLOW),
                    false
                );
                return;
            }
            
            if (villagePositions != null && !villagePositions.isEmpty()) {
                player.displayClientMessage(
                    Component.literal("=== Nearby Villages ===").withStyle(ChatFormatting.GREEN), 
                    false
                );

                // Sort villages by distance, nearest first for faster reading
                List<BlockPos> sortedVillages = new ArrayList<>(villagePositions);
                sortedVillages.sort((a, b) -> {
                    double distA = DirectionHelper.getDistance(player, a);
                    double distB = DirectionHelper.getDistance(player, b);
                    return Double.compare(distA, distB);
                });

                for (BlockPos pos : sortedVillages) {
                    player.displayClientMessage(
                        DirectionHelper.formatVillageInfo(player, pos),
                        false
                    );
                }
            } else {
                player.displayClientMessage(
                    Component.literal("No villages found within search radius.").withStyle(ChatFormatting.GOLD),
                    false
                );
            }
        });
    }
}