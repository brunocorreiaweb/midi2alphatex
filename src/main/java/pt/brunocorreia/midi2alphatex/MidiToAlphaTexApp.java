import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import pt.brunocorreia.midi2alphatex.RealTimeAlphaTexReceiver;

void main() {
  IO.println("🎹 Initializing MIDI-to-AlphaTex Prototype...");

  MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
  if (infos.length == 0) {
    System.err.println("No MIDI devices found. Check your USB connection!");
    return;
  }

  MidiDevice inputDevice = null;

  // Loop through devices to find a suitable Input (Transmitter)
  for (MidiDevice.Info info : infos) {
    try {
      MidiDevice device = MidiSystem.getMidiDevice(info);

      if (device.getMaxTransmitters() != 0
          && !info.getName().toLowerCase().contains("sequencer")
          && !info.getName().toLowerCase().contains("java")) {

        System.out.println("🎯 TARGET ACQUIRED: " + info.getName());
        inputDevice = device;
        break;
      }
    } catch (MidiUnavailableException e) {
      System.err.println("Could not access device: " + info.getName());
    }
  }

  RealTimeAlphaTexReceiver myReceiver = new RealTimeAlphaTexReceiver(8080);
  myReceiver.start();
  // Add this inside your main method
  Runtime.getRuntime()
      .addShutdownHook(
          new Thread(
              () -> {
                System.out.println("\n🛑 Shutting down WebSocket server...");
                try {
                  if (myReceiver != null) {
                    myReceiver.stop(1000);
                    System.out.println("✅ Port 8080 released.");
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }));

  if (inputDevice != null) {
    try {
      inputDevice.open();

      Transmitter transmitter = inputDevice.getTransmitter();
      transmitter.setReceiver(myReceiver);

      IO.println("🚀 Listening for notes... Play something on your MIDI controller!");
      IO.println("(Press Ctrl+C to stop)");

      // Keep the main thread alive while the background MIDI thread works
      while (true) {
        Thread.sleep(1000);
      }

    } catch (MidiUnavailableException | InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (inputDevice.isOpen()) inputDevice.close();
    }
  } else {
    IO.println("❌ No external MIDI controllers detected.");
  }
}
