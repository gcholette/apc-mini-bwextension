package com.akai;

import java.util.ArrayList;

public class PadGrid {
    private ArrayList<Pad> pads = new ArrayList<Pad>();

    protected PadGrid() {
        for (int i = 0; i < 64; i++) {
            Pad newPad = new Pad(i);
            pads.add(newPad);
        }
    }
}
