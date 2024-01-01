package com.akai;

public class Pad {
    private int id;
    private LedColor ledColor;

    public enum LedColor {
        OFF(0),
        GREEN(1),
        GREEN_BLINK(2),
        RED(3),
        RED_BLINK(4),
        YELLOW(5),
        YELLOW_BLINK(6),
        UNASSIGNED(127);

        private final int value;

        LedColor(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    protected Pad(int id) {
        this.id = id;
        this.ledColor = LedColor.UNASSIGNED;
    }

    public Pad(Pad other) {
        this.id = other.id;
        this.ledColor = other.ledColor;
    }

    public int getId() {
        return id;
    }

    public void setLedColor(LedColor color) {
        this.ledColor = color;
    }

    public LedColor getLedColor() {
        return ledColor;
    }
}
