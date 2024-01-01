package com.akai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.akai.Pad.LedColor;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.ControllerExtension;

public class matrixExtension extends ControllerExtension {
   private Transport mTransport;
   private ControllerHost host;
   private Integer[] verticalPadsArray = { 15, 23, 31, 39, 47, 55, 63 };
   private Integer[] horizontalPadsArray = { 0, 1, 2, 3, 4, 5, 6 };

   final int[][] padColumns = {
         { 8, 16, 24, 32, 40, 48, 56 },
         { 9, 17, 25, 33, 41, 49, 57 },
         { 10, 18, 26, 34, 42, 50, 58 },
         { 11, 19, 27, 35, 43, 51, 59 },
         { 12, 20, 28, 36, 44, 52, 60 },
         { 13, 21, 29, 37, 45, 53, 61 },
         { 14, 22, 30, 38, 46, 54, 62 }
   };

   private List<Integer> slotsWithHasContent = new ArrayList<>();
   private List<Integer> slotsWithIsPlaying = new ArrayList<>();
   private List<Integer> slotsWithIsPlaybackQueued = new ArrayList<>();
   private List<Integer> slotsWithIsRecording = new ArrayList<>();
   private List<Integer> slotsWithIsRecordingQueued = new ArrayList<>();

   private final int shift = 98;
   private final int playStopPad = 7;

   private boolean isPlaying = false;
   private boolean shiftIsPressed = false;

   private PadGrid padGrid;

   enum Mode {
      CLIP_STOP,
      SOLO,
      REC_ARM,
      MUTE,
      SELECT,
      VOID_1,
      VOID_2
   }

   private Mode selectedMode = Mode.CLIP_STOP;
   private final int CLIP_STOP_ID = 82;
   private final int SOLO_ID = 83;
   private final int REC_ARM_ID = 84;
   private final int MUTE_ID = 85;
   private final int SELECT_ID = 86;
   private final int VOID_1_ID = 87;
   private final int VOID_2_ID = 88;
   private final int STOP_ALL_CLIPS_ID = 89;

   private List<Integer> verticalPads = new ArrayList<>(Arrays.asList(verticalPadsArray));
   private List<Integer> horizontalPads = new ArrayList<>(Arrays.asList(horizontalPadsArray));

   private CursorTrack cursorTrack;
   private MasterTrack masterTrack;
   private TrackBank trackBank;
   private PinnableCursorClip cursorClip;
   private ClipLauncherSlotBank cursorClipSlotBank;
   private ClipLauncherSlotBank clipSlotBank;

