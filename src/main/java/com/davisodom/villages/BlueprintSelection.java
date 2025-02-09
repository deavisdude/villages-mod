package com.davisodom.villages;

import net.minecraft.core.BlockPos;

public class BlueprintSelection {
    private final BlockPos corner1;
    private final BlockPos corner2;

    public BlueprintSelection(BlockPos corner1, BlockPos corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public BlockPos getCorner1() {
        return corner1;
    }

    public BlockPos getCorner2() {
        return corner2;
    }
}
