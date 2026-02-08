package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.LvTransformerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.LvTransformerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * LV Transformer (НН): LV <-> MV.
 *
 * DOT side = MV (tier2=128).
 * Other sides = LV (tier1=32).
 *
 * Energy is conserved:
 * - Step-up: accumulate 128 from LV inputs, emit 128 on DOT.
 * - Step-down: accept 128 on DOT, emit up to 4x32 on other sides.
 */
public class LvTransformerBlockEntity extends BlockEntity implements IEuEnergyStorage, ExtendedScreenHandlerFactory {

    // Tier mapping for this transformer
    private static final int LV = 1; // 32
    private static final int MV = 2; // 128
    private static final long LV_PACKET = EuUtil.powerFromTier(LV); // 32
    private static final long MV_PACKET = EuUtil.powerFromTier(MV); // 128

    // Buffers:
    // lowBuffer collects LV energy to form MV packets (step-up output).
    // highBuffer collects MV energy to be split into LV packets (step-down output).
    private long lowBuffer = 0;   // 0..128
    private long highBuffer = 0;  // 0..128

    // GUI props: lowBuffer, highBuffer, dotDirOrdinal
    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override public int size() { return LvTransformerScreenHandler.PROP_COUNT; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, lowBuffer);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, highBuffer);
                case 2 -> getDot().getId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lowBuffer = clamp(value, 0, (int) MV_PACKET);
                case 1 -> highBuffer = clamp(value, 0, (int) MV_PACKET);
                default -> {}
            }
        }

        private long clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
    };

    public LvTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LV_TRANSFORMER, pos, state);
    }

    private Direction getDot() {
        if (getCachedState().contains(LvTransformerBlock.DOT)) {
            return getCachedState().get(LvTransformerBlock.DOT);
        }
        return Direction.NORTH;
    }

    public static void tick(World world, BlockPos pos, BlockState state, LvTransformerBlockEntity be) {
        if (world.isClient) return;

        // Step-up emission: lowBuffer -> MV out on DOT
        if (be.lowBuffer >= MV_PACKET) {
            Direction dot = be.getDot();
            // Try route one MV packet. If it goes nowhere, we keep energy (IC2 feel).
            long spent = EuNetwork.route(world, pos, be, dot, MV_PACKET);
            // extractEu() will subtract from lowBuffer only if it actually spent.
            if (spent > 0) {
                be.markDirty();
            }
        }

        // Step-down emission: highBuffer -> LV out on non-DOT sides (up to 4 packets per 128)
        if (be.highBuffer >= LV_PACKET) {
            Direction dot = be.getDot();

            int maxPackets = (int) Math.min(4, be.highBuffer / LV_PACKET);
            int sent = 0;

            // Simple round-robin-ish over sides (skip DOT)
            for (Direction d : Direction.values()) {
                if (d == dot) continue;
                if (sent >= maxPackets) break;

                long spent = EuNetwork.route(world, pos, be, d, LV_PACKET);
                if (spent > 0) {
                    sent++;
                    be.markDirty();
                }
            }
        }
    }

    // ---- IEuEnergyStorage ----

    @Override public long getEuStored() { return lowBuffer + highBuffer; }
    @Override public long getEuCapacity() { return 2L * MV_PACKET; } // conceptual

    @Override
    public int getSinkTier(Direction side) {
        // DOT accepts MV; other sides accept LV.
        return (side == getDot()) ? MV : LV;
    }

    @Override
    public int getSourceTier(Direction side) {
        // DOT outputs MV; other sides output LV.
        return (side == getDot()) ? MV : LV;
    }

    /**
     * Some generic helpers (and older callers) use the no-arg tier getters.
     * For a transformer, the "highest" tier it can handle is the MV tier.
     */
    @Override
    public int getSinkTier() {
        return MV;
    }

    @Override
    public int getSourceTier() {
        return MV;
    }

    @Override
    public boolean canInsert(Direction from) {
        return true;
    }

    @Override
    public boolean canExtract(Direction to) {
        Direction dot = getDot();
        if (to == dot) {
            return lowBuffer >= MV_PACKET; // step-up ready
        } else {
            return highBuffer >= LV_PACKET; // step-down ready
        }
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0) return 0;

        Direction dot = getDot();
        if (from == dot) {
            // MV input (step-down buffer)
            long max = Math.min(amount, MV_PACKET);
            long free = MV_PACKET - highBuffer;
            long acc = Math.min(max, free);
            if (!simulate && acc > 0) {
                highBuffer += acc;
                markDirty();
            }
            return acc;
        } else {
            // LV input (step-up buffer)
            long max = Math.min(amount, LV_PACKET);
            long free = MV_PACKET - lowBuffer;
            long acc = Math.min(max, free);
            if (!simulate && acc > 0) {
                lowBuffer += acc;
                markDirty();
            }
            return acc;
        }
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0) return 0;

        Direction dot = getDot();
        if (to == dot) {
            // MV output consumes lowBuffer in chunks of 128
            long can = Math.min(amount, MV_PACKET);
            if (lowBuffer < can) return 0;
            if (!simulate) {
                lowBuffer -= can;
                markDirty();
            }
            return can;
        } else {
            // LV output consumes highBuffer in chunks of 32
            long can = Math.min(amount, LV_PACKET);
            if (highBuffer < can) return 0;
            if (!simulate) {
                highBuffer -= can;
                markDirty();
            }
            return can;
        }
    }

    // GUI integration (simple)
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.industrial_legacy.lv_transformer");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, net.minecraft.entity.player.PlayerEntity player) {
        return new LvTransformerScreenHandler(syncId, inv, this, guiProps);
    }
}
