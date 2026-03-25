import type { HybridObject } from 'react-native-nitro-modules'

export interface AudioRecordOptions {
  sampleRate: number
  channels: 1 | 2
  bitsPerSample: 8 | 16
  audioSource?: number // Android only
  wavFile?: string
}

export interface NitroAudioRecord
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  setup(options: AudioRecordOptions): void
  start(): void
  stop(): Promise<string>
  onData(callback: (data: ArrayBuffer) => void): void
}