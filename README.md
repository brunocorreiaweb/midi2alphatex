# midi2alphatex

Real-time MIDI-to-AlphaTex converter for percussion instruments. Convert drum patterns from your MIDI controller (e-drum, USB keyboard, etc.) directly into **AlphaTex** notation in real-time.

## Overview

This Java application captures MIDI events from connected USB controllers and converts them into [AlphaTex](https://www.alphatab.net/) format, which is then streamed via WebSocket to a visual renderer. Perfect for music notation enthusiasts, composers, and percussionists who want to see their drum patterns transcribed in real-time.

### Key Features

✨ **Real-time MIDI Capture** — Listens to any connected USB MIDI device (e-drums, keyboards, controllers)  
✨ **Intelligent Simultaneity Detection** — Groups notes hit within 80ms as simultaneous (chords)  
✨ **AlphaTex Generation** — Converts MIDI data into AlphaTex notation with proper quantization  
✨ **WebSocket Streaming** — Broadcasts AlphaTex to connected clients in real-time  
✨ **Measure Tracking** — Automatically adds bar lines based on tempo and note duration  
✨ **BPM Support** — Configurable tempo (default 120 BPM)  

## WebSocket Renderer

This project sends AlphaTex data via WebSocket to the **solo-music** renderer:

🔗 **[solo-music/edit-alphatex](https://github.com/brunocorreiaweb/solo-music/tree/master/edit-alphatex)** — JS/React renderer that displays live sheet music notation  
🌐 **Live Renderer**: https://solo-music.dev-tools.pt/edit-alphatex/render

The renderer visualizes your MIDI input as professional sheet music in real-time.

## How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│ USB MIDI Controller (e-drum, keyboard, etc.)                    │
└────────────────────┬────────────────────────────────────────────┘
                     │ MIDI Events
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ Java MIDI System (javax.sound.midi)                             │
│ - Detects connected MIDI device                                 │
│ - Captures NOTE_ON events                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │ Raw MIDI Events
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ RealTimeAlphaTexReceiver                                        │
│ - Tracks timing of note events                                  │
│ - Groups simultaneous notes (within 80ms threshold)             │
│ - Quantizes durations based on tempo                            │
│ - Generates AlphaTex notation                                   │
│ - Maintains measure tracking                                    │
└────────────────────┬────────────────────────────────────────────┘
                     │ AlphaTex String
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ WebSocket Server (port 8080)                                    │
│ - Broadcasts AlphaTex to connected clients                      │
└────────────────────┬────────────────────────────────────────────┘
                     │ WebSocket Message
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ AlphaTex Renderer (Web UI)                                      │
│ https://solo-music.dev-tools.pt/edit-alphatex/render           │
│ - Displays sheet music notation in real-time                    │
└─────────────────────────────────────────────────────────────────┘
```

## Requirements

- **Java 25+** (uses Java records and text blocks)
- **Maven 3.6+**
- USB MIDI Controller (e-drum, keyboard, controller, etc.)
- A browser to view the AlphaTex renderer

### Dependencies

- `Java-WebSocket 1.5.3` — WebSocket server for real-time communication
- `SLF4J 2.0.7` — Logging (API and simple implementation)
- `javax.sound.midi` — Built-in Java MIDI support

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/brunocorreiaweb/midi2alphatex.git
cd midi2alphatex
```

### 2. Connect Your MIDI Device

Plug in your USB MIDI controller (e-drum, keyboard, etc.) before running the application.

### 3. Build the Project

```bash
mvn clean compile
```

### 4. Run the Application

```bash
mvn exec:java@run
```

Or compile to a JAR and run directly:

```bash
mvn clean package
java -cp target/midi2alphatex-1.0-SNAPSHOT.jar pt.brunocorreia.midi2alphatex.MidiToAlphaTexApp
```

## Usage

1. **Start the Java application**
   ```bash
   mvn exec:java@run
   ```
   Output:
   ```
   🎹 Initializing MIDI-to-AlphaTex Prototype...
   🎯 TARGET ACQUIRED: USB Midi Device
   🚀 WebSocket Server started on port 8080
   🚀 Listening for notes... Play something on your MIDI controller!
   Open https://solo-music.dev-tools.pt/edit-alphatex/render for AlphaTex WebSocket Renderer
   ```

2. **Open the Web Renderer**  
   Visit: https://solo-music.dev-tools.pt/edit-alphatex/render
   
   This page connects to your local WebSocket server and displays sheet music in real-time.

3. **Play Your MIDI Controller**  
   Hit drums, keys, pads, etc. on your connected MIDI device. The application will:
   - Capture the MIDI events
   - Convert them to AlphaTex notation
   - Send to the web renderer via WebSocket
   - Display the notation on the screen in real-time

4. **Stop the Application**  
   Press `Ctrl+C` to gracefully shut down the server and release port 8080.

## Configuration

### Tempo (BPM)

Edit the `bpm` field in `RealTimeAlphaTexReceiver`:

```java
private int bpm = 120;  // Change this value
```

The BPM affects:
- Note duration quantization
- Measure tracking
- AlphaTex tempo marking

### Simultaneity Threshold

Notes arriving within `SIMULTANEITY_THRESHOLD` milliseconds are grouped as a single chord:

```java
private static final long SIMULTANEITY_THRESHOLD = 80;  // ms
```

This prevents two fast drum hits (like hi-hat + snare) from being recorded as separate notes with gaps.

### MIDI Note Mapping

The `mapMidiToDrum()` method converts MIDI pitch numbers to drum names. Currently maps to pitch values:

```java
private String mapMidiToDrum(int pitch) {
    return String.valueOf(pitch);  // Customize as needed
}
```

You can extend this to map specific pitches to drum names:

```java
private String mapMidiToDrum(int pitch) {
    return switch(pitch) {
        case 35 -> "bd";    // Bass drum
        case 38 -> "sn";    // Snare
        case 46 -> "hho";   // Hi-hat open
        case 42 -> "hhc";   // Hi-hat closed
        default -> String.valueOf(pitch);
    };
}
```

## Architecture

### Main Classes

#### `MidiToAlphaTexApp.java`

Entry point of the application. Responsible for:
- Scanning connected MIDI devices
- Filtering out Java internal devices
- Opening the selected MIDI device
- Attaching the receiver
- Maintaining the application lifecycle

#### `RealTimeAlphaTexReceiver.java`

Core logic handler. Extends `WebSocketServer` and implements `Receiver`:

**Key Methods:**

- `send(MidiMessage, long)` — MIDI event handler
  - Captures NOTE_ON events
  - Calculates timing deltas between notes
  - Implements simultaneity detection
  - Groups simultaneous notes into chords
  - Quantizes note durations
  - Updates measure progress

- `quantizeDuration(long)` — Converts millisecond delta to AlphaTex duration
  - `:4` = Quarter note
  - `:8` = Eighth note
  - `:16` = Sixteenth note
  - `:32` = Thirty-second note
  - `:64` = Sixty-fourth note

- `updateMeasure(String)` — Tracks measure progress
  - Increments based on note duration
  - Adds bar lines (`|`) when reaching 1.0 (full 4/4 measure)

- `broadcastAlphaTex(String)` — Sends AlphaTex to all connected WebSocket clients

**WebSocket Methods:**

- `onOpen()` — Handles new client connections
- `onClose()` — Handles client disconnections
- `onError()` — Logs WebSocket errors
- `onStart()` — Logs server startup

## AlphaTex Output Example

When playing a simple drum pattern:

```alphatex
\tempo 120
bd sn.8 | hho.16 hho.16 | (bd sn)
```

This represents:
- Tempo of 120 BPM
- Bass drum, snare (eighth note), bar line
- Two hi-hat hits (sixteenths), bar line
- Bass drum and snare hit together (simultaneous)

## WebSocket Communication

The application sends raw AlphaTex strings via WebSocket on port 8080:

```javascript
// Client-side (in the web renderer)
const ws = new WebSocket('ws://localhost:8080');
ws.onmessage = (event) => {
    const alphaTex = event.data;
    console.log(alphaTex); // e.g., "\tempo 120 bd sn | ..."
    // Update the renderer with new notation
};
```

## Troubleshooting

### No MIDI Device Found

```
❌ No external MIDI controllers detected.
```

**Solutions:**
- Ensure your USB MIDI device is plugged in
- Try another USB port
- Check device drivers are installed
- Restart the application

### WebSocket Connection Failed

If the web renderer can't connect to `ws://localhost:8080`:
- Verify the Java application is running
- Check port 8080 is not blocked by a firewall
- Ensure you're accessing the renderer from the same machine or adjust `localhost` appropriately

### Notes Appear Out of Sync

If notes seem delayed or out of order:
- Increase `SIMULTANEITY_THRESHOLD` (e.g., 100ms instead of 80ms)
- Check your MIDI device's latency settings
- Verify BPM matches your playing speed

## Future Enhancements

- 📝 Configurable drum notation (map MIDI pitches to AlphaTex drum symbols)
- 🎛️ Interactive UI to configure BPM and threshold
- 📊 Recording and playback of MIDI sessions
- 🎼 Support for multiple instruments/channels
- 🔌 MIDI file export
- 🌐 Cross-machine WebSocket support (not just localhost)

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

You are free to use, modify, and distribute this software in your own projects, whether commercial or personal.

**Copyright (c) 2026 Bruno Correia**

## Links

- **AlphaTex Documentation**: https://www.alphatab.net/
- **AlphaTex Renderer**: https://solo-music.dev-tools.pt/edit-alphatex/render
- **Java MIDI API**: https://docs.oracle.com/javase/8/docs/technotes/guides/sound/
- **Java-WebSocket Library**: https://github.com/TooTallNate/Java-WebSocket