   protected matrixExtension(final matrixExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   // private void animation1() {
   // int increment = 50;
   // int delay = 2500;
   // for (int i = 0; i < horizontalPadsArray.length; i++) {
   // final int finali = i;
   // host.scheduleTask(() -> getMidiOutPort(0).sendMidi(0x90,
   // horizontalPadsArray[finali], 3),
   // delay + i * increment);
   // host.scheduleTask(() -> getMidiOutPort(0).sendMidi(0x90,
   // horizontalPadsArray[finali], 3),
   // delay + i * increment);
   // }
   // for (int i = 0; i < verticalPadsArray.length; i++) {
   // final int finali = i;
   // host.scheduleTask(() -> getMidiOutPort(0).sendMidi(0x90,
   // verticalPadsArray[finali], 3),
   // delay + i * increment + horizontalPadsArray.length * increment);
   // host.scheduleTask(() -> getMidiOutPort(0).sendMidi(0x90,
   // verticalPadsArray[finali], 3),
   // delay + i * increment + horizontalPadsArray.length * increment);
   // }
   // }

   private void registerPadValueObserver(int x, int y, List<Integer> list, boolean value) {
      final int id = padColumns[x][6 - y];
      if (value == true) {
         if (!list.contains(id)) {
            list.add(id);
         }
      } else {
         if (list.contains(id)) {
            final int index = list.indexOf(id);
            list.remove(index);
         }
      }
      renderClipPads();
   }

   private void setupClipPads() {
      for (int i = 0; i < padColumns.length; i++) {
         Track track = trackBank.getItemAt(i);
         clipSlotBank = track.clipLauncherSlotBank();
         for (int j = 0; j < 7; j++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(j);
            final int finali = i;
            final int finalj = j;

            slot.hasContent().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithHasContent, x);
            });

            slot.isPlaybackQueued().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithIsPlaybackQueued, x);
            });

            slot.isPlaying().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithIsPlaying, x);
            });

            slot.isRecording().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithIsRecording, x);
            });

            slot.isRecordingQueued().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithIsRecordingQueued, x);
            });
         }
      }
   }

   private void renderClipPads() {
      for (int i = 0; i < padColumns.length; i++) {
         for (int j = 0; j < 7; j++) {
            final int id = padColumns[i][6 - j];

            if (slotsWithIsPlaying.contains(id)) {
               padGrid.setPadColor(id, LedColor.GREEN);
            } else if (slotsWithIsPlaybackQueued.contains(id)) {
               padGrid.setPadColor(id, LedColor.GREEN_BLINK);
            } else if (slotsWithIsRecording.contains(id)) {
               padGrid.setPadColor(id, LedColor.RED);
            } else if (slotsWithIsRecordingQueued.contains(id)) {
               padGrid.setPadColor(id, LedColor.RED_BLINK);
            } else if (slotsWithHasContent.contains(id)) {
               padGrid.setPadColor(id, LedColor.YELLOW);
            } else {
               padGrid.setPadColor(id, LedColor.OFF);
            }
         }
      }
      padGrid.render();
   }

   @Override
   public void init() {
      this.host = getHost();
      initPads();

      mTransport = host.createTransport();
      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
      host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));

      this.trackBank = host.createTrackBank(7, 0, 7);
      this.cursorTrack = host.createCursorTrack(0, 7);
      this.masterTrack = host.createMasterTrack(0);
      this.cursorClip = cursorTrack.createLauncherCursorClip(7, 7);
      this.cursorClipSlotBank = cursorTrack.clipLauncherSlotBank();

      setupClipPads();
      renderSceneButtons(selectedMode);

      mTransport.isPlaying().addValueObserver(x -> {
         isPlaying = x;
         if (isPlaying == true) {
            padGrid.setPadColor(playStopPad, LedColor.GREEN_BLINK);
         } else {
            padGrid.setPadColor(playStopPad, LedColor.OFF);
         }
         padGrid.render();
      });

      host.showPopupNotification("Matrix Initialized");
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
      for (int i = 82; i < 89; i++) {
         updateLed(i, 0);
      }

      padGrid.render();
   }

   @Override
   public void exit() {
      host.showPopupNotification("matrix Exited");
   }

   @Override
   public void flush() {

   }

   /**
    * @param padId 8 - 62
    */
   private void launchClip(int padId) {
      int clipId = 6 - Utils.getPadRow(padId, padColumns);
      cursorClipSlotBank.select(clipId);
      cursorClipSlotBank.launch(clipId);
   }

   private void stopClip() {
      cursorClipSlotBank.stop();
   }

   /**
    * @param padId 0 - 7
    */
   private void selectTrack(int padId) {
      int trackId = Utils.getPadColum(padId, padColumns);
      Track track = trackBank.getItemAt(trackId);
      cursorTrack.selectChannel(track);
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
      if (data1 > 7 && data1 < 63 && !verticalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            selectTrack(data1);
            launchClip(data1);
         }
      }

      // Horizontal pads
      if (horizontalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            padGrid.setPadColor(data1, LedColor.GREEN);
            Track track = trackBank.getItemAt(data1);
            cursorTrack.selectChannel(track);

            switch (selectedMode) {
               case CLIP_STOP:
                  stopClip();
                  break;
               case SOLO:
                  break;
               case REC_ARM:
                  break;
               case MUTE:
                  break;
               case SELECT:
                  break;
               case VOID_1:
                  break;
               case VOID_2:
                  break;
               default:
                  break;
            }

         } else if (status == ShortMidiMessage.NOTE_OFF) {
            padGrid.setPadColor(data1, LedColor.RED);
         }
      }

      // Vertical pads
      if (verticalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            padGrid.setPadColor(data1, LedColor.GREEN);
            trackBank.sceneBank().launchScene(6 - verticalPads.indexOf(data1));
            trackBank.sceneBank().getScene(6 - verticalPads.indexOf(data1)).selectInEditor();
         } else if (status == ShortMidiMessage.NOTE_OFF) {
            padGrid.setPadColor(data1, LedColor.RED);
         }
      }

      if (data1 >= CLIP_STOP_ID && data1 <= VOID_2_ID) {
         int modeId = modeToId(selectedMode);
         updateLed(modeId, 0);
         switch (data1) {
            case CLIP_STOP_ID:
               this.selectedMode = Mode.CLIP_STOP;
               break;
            case SOLO_ID:
               this.selectedMode = Mode.SOLO;
               break;
            case REC_ARM_ID:
               this.selectedMode = Mode.REC_ARM;
               break;
            case MUTE_ID:
               this.selectedMode = Mode.MUTE;
               break;
            case SELECT_ID:
               this.selectedMode = Mode.SELECT;
               break;
            case VOID_1_ID:
               this.selectedMode = Mode.VOID_1;
               break;
            case VOID_2_ID:
               this.selectedMode = Mode.VOID_2;
               break;
            case STOP_ALL_CLIPS_ID:
               break;
            default:
         }
      }

      if (data1 == playStopPad && status == ShortMidiMessage.NOTE_ON) {
         if (isPlaying) {
            mTransport.stop();
         } else {
            mTransport.play();
         }
      }

      renderSceneButtons(selectedMode);
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

   private void updateLed(int id, int color) {
      host.getMidiOutPort(0).sendMidi(0x90, id, color);
   }

   private void renderSceneButtons(Mode mode) {
      int modeId = modeToId(mode);
      updateLed(modeId, 1);
   }

   private int modeToId(Mode mode) {
      switch (mode) {
         case CLIP_STOP:
            return CLIP_STOP_ID;
         case SOLO:
            return SOLO_ID;
         case REC_ARM:
            return REC_ARM_ID;
         case MUTE:
            return MUTE_ID;
         case SELECT:
            return SELECT_ID;
         case VOID_1:
            return VOID_1_ID;
         case VOID_2:
            return VOID_2_ID;
         default:
            return -1;
      }
   }
}
