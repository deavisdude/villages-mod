package com.davisodom.villages.network;

import com.davisodom.villages.util.DirectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.ArrayList;

public class VillageResponsePacket {
    private final List<BlockPos> villagePositions;

    public VillageResponsePacket(List<BlockPos> villagePositions) {
        this.villagePositions = villagePositions;
    }

    public void encode(FriendlyByteBuf buf) {
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
        if (buf.readBoolean()) {
            int count = buf.readInt();
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
            }
            return new VillageResponsePacket(positions);
        }
        return new VillageResponsePacket(null);
    }

    public void handle() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) {
                return;
            }
            
            if (villagePositions != null && !villagePositions.isEmpty()) {
                player.displayClientMessage(
                    Component.literal("=== Nearby Villages ==="), 
                    false
                );

                // Sort villages by distance, farthest first so nearest appears last
                List<BlockPos> sortedVillages = new ArrayList<>(villagePositions);
                sortedVillages.sort((a, b) -> {
                    double distA = DirectionHelper.getDistance(player, a);
                    double distB = DirectionHelper.getDistance(player, b);
                    return Double.compare(distB, distA);
                });

                for (BlockPos pos : sortedVillages) {
                    player.displayClientMessage(
                        DirectionHelper.formatVillageInfo(player, pos),
                        false
                    );
                }
            } else {
                player.displayClientMessage(
                    Component.literal("No villages found nearby."),
                    false
                );
            }
        });
    }
}