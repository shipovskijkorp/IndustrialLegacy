package com.shipovskijkorp.industriallegacy.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;

/**
 * IL Experimental style per-world wind simulation.
 *
 * <p>Source of truth: industrial_legacy.core.WindSim from IL 2.8.222-ex112.</p>
 */
public final class WindSimulation extends PersistentState {
    private static final String STATE_ID = "industrial_legacy_wind";
    private static final double MAX_WIND = 108.0D;

    private int windStrength;
    private int windDirection;
    private int windTicker;

    private WindSimulation() {
        this.windStrength = 5 + MathHelper.nextInt(net.minecraft.util.math.random.Random.create(), 0, 19);
        this.windDirection = MathHelper.nextInt(net.minecraft.util.math.random.Random.create(), 0, 359);
    }

    private WindSimulation(NbtCompound nbt) {
        this.windStrength = nbt.contains("windStrength") ? nbt.getInt("windStrength") : 10;
        this.windDirection = nbt.contains("windDirection") ? nbt.getInt("windDirection") : 0;
        this.windTicker = nbt.getInt("windTicker");
    }

    public static WindSimulation get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(WindSimulation::fromNbt, WindSimulation::new, STATE_ID);
    }

    private static WindSimulation fromNbt(NbtCompound nbt) {
        return new WindSimulation(nbt);
    }

    public void tick(ServerWorld world) {
        if (this.windTicker++ % 128 != 0) {
            return;
        }

        int upChance = 10;
        int downChance = 10;
        if (this.windStrength > 20) {
            upChance -= this.windStrength - 20;
        } else if (this.windStrength < 10) {
            downChance -= 10 - this.windStrength;
        }

        if (world.random.nextInt(100) < upChance) {
            ++this.windStrength;
            markDirty();
        } else if (world.random.nextInt(100) < downChance) {
            --this.windStrength;
            markDirty();
        }

        int turn = world.random.nextInt(3);
        if (turn == 0) {
            this.windDirection = changeDirection(-18);
            markDirty();
        } else if (turn == 2) {
            this.windDirection = changeDirection(18);
            markDirty();
        }
    }

    public double getWindAt(ServerWorld world, double y) {
        double ret = this.windStrength;
        ret *= heightMultiplier(world, y);
        if (world.isThundering()) {
            ret *= 1.5D;
        } else if (world.isRaining()) {
            ret *= 1.25D;
        }
        return Math.max(0.0D, ret * 2.4D);
    }

    public double getMaxWind() {
        return MAX_WIND;
    }

    public int getWindStrengthBase() {
        return windStrength;
    }

    public int getWindDirection() {
        return windDirection;
    }

    private int changeDirection(int amount) {
        int next = this.windDirection + amount;
        if (next < 0) {
            next += 360;
        } else if (next > 359) {
            next -= 360;
        }
        return next;
    }

    private static double heightMultiplier(ServerWorld world, double y) {
        int height = Math.max(1, world.getTopY());
        int seaLevel = Math.max(0, world.getSeaLevel());
        double baseHeight = seaLevel < height ? (double) seaLevel : (double) height * 0.5D;
        double sh = baseHeight + ((double) height - baseHeight) / 2.0D;
        double fh = (double) height * 1.125D;

        double[][] a = new double[][] {
                {sh, sh * sh, sh * sh * sh},
                {fh, fh * fh, fh * fh * fh},
                {1.0D, 2.0D * sh, 3.0D * sh * sh}
        };
        double[] b = new double[] {1.0D, 0.0D, 0.0D};
        double[] c = solve3(a, b);
        return Math.max(0.0D, y * c[0] + y * y * c[1] + y * y * y * c[2]);
    }

    private static double[] solve3(double[][] a, double[] b) {
        double[][] m = new double[3][4];
        for (int row = 0; row < 3; row++) {
            System.arraycopy(a[row], 0, m[row], 0, 3);
            m[row][3] = b[row];
        }

        for (int col = 0; col < 3; col++) {
            int pivot = col;
            for (int row = col + 1; row < 3; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;

            double div = m[col][col];
            if (Math.abs(div) < 1.0E-12D) {
                return new double[] {0.0D, 0.0D, 0.0D};
            }
            for (int k = col; k < 4; k++) {
                m[col][k] /= div;
            }
            for (int row = 0; row < 3; row++) {
                if (row == col) continue;
                double factor = m[row][col];
                for (int k = col; k < 4; k++) {
                    m[row][k] -= factor * m[col][k];
                }
            }
        }
        return new double[] {m[0][3], m[1][3], m[2][3]};
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("windStrength", windStrength);
        nbt.putInt("windDirection", windDirection);
        nbt.putInt("windTicker", windTicker);
        return nbt;
    }
}
