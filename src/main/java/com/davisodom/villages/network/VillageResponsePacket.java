package com.davisodom.villages.network;

import com.davisodom.villages.util.DirectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class VillageResponsePacket {
    private final BlockPos villagePos;

    public VillageResponsePacket(BlockPos villagePos) {
        this.villagePos = villagePos;
    }

    public void encode(FriendlyByteBuf buf) {
        if (villagePos != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(villagePos);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static VillageResponsePacket decode(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return new VillageResponsePacket(buf.readBlockPos());
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
            
            if (villagePos != null) {
                player.displayClientMessage(
                    Component.literal("=== Nearby Villages ==="), 
                    false
                );
                player.displayClientMessage(
                    DirectionHelper.formatVillageInfo(player, villagePos),
                    false
                );
            } else {
                player.displayClientMessage(
                    Component.literal("No villages found nearby."),
                    false
                );
            }
        });
    }
}