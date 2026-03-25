# react-native-nitro-audio-record

A high-performance audio recording library for React Native powered by **Nitro Modules**.

## Relationship with `react-native-audio-record`

This library is a **modernized, high-performance successor** to the original [react-native-audio-record](https://github.com/goodatlas/react-native-audio-record). 

While it provides similar functionality, it has been completely rewritten to leverage the **New React Native Architecture** and **Nitro Modules** for zero-copy JSI communication. It is designed to be faster, type-safe, and future-proof.

- **Turbo Performance**: Zero-copy data transfer using JSI `ArrayBuffer`.
- **Modern Architecture**: Built for the new React Native architecture (TurboModules/Fabric).
- **TypeScript First**: Fully type-safe API.
- **Real-time Streaming**: Get audio chunks as they are recorded.

## Use Cases

- **Real-time Speech Recognition**: Stream PCM buffers directly to APIs like Google Cloud Speech-to-Text, OpenAI Whisper, or Deepgram.
- **Audio Visualization**: Draw real-time waveforms or frequency spectrums using the zero-copy `ArrayBuffer`.
- **Custom Audio Processing**: Apply Noise Suppression, Gain Control, or other DSP algorithms in JavaScript or C++.
- **Accurate Audio Scoring (GOP)**: High-fidelity audio capture for language learning apps that require precise PCM data.
- **Low-Latency VOIP**: Lightweight recording for real-time communication.

## Installation

```sh
npm install react-native-nitro-audio-record react-native-nitro-modules
npx pod-install
```

## Usage

```typescript
import { NitroAudioRecord } from 'react-native-nitro-audio-record';

// Initialize with options
NitroAudioRecord.setup({
  sampleRate: 16000,  // default 44100
  channels: 1,        // 1 or 2
  bitsPerSample: 16,  // 8 or 16
  wavFile: 'test.wav' // saved in document directory
});

// Listen for real-time audio data (ArrayBuffer)
NitroAudioRecord.onData((data: ArrayBuffer) => {
  // Direct zero-copy access to PCM data
  console.log('Chunk size:', data.byteLength);
});

// Start recording
NitroAudioRecord.start();

// Stop recording and get the file path
const audioFile = await NitroAudioRecord.stop();
console.log('Saved to:', audioFile);
```

## Migration from `react-native-audio-record`

If you are coming from the original `react-native-audio-record`, here are the key changes:

| Field | original `react-native-audio-record` | `react-native-nitro-audio-record` |
| --- | --- | --- |
| **Import** | `import AudioRecord from '...'` | `import { NitroAudioRecord } from '...'` |
| **Initialization** | `AudioRecord.init(options)` | `NitroAudioRecord.setup(options)` |
| **Real-time Data** | Event Listener (`.on('data', ...)`) | Callback (`.onData(...)`) |
| **Data Format** | Base64 String | `ArrayBuffer` (Fast JSI) |
| **New Arch** | Bridge (Bridge-based) | Nitro (New Arch / JSI) |

### Code Example (Before vs After)

**Before:**
```javascript
import AudioRecord from 'react-native-audio-record';
AudioRecord.init(options);
AudioRecord.on('data', base64Data => {
  const buffer = Buffer.from(base64Data, 'base64');
});
```

**After:**
```typescript
import { NitroAudioRecord } from 'react-native-nitro-audio-record';
NitroAudioRecord.setup(options);
NitroAudioRecord.onData(arrayBuffer => {
  // Use arrayBuffer directly without decoding!
});
```

## License

MIT
