package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

/**
 * IC2 rubber wood state machine (1.12.2):
 *  - plain_{axis}: normal log blocks (piston movable)
 *  - dry_{side}: harvested resin hole, can regenerate to wet
 *  - wet_{side}: resin available
 */
public enum RubberWoodState implements StringIdentifiable {
    plain_y(Direction.Axis.Y, null, false),
    plain_x(Direction.Axis.X, null, false),
    plain_z(Direction.Axis.Z, null, false),

    dry_north(Direction.Axis.Y, Direction.NORTH, false),
    dry_south(Direction.Axis.Y, Direction.SOUTH, false),
    dry_west(Direction.Axis.Y, Direction.WEST, false),
    dry_east(Direction.Axis.Y, Direction.EAST, false),

    wet_north(Direction.Axis.Y, Direction.NORTH, true),
    wet_south(Direction.Axis.Y, Direction.SOUTH, true),
    wet_west(Direction.Axis.Y, Direction.WEST, true),
    wet_east(Direction.Axis.Y, Direction.EAST, true);

    public final Direction.Axis axis;
    public final Direction facing; // null for plain_*
    public final boolean wet;

    RubberWoodState(Direction.Axis axis, Direction facing, boolean wet) {
        this.axis = axis;
        this.facing = facing;
        this.wet = wet;
    }

    @Override
    public String asString() {
        return name();
    }

    public boolean isPlain() {
        return facing == null;
    }

    /** True for dry_* states only. */
    public boolean canRegenerate() {
        return !isPlain() && !wet;
    }

    public RubberWoodState getWet() {
        if (isPlain()) return null;
        if (wet) return this;
        // dry_* ordinals are +4 from plain_* group, wet_* are +4 from dry_*
        return values()[ordinal() + 4];
    }

    public RubberWoodState getDry() {
        if (isPlain() || !wet) return this;
        return values()[ordinal() - 4];
    }

    public static RubberWoodState getWet(Direction facing) {
        return switch (facing) {
            case NORTH -> wet_north;
            case SOUTH -> wet_south;
            case WEST -> wet_west;
            case EAST -> wet_east;
            default -> throw new IllegalArgumentException("incompatible facing: " + facing);
        };
    }

    public static RubberWoodState plainForAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> plain_x;
            case Z -> plain_z;
            default -> plain_y;
        };
    }

    public static RubberWoodState withFacing(boolean wet, Direction facing) {
        RubberWoodState w = getWet(facing);
        return wet ? w : w.getDry();
    }
}
