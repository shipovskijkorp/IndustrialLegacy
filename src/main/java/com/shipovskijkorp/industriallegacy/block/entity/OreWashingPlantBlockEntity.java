package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.OreWashingPlantBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.OreWashingRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.OreWashingPlantScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OreWashingPlantBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_0 = 1;
    public static final int SLOT_OUTPUT_1 = 2;
    public static final int SLOT_OUTPUT_2 = 3;
    public static final int SLOT_WATER = 4;
    public static final int SLOT_CELL_OUTPUT = 5;
    public static final int SLOT_DISCHARGE = 6;
    public static final int SLOT_UPGRADE_0 = 7;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = { SLOT_INPUT, SLOT_WATER };
    private static final int[] SIDE_SLOTS = { SLOT_INPUT, SLOT_WATER, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_CELL_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 8000L;
    private static final int EU_PER_TICK = 16;
    private static final int BASE_TICKS = 500;
    public static final int WATER_CAPACITY = 8000;
    public static final int WATER_CELL_AMOUNT = 1000;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;
    private int waterAmount = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> waterAmount;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> waterAmount = Math.max(0, Math.min(WATER_CAPACITY, value));
                default -> { }
            }
        }
    };

    public OreWashingPlantBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ORE_WASHING_PLANT, pos, state); }

    public static void tick(World world, BlockPos pos, BlockState state, OreWashingPlantBlockEntity be) {
        if (world.isClient) return;
        be.gainWater();
        boolean active = be.processTick(world);
        if (state.get(OreWashingPlantBlock.LIT) != active) world.setBlockState(pos, state.with(OreWashingPlantBlock.LIT, active), 3);
        if (active) be.markDirty();
    }

    private boolean processTick(World world) {
        OreWashingRecipe recipe = findRecipe(world).orElse(null);
        if (recipe == null) { progress = 0; return false; }
        if (waterAmount < recipe.getWaterAmount()) { progress = 0; return false; }
        List<ItemStack> outputs = recipe.getResults();
        if (!canOutput(outputs) || energy < EU_PER_TICK) return false;
        energy -= EU_PER_TICK;
        maxProgress = recipe.getTicks() <= 0 ? BASE_TICKS : recipe.getTicks();
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_INPUT).decrement(recipe.getInputCount());
            waterAmount = Math.max(0, waterAmount - recipe.getWaterAmount());
            insertOutputs(outputs);
            progress = 0;
        }
        return true;
    }

    private Optional<OreWashingRecipe> findRecipe(World world) {
        return MachineRecipeManager.findOreWashingRecipe(this);
    }

    private void gainWater() {
        if (waterAmount > WATER_CAPACITY - WATER_CELL_AMOUNT) return;
        ItemStack stack = items.get(SLOT_WATER);
        if (stack.isEmpty()) return;
        ItemStack empty = ItemStack.EMPTY;
        boolean valid = false;
        if (stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER) {
            empty = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
            valid = true;
        } else if (stack.isOf(Items.WATER_BUCKET)) {
            empty = new ItemStack(Items.BUCKET);
            valid = true;
        }
        if (!valid || !canCellOutput(empty)) return;
        stack.decrement(1);
        insertCellOutput(empty);
        waterAmount += WATER_CELL_AMOUNT;
        markDirty();
    }

    private boolean canCellOutput(ItemStack stack) {
        ItemStack cur = items.get(SLOT_CELL_OUTPUT);
        return cur.isEmpty() || (ItemStack.canCombine(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxCount());
    }
    private void insertCellOutput(ItemStack stack) {
        ItemStack cur = items.get(SLOT_CELL_OUTPUT);
        if (cur.isEmpty()) items.set(SLOT_CELL_OUTPUT, stack); else cur.increment(stack.getCount());
    }

    private boolean canOutput(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy()); temp.add(items.get(SLOT_OUTPUT_1).copy()); temp.add(items.get(SLOT_OUTPUT_2).copy());
        for (ItemStack out : outputs) {
            int rem = out.getCount();
            for (ItemStack slot : temp) if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) { int add = Math.min(rem, slot.getMaxCount()-slot.getCount()); if (add>0){ slot.increment(add); rem-=add; } }
            for (int i=0;i<temp.size() && rem>0;i++) if (temp.get(i).isEmpty()) { ItemStack placed=out.copy(); int add=Math.min(rem, placed.getMaxCount()); placed.setCount(add); temp.set(i, placed); rem-=add; }
            if (rem>0) return false;
        }
        return true;
    }
    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            int rem = out.getCount();
            for (int slotId : new int[]{SLOT_OUTPUT_0,SLOT_OUTPUT_1,SLOT_OUTPUT_2}) { ItemStack slot=items.get(slotId); if(!slot.isEmpty() && ItemStack.canCombine(slot,out)){ int add=Math.min(rem, slot.getMaxCount()-slot.getCount()); if(add>0){ slot.increment(add); rem-=add; } } }
            for (int slotId : new int[]{SLOT_OUTPUT_0,SLOT_OUTPUT_1,SLOT_OUTPUT_2}) { if(rem<=0) break; if(items.get(slotId).isEmpty()){ ItemStack placed=out.copy(); int add=Math.min(rem, placed.getMaxCount()); placed.setCount(add); items.set(slotId, placed); rem-=add; } }
        }
    }

    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); Inventories.writeNbt(nbt, items); nbt.putLong("energy", energy); nbt.putInt("progress", progress); nbt.putInt("maxProgress", maxProgress); nbt.putInt("waterAmount", waterAmount); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); Inventories.readNbt(nbt, items); energy=Math.max(0L,Math.min(CAPACITY,nbt.getLong("energy"))); progress=Math.max(0,nbt.getInt("progress")); maxProgress=Math.max(1,nbt.getInt("maxProgress")); waterAmount=Math.max(0,Math.min(WATER_CAPACITY,nbt.getInt("waterAmount"))); }

    @Override public int size(){return items.size();} @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);} @Override public ItemStack getStack(int slot){return items.get(slot);} @Override public ItemStack removeStack(int slot,int amount){ItemStack out=Inventories.splitStack(items,slot,amount); if(!out.isEmpty()) markDirty(); return out;} @Override public ItemStack removeStack(int slot){ItemStack out=Inventories.removeStack(items,slot); markDirty(); return out;} @Override public void setStack(int slot,ItemStack stack){items.set(slot,stack); if(stack.getCount()>stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty();} @Override public void clear(){for(int i=0;i<items.size();i++) items.set(i,ItemStack.EMPTY);}
    @Override public boolean canPlayerUse(PlayerEntity player){return world!=null && world.getBlockEntity(pos)==this && player.squaredDistanceTo(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64.0;}
    @Override public int[] getAvailableSlots(Direction side){return side==Direction.UP?TOP_SLOTS:side==Direction.DOWN?BOTTOM_SLOTS:SIDE_SLOTS;}
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir){return slot==SLOT_INPUT || slot==SLOT_WATER || slot==SLOT_DISCHARGE || (slot>=SLOT_UPGRADE_0 && slot<SLOT_UPGRADE_0+UPGRADE_SLOTS);}
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir){return slot==SLOT_OUTPUT_0 || slot==SLOT_OUTPUT_1 || slot==SLOT_OUTPUT_2 || slot==SLOT_CELL_OUTPUT;}
    @Override public long getEuStored(){return energy;} @Override public long getEuCapacity(){return CAPACITY;} @Override public int getSinkTier(){return TIER;} @Override public int getSourceTier(){return 0;} @Override public boolean canInsert(Direction from){return true;} @Override public boolean canExtract(Direction to){return false;}
    @Override public long insertEu(long amount, Direction from, boolean simulate){ if(amount<=0)return 0; long free=CAPACITY-energy; if(free<=0)return 0; long accepted=Math.min(amount,free); if(!simulate&&accepted>0){energy+=accepted;markDirty();} return accepted; }
    @Override public long extractEu(long amount, Direction to, boolean simulate){return 0;}
    public PropertyDelegate getGuiProps(){return props;} public int getWaterAmount(){return waterAmount;}
    @Override public Text getDisplayName(){return Text.translatable("container.industrial_legacy.ore_washing_plant");}
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf){buf.writeBlockPos(pos);}
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player){return new OreWashingPlantScreenHandler(syncId, inv, this);}
}
