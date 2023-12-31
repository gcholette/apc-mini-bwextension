package com.akai;

public class Pad {
    private int id;
    private LedColor ledColor = LedColor.OFF;

    public enum LedColor {
        OFF,
        GREEN,
        GREEN_BLINK,
        RED,
        RED_BLINK,
        YELLOW,
        YELLOW_BLINK
    }

    protected Pad(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
