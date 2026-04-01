package pt.brunocorreia.midi2alphatex;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import javax.sound.midi.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class RealTimeAlphaTexReceiver extends WebSocketServer implements Receiver {
  private long lastNoteTime = -1;
  private double measureProgress = 0; // Tracks 0.0 to 1.0 (one full bar)
  private int bpm = 120;

  private ArrayList<String> alphaTexBuffer = new ArrayList<>();

  public RealTimeAlphaTexReceiver(int port) {
    super(new InetSocketAddress(port));
    alphaTexBuffer.add("\\tempo " + bpm);
  }

  @Override
  public void send(MidiMessage message, long timeStamp) {
    if (message instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage) message;

      if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
        long currentTime = System.currentTimeMillis();
        String note = mapMidiToDrum(sm.getData1());

        if (lastNoteTime != -1) {
          long deltaMs = currentTime - lastNoteTime;

          // 1. Calculate the duration of the PREVIOUS note
          String duration = quantizeDuration(deltaMs);

          // 2. Track measure progress (Quarter note = 0.25)
          updateMeasure(duration);

          // 3. Add to buffer
          String last = alphaTexBuffer.removeLast();
          boolean addPipe = false;
          if (last.contains("|")) {
            addPipe = true;
            last = alphaTexBuffer.removeLast();
          }

          alphaTexBuffer.add(duration + " " + last);
          if (addPipe) {
            alphaTexBuffer.add("|");
          }
        }
        alphaTexBuffer.add(note);

        // Print updated string
        System.out.print("\rAlphaTex: " + String.join(" ", alphaTexBuffer));

        broadcastAlphaTex(String.join(" ", alphaTexBuffer));

        lastNoteTime = currentTime;
      }
    }
  }

  private String quantizeDuration(long deltaMs) {
    double msPerQuarter = 60000.0 / bpm;

    // Thresholds for 60 BPM:
    // 8th note ~ 500ms
    // 16th note ~ 250ms
    // 32nd note ~ 125ms

    if (deltaMs > msPerQuarter * 0.75) return ":4";
    if (deltaMs > msPerQuarter * 0.375) return ":8";
    if (deltaMs > msPerQuarter * 0.187) return ":16";
    if (deltaMs > msPerQuarter * 0.0935) return ":32";
    return ":64";
  }

  private void updateMeasure(String duration) {
    double val =
        switch (duration) {
          case ":4" -> 0.25;
          case ":8" -> 0.125;
          case ":16" -> 0.0625;
          case ":32" -> 0.03125;
          default -> 0;
        };

    measureProgress += val;

    // If we've reached a full 4/4 bar (1.0), add a pipe
    if (measureProgress >= 0.99) { // Using 0.99 to avoid floating point errors
      alphaTexBuffer.add("|");
      measureProgress = 0;
    }
  }

  private String mapMidiToDrum(int pitch) {
    return String.valueOf(pitch);
  }

  @Override
  public void close() {
    try {
      stop();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {}

  public void broadcastAlphaTex(String tex) {
    broadcast(tex);
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
  }

  @Override
  public void onStart() {
    System.out.println("🚀 WebSocket Server started on port 8080");
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    System.out.println("📩 Received: " + message);
    if (message.startsWith("bpm:")) {
      try {
        int newBpm = Integer.parseInt(message.substring(4).trim());
        updateSystemBpm(newBpm);
      } catch (NumberFormatException e) {
        System.err.println("❌ Invalid BPM format received");
      }
    } else {
      handleCommand(message);
    }
  }

  private void updateSystemBpm(int newBpm) {
    bpm = newBpm;
    alphaTexBuffer.removeFirst();
    alphaTexBuffer.addFirst("\\tempo " + bpm);
    broadcastAlphaTex(String.join(" ", alphaTexBuffer));
    System.out.println("⏱️ System BPM updated to: " + newBpm);
  }

  private void handleCommand(String command) {
    switch (command.toLowerCase()) {
      case "clear":
        alphaTexBuffer.clear();
        measureProgress = 0;
        lastNoteTime = -1;
        alphaTexBuffer.add("\\tempo " + bpm);
        break;

      case "undo_bar":
        boolean isFirst = true;

        while (!alphaTexBuffer.isEmpty()) {
          String removed = alphaTexBuffer.removeLast();
          if (removed.contains("|") && !isFirst) {
            alphaTexBuffer.add(removed);
            if (alphaTexBuffer.isEmpty()) {
              alphaTexBuffer.add("\\tempo " + bpm);
            }
            break;
          }
          isFirst = false;
        }
        lastNoteTime = -1;
        measureProgress = 0;
        broadcastAlphaTex(String.join(" ", alphaTexBuffer));
        break;
    }
  }
}
