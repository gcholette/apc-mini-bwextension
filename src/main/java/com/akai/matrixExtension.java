package com.akai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.akai.Pad.LedColor;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.ControllerExtension;

public class matrixExtension extends ControllerExtension {
   ControllerHost host;
   Integer[] verticalPadsArray = { 15, 23, 31, 39, 47, 55, 63 };
   Integer[] horizontalPadsArray = { 0, 1, 2, 3, 4, 5, 6 };
   Integer[] navPads = { 64, 65, 66, 67, 68 };
   Integer[] ctrlPads = { 68, 69, 70, 71 };
   final Integer shift = 98;
   final Integer playStopPad = 7;


   PadGrid padGrid;

   enum VerticalButtonLed {
      OFF,
      RED
   }

   enum HorizonButtonLed {
      OFF,
      RED
   }

   enum Mode {
      CLIP_STOP,
      SOLO,
      REC_ARM,
      MUTE,
      SELECT,
      VOID_1,
      VOID_2,
      STOP_ALL_CLIPS
   }

   Mode selectedMode = Mode.CLIP_STOP;

   List<Integer> verticalPads = new ArrayList<>(Arrays.asList(verticalPadsArray));
   List<Integer> horizontalPads = new ArrayList<>(Arrays.asList(horizontalPadsArray));

   CursorTrack cursorTrack;
   MasterTrack masterTrack;
   TrackBank trackBank;
   Clip cursorClip;

   protected matrixExtension(final matrixExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   private void animation1() {
      int increment = 50;
      int delay = 2500;
      for (int i = 0; i < horizontalPadsArray.length; i++) {
         final int finali = i;
         host.scheduleTask(() -> host.getMidiOutPort(0).sendMidi(0x90, horizontalPadsArray[finali], 3),
               delay + i * increment);
         host.scheduleTask(() -> host.getMidiOutPort(0).sendMidi(0x90, horizontalPadsArray[finali], 3),
               delay + i * increment);
      }
      for (int i = 0; i < verticalPadsArray.length; i++) {
         final int finali = i;
         host.scheduleTask(() -> host.getMidiOutPort(0).sendMidi(0x90, verticalPadsArray[finali], 3),
               delay + i * increment + horizontalPadsArray.length * increment);
         host.scheduleTask(() -> host.getMidiOutPort(0).sendMidi(0x90, verticalPadsArray[finali], 3),
               delay + i * increment + horizontalPadsArray.length * increment);
      }
   }

   @Override
   public void init() {
      this.host = getHost();

      mTransport = host.createTransport();
      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
      host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));

      initPads();

      this.trackBank = host.createTrackBank(7, 0, 7);
      this.cursorTrack = host.createCursorTrack(0, 0);
      this.masterTrack = host.createMasterTrack(0);
      this.cursorClip = host.createLauncherCursorClip(7, 7);

      host.showPopupNotification("matrix Initialized");
   }

   private void initPads() {
      this.padGrid = new PadGrid(host);

      for (int i = 0; i < 64; i++) {
         padGrid.setPadColor(i, LedColor.OFF);
      }
      for (int i = 0; i < horizontalPads.size(); i++) {
         padGrid.setPadColor(horizontalPads.get(i), LedColor.RED);
      }
      for (int i = 0; i < verticalPads.size(); i++) {
         padGrid.setPadColor(verticalPads.get(i), LedColor.RED);
      }

      // animation1();
      padGrid.render();
   }

   @Override
   public void exit() {
      host.println("Bye");
      host.showPopupNotification("matrix Exited");
   }

   @Override
   public void flush() {

   }

   private void onMidi0(ShortMidiMessage msg) {
      int data1 = msg.getData1();
      int data2 = msg.getData2();
      int status = msg.getStatusByte();

      host.println("Midi");
      host.println("status " + status);
      host.println("data1 " + data1);
      host.println("data2 " + data2);

      // Center pads
      if (data1 >= 0 && data1 < 64 && !horizontalPads.contains(data1) && !verticalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            padGrid.setPadColor(data1, LedColor.GREEN);
         } else if (status == ShortMidiMessage.NOTE_OFF) {
            padGrid.setPadColor(data1, LedColor.OFF);
         }
      }

      if (data1 == 7) {
         mTransport.stop();
      }

      // Horizontal pads
      if (horizontalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            padGrid.setPadColor(data1, LedColor.GREEN);
            Track track = trackBank.getItemAt(data1);
            cursorTrack.selectChannel(track);
         } else if (status == ShortMidiMessage.NOTE_OFF) {
            padGrid.setPadColor(data1, LedColor.RED);
         }
      }

      // Vertical pads
      if (verticalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            padGrid.setPadColor(data1, LedColor.GREEN);
            trackBank.sceneBank().launch(6 - verticalPads.indexOf(data1));
         } else if (status == ShortMidiMessage.NOTE_OFF) {
            padGrid.setPadColor(data1, LedColor.RED);
         }
      }

      if (data1 >= 82 && data1 <= 89) {
         switch (data1) {
            case 82:
               break;
            case 83:
               break;
            case 84:
               break;
            case 85:
               break;
            case 86:
               break;
            case 87:
               break;
            case 88:
               break;
            case 89:
               break;
            default:
         }
      }

      padGrid.render();
   }

   private void onSysex0(final String data) {
      if (data.equals("f07f7f0605f7"))
         mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
         mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
         mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
         mTransport.play();
      else if (data.equals("f07f7f0606f7"))
         mTransport.record();
   }

   private Transport mTransport;
}
