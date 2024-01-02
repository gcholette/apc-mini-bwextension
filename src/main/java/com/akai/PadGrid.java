package com.akai;

import java.util.ArrayList;

import com.akai.Pad.LedColor;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

public class PadGrid {
    ControllerHost host;
    MidiOut midiOutPort;
    private ArrayList<Pad> pads = new ArrayList<Pad>();

    /**
     * Cache of which pads have been rendered.
     * Prevents rendering pads that are already rendered.
     */
    private ArrayList<Pad> renderedPads = new ArrayList<Pad>();

    protected PadGrid(ControllerHost extensionHost) {
        this.host = extensionHost;
        this.midiOutPort = host.getMidiOutPort(0);

        for (int i = 0; i < 64; i++) {
            pads.add(new Pad(i));
            renderedPads.add(new Pad(i));
        }
    }

    public void setPadColor(int index, LedColor color) {
        Pad pad = pads.get(index);
        pad.setLedColor(color);
    }

    public void render() {
        ArrayList<Pad> padsToRender = new ArrayList<Pad>();

        for (int i = 0; i < pads.size(); i++) {
            if (pads.get(i).getLedColor() != renderedPads.get(i).getLedColor()) {
                padsToRender.add(pads.get(i));
            }
        }

        for (int i = 0; i < padsToRender.size(); i++) {
            Pad padToRender = padsToRender.get(i);
            renderedPads.set(padToRender.getId(), new Pad(padToRender));
            midiOutPort.sendMidi(0x90, padToRender.getId(), padToRender.getLedColor().getValue());
            midiOutPort.sendMidi(0x90, padToRender.getId(), padToRender.getLedColor().getValue());
        }
    }
}
