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
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
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
   private List<Integer> slotsWithIsStopQueued = new ArrayList<>();
   private List<Integer> slotsWithIsRecording = new ArrayList<>();
   private List<Integer> slotsWithIsRecordingQueued = new ArrayList<>();

   private final int shift = 98;
   private final int playStopPad = 7;

   private boolean isPlaying = false;
   private boolean shiftIsPressed = false;
   private int selectedTrackId = 0;

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

   private int selectedFaderCtrl = 68;
   private final int VOLUME_ID = 68;
   private final int PAN_ID = 69;
   private final int SEND_ID = 70;
   private final int DEVICE_ID = 71;

   private List<Integer> verticalPads = new ArrayList<>(Arrays.asList(verticalPadsArray));
   private List<Integer> horizontalPads = new ArrayList<>(Arrays.asList(horizontalPadsArray));

   private CursorTrack cursorTrack;
   private MasterTrack masterTrack;
   private TrackBank trackBank;
   private PinnableCursorClip cursorClip;
   private ClipLauncherSlotBank cursorClipSlotBank;
   private ClipLauncherSlotBank clipSlotBank;
   private CursorRemoteControlsPage remoteControlsPage;

   protected matrixExtension(final matrixExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
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
      this.remoteControlsPage = cursorTrack.createCursorRemoteControlsPage(9);

      setupClipPads();
      renderSceneButtons(selectedMode);
      renderFaderCtrlButtons(selectedFaderCtrl);

      mTransport.isPlaying().addValueObserver(this::transportIsPlayingListener);
      cursorTrack.position().addValueObserver(this::cursorTrackPositionListener);

      host.showPopupNotification("Matrix Initialized");
   }

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

            track.isQueuedForStop().addValueObserver(x -> {
               registerPadValueObserver(finali, finalj, slotsWithIsStopQueued, x);
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

            if (slotsWithIsStopQueued.contains(id)) {
               padGrid.setPadColor(id, LedColor.RED);
            } else if (slotsWithIsPlaying.contains(id)) {
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

   private void cursorTrackPositionListener(int index) {
      host.println("Cursor Index: " + index);
      if (index >= 0) {
         this.selectedTrackId = index;
         for (int i = 0; i < 8; i++) {
            if (i == index) {
               padGrid.setPadColor(i, LedColor.GREEN);
            } else {
               padGrid.setPadColor(i, LedColor.RED);
            }
         }
         padGrid.render();
      }
   }

   private void transportIsPlayingListener(boolean isPlaying2) {
      if (isPlaying2 == true) {
         padGrid.setPadColor(playStopPad, LedColor.GREEN);
      } else {
         padGrid.setPadColor(playStopPad, LedColor.RED);
      }
      this.isPlaying = isPlaying2;
      padGrid.render();
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
      int trackId = Utils.getPadColum(padId, padColumns);
      int clipId = 6 - Utils.getPadRow(padId, padColumns);
      Track track = trackBank.getItemAt(trackId);
      ClipLauncherSlotBank bank = track.clipLauncherSlotBank();
      bank.select(clipId);
      bank.launch(clipId);
   }

   /**
    * @param padId 8 - 62
    */
   private void releaseClip(int padId) {
      int trackId = Utils.getPadColum(padId, padColumns);
      int clipId = 6 - Utils.getPadRow(padId, padColumns);
      Track track = trackBank.getItemAt(trackId);
      ClipLauncherSlotBank bank = track.clipLauncherSlotBank();
      bank.getItemAt(clipId).launchRelease();
   }

   /**
    * @param padId 0 - 7
    */
   private void stopTrack(int padId) {
      Track track = trackBank.getItemAt(padId);
      ClipLauncherSlotBank bank = track.clipLauncherSlotBank();
      bank.stop();
   }

   /**
    * @param padId 0 - 7
    */
   private void selectTrack(int padId) {
      Track track = trackBank.getItemAt(padId);
      cursorTrack.selectChannel(track);
      selectedTrackId = padId;
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
            launchClip(data1);
         } else if (status == ShortMidiMessage.NOTE_OFF) {
            releaseClip(data1);
         }
      }

      // Horizontal pads
      if (horizontalPads.contains(data1)) {
         if (status == ShortMidiMessage.NOTE_ON) {
            switch (selectedMode) {
               case CLIP_STOP:
                  stopTrack(data1);
                  break;
               case SOLO:
                  break;
               case REC_ARM:
                  break;
               case MUTE:
                  break;
               case SELECT:
                  selectTrack(data1);
                  break;
               case VOID_1:
                  break;
               case VOID_2:
                  break;
               default:
                  break;
            }

         } else if (status == ShortMidiMessage.NOTE_OFF) {
            if (data1 == selectedTrackId) {
               // padGrid.setPadColor(data1, LedColor.YELLOW);
            } else {
               // padGrid.setPadColor(data1, LedColor.RED);
            }
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

      if (data1 >= 68 && data1 <= 71) {
         this.selectedFaderCtrl = data1;
      }

      if (data1 == playStopPad && status == ShortMidiMessage.NOTE_ON) {
         if (isPlaying) {
            mTransport.stop();
         } else {
            mTransport.play();
         }
      }

      // Sliders
      if (status == 176) {
         int faderIndex = data1 - 48;
         float normalizedValue = data2 / 127.0f;
         if (faderIndex == 7) {
            // masterTrack.volume().set(normalizedValue);
         } else if (faderIndex == 8) {
            masterTrack.volume().set(normalizedValue);
         } else {
            switch (selectedFaderCtrl) {
               case VOLUME_ID:
                  trackBank.getItemAt(faderIndex).volume().set(normalizedValue);
                  break;
               case PAN_ID:
                  trackBank.getItemAt(faderIndex).pan().set(normalizedValue);
                  break;
               case DEVICE_ID:
                  remoteControlsPage.getParameter(faderIndex).set(normalizedValue);
                  break;
               default:
                  break;
            }
         }
      }

      // Never render when the sliders update
      // Too spammy for the controller
      if (status != 176) {
         renderSceneButtons(selectedMode);
         renderFaderCtrlButtons(selectedFaderCtrl);
         padGrid.render();
      }
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

   private void renderFaderCtrlButtons(int id) {
      updateLed(68, 0);
      updateLed(69, 0);
      updateLed(70, 0);
      updateLed(71, 0);
      updateLed(id, 1);
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
