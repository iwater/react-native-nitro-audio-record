# react-native-nitro-audio-record

一个基于 **Nitro Modules** 的高性能 React Native 音频录制库。

## 与 `react-native-audio-record` 的关系

本作是 [react-native-audio-record](https://github.com/goodatlas/react-native-audio-record) 的**现代化、高性能继任者**。

虽然它们的功能相似，但本项目使用 **React Native 新架构** 和 **Nitro Modules** 进行了完全重写。通过零拷贝的 JSI 通信，它提供了更快的速度、更强的类型安全性，并完美适配 React Native 的未来发展方向。

- **Turbo 级性能**：使用 JSI `ArrayBuffer` 实现**零拷贝**数据传输，大幅降低 CPU 和内存开销。
- **现代化架构**：专为 React Native 新架构（TurboModules/Fabric）设计。
- **TypeScript 优先**：提供完整的类型安全 API。
- **实时数据流**：在录制过程中实时获取 PCM 音频块，适用于语音识别、波形绘制等场景。

## 适用场景

- **实时语音识别**：直接将 PCM 缓冲区流式传输给 Google Cloud Speech-to-Text、OpenAI Whisper 或阿里云语音识别等 API。
- **音频可视化**：利用零拷贝的 `ArrayBuffer` 实时绘制音频波形或频谱图。
- **自定义音频处理**：在 JavaScript 或 C++ 中实现降噪、音量增益或其他 DSP 算法。
- **口语测评 (GOP)**：由于能获取精准的 PCM 数据，非常适合需要高保真采样进行发音打分的教育类应用。
- **低延迟 VOIP**：为实时通讯提供轻量级的音频采集。

## 安装

```sh
npm install react-native-nitro-audio-record react-native-nitro-modules
npx pod-install
```

## 使用方法

```typescript
import { NitroAudioRecord } from 'react-native-nitro-audio-record';

// 使用配置项进行初始化
NitroAudioRecord.setup({
  sampleRate: 16000,  // 默认 44100
  channels: 1,        // 1 或 2，默认 1
  bitsPerSample: 16,  // 8 或 16，默认 16
  wavFile: 'test.wav' // 音频将保存到文档目录下的该文件名
});

// 监听实时音频数据（ArrayBuffer）
NitroAudioRecord.onData((data: ArrayBuffer) => {
  // 直接访问零拷贝的 PCM 原始数据
  console.log('接收到音频数据块大小:', data.byteLength);
});

// 开始录音
NitroAudioRecord.start();

// 停止录音并获取文件路径
const audioFile = await NitroAudioRecord.stop();
console.log('音频文件已保存到:', audioFile);
```

## 配置项 (AudioRecordOptions)

| 属性 | 类型 | 描述 |
| --- | --- | --- |
| `sampleRate` | `number` | 采样率 (例如 16000, 44100) |
| `channels` | `1 \| 2` | 声道数 (1 为单声道, 2 为立体声) |
| `bitsPerSample` | `8 \| 16` | 每个样本的位数 (8 或 16) |
| `wavFile` | `string` | 保存的文件名 (例如 'audio.wav') |
| `audioSource` | `number` | (仅 Android) 音频源常数 |

## 说明

与传统的 `react-native-audio-record` 不同，本项目利用 Nitro Modules 直接在 JS 运行时和原生端之间传递 `ArrayBuffer`。这意味着：
1. **无需 Base64 编解码**：避免了频繁编解码带来的性能抖动。
2. **内存占用极低**：音频缓冲区的管理在底层更加高效。
3. **支持同步调用**：大部分控制操作均为同步执行，响应极其灵敏。

## 从 `react-native-audio-record` 迁移

如果你是从原版 `react-native-audio-record` 迁移过来的，以下是主要的 API 差异：

| 变更项 | 原版 `react-native-audio-record` | `react-native-nitro-audio-record` |
| --- | --- | --- |
| **导入方式** | `import AudioRecord from '...'` | `import { NitroAudioRecord } from '...'` |
| **初始化方法** | `AudioRecord.init(options)` | `NitroAudioRecord.setup(options)` |
| **数据监听** | 事件监听器 (`.on('data', ...)`) | 回调函数 (`.onData(...)`) |
| **数据格式** | Base64 字符串 | `ArrayBuffer` (零拷贝 JSI) |
| **架构支持** | 传统 Bridge | Nitro (新架构 / JSI) |

### 代码对比示例

**迁移前：**
```javascript
import AudioRecord from 'react-native-audio-record';

AudioRecord.init(options);
AudioRecord.on('data', base64Data => {
  // 需要手动解码 Base64
  const buffer = Buffer.from(base64Data, 'base64');
});
```

**迁移后：**
```typescript
import { NitroAudioRecord } from 'react-native-nitro-audio-record';

NitroAudioRecord.setup(options);
NitroAudioRecord.onData(arrayBuffer => {
  // 直接使用 arrayBuffer，无需解码！
});
```

## 许可证

MIT
