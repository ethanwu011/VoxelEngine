package org.example.engine;

import org.example.voxel.Block;

public class Hotbar {
    public static final int SLOTS = 9;
    private final byte[] slots = new byte[SLOTS];
    private int selected = 0;

    public Hotbar() {
        slots[0] = Block.GRASS;
        slots[1] = Block.DIRT;
        slots[2] = Block.STONE;
        slots[3] = Block.COBBLESTONE;
        slots[4] = Block.PLANKS;
        slots[5] = Block.WOOD;
        slots[6] = Block.LEAVES;
        slots[7] = Block.SAND;
        slots[8] = Block.GRASS;
    }

    public byte getSelected() { return slots[selected]; }
    public int getSelectedSlot() { return selected; }
    public byte getSlot(int i) { return slots[i]; }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < SLOTS) selected = slot;
    }

    public void scroll(int dir) {
        selected = ((selected + dir) % SLOTS + SLOTS) % SLOTS;
    }
}