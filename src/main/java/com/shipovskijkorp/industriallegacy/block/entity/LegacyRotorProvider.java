package com.shipovskijkorp.industriallegacy.block.entity;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public interface LegacyRotorProvider {
    int getRotorDiameter();
    float getAngle();
    Identifier getRotorRenderTexture();
    Direction getFacing();
}
