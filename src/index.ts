import { NitroModules } from 'react-native-nitro-modules'
import type { NitroAudioRecord as NitroAudioRecordSpec } from './specs/nitro-audio-record.nitro'

export const NitroAudioRecord =
  NitroModules.createHybridObject<NitroAudioRecordSpec>('NitroAudioRecord')