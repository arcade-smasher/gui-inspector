package net.caffeinemc.mods.sodium.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record Dim2i(int x, int y, int width, int height) {}