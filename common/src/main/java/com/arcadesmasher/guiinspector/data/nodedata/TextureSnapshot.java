package com.arcadesmasher.guiinspector.data.nodedata;

public final class TextureSnapshot {
	public static final TextureSnapshot EMPTY = new TextureSnapshot(-1, -1, -1);
	private final int glId;
	private final int width;
	private final int height;

	public TextureSnapshot(int glId, int width, int height) {
		this.glId = glId;
		this.width = width;
		this.height = height;
	}

	public int glId() {
		return glId;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}
}