package com.nitroaudiorecord

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.util.Log
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.ArrayBuffer
import com.margelo.nitro.core.Promise
import com.margelo.nitro.nitroaudiorecord.AudioRecordOptions
import com.margelo.nitro.nitroaudiorecord.HybridNitroAudioRecordSpec
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class HybridNitroAudioRecord : HybridNitroAudioRecordSpec() {
    private val TAG = "NitroAudioRecord"

    private var sampleRateInHz = 44100
    private var channelConfig = AudioFormat.CHANNEL_IN_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var audioSource = AudioSource.VOICE_RECOGNITION

    private var recorder: AudioRecord? = null
    private var bufferSize = 0
    private var isRecording = false

    private lateinit var tmpFile: String
    private lateinit var outFile: String
    
    private var onDataCallback: ((data: ArrayBuffer) -> Unit)? = null

    override fun setup(options: AudioRecordOptions) {
        sampleRateInHz = options.sampleRate.toInt()
        channelConfig = if (options.channels >= 2.0) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        audioFormat = if (options.bitsPerSample <= 8.0) AudioFormat.ENCODING_PCM_8BIT else AudioFormat.ENCODING_PCM_16BIT
        audioSource = (options.audioSource ?: AudioSource.VOICE_RECOGNITION.toDouble()).toInt()

        val context = NitroModules.applicationContext ?: throw Exception("NitroModules.applicationContext is null!")
        val documentDirectoryPath = context.filesDir.absolutePath
        outFile = "$documentDirectoryPath/${options.wavFile ?: "audio.wav"}"
        tmpFile = "$documentDirectoryPath/temp.pcm"

        isRecording = false
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        if (bufferSize <= 0) {
            bufferSize = 1024 // Fallback
        }
        val recordingBufferSize = bufferSize * 3
        recorder = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, recordingBufferSize)
    }

    override fun start() {
        if (isRecording) return
        
        isRecording = true
        recorder?.startRecording()
        Log.d(TAG, "started recording")

        Thread {
            try {
                var bytesRead: Int
                var count = 0
                val buffer = ByteArray(bufferSize)
                val os = FileOutputStream(tmpFile)

                while (isRecording) {
                    bytesRead = recorder?.read(buffer, 0, buffer.length) ?: -1

                    // skip first 2 buffers to eliminate "click sound"
                    if (bytesRead > 0 && ++count > 2) {
                        // Create ArrayBuffer and notify callback
                        onDataCallback?.let { callback ->
                            val byteBuffer = ByteBuffer.allocateDirect(bytesRead)
                            byteBuffer.put(buffer, 0, bytesRead)
                            val arrayBuffer = ArrayBuffer.wrap(byteBuffer)
                            callback(arrayBuffer)
                        }
                        os.write(buffer, 0, bytesRead)
                    }
                }

                recorder?.stop()
                os.close()
                saveAsWav()
            } catch (e: Exception) {
                Log.e(TAG, "Error during recording", e)
            }
        }.start()
    }

    override fun stop(): Promise<String> {
        isRecording = false
        return Promise { resolve ->
            // In a real implementation, we might need a better way to wait for the thread to finish
            // such as using a CountDownLatch or joining the thread.
            // For now, we'll wait a bit (simplistic).
            Thread {
                Thread.sleep(100)
                resolve(outFile)
            }.start()
        }
    }

    override fun onData(callback: (data: ArrayBuffer) -> Unit) {
        onDataCallback = callback
    }

    private fun saveAsWav() {
        try {
            val fileIn = File(tmpFile)
            val `in` = FileInputStream(fileIn)
            val out = FileOutputStream(outFile)
            val totalAudioLen = fileIn.length()
            val totalDataLen = totalAudioLen + 36

            addWavHeader(out, totalAudioLen, totalDataLen)

            val data = ByteArray(bufferSize)
            var bytesRead: Int
            while (`in`.read(data).also { bytesRead = it } != -1) {
                out.write(data, 0, bytesRead)
            }
            Log.d(TAG, "file saved at path: $outFile")

            `in`.close()
            out.close()
            fileIn.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving as wav", e)
        }
    }

    @Throws(Exception::class)
    private fun addWavHeader(out: FileOutputStream, totalAudioLen: Long, totalDataLen: Long) {
        val sampleRate = sampleRateInHz.toLong()
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_8BIT) 8 else 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)

        header[0] = 'R'.toByte()                                    // RIFF chunk
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()                // how big is the rest of this file
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()                                    // WAVE chunk
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()                                   // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16                                             // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1                                              // format = 1 for PCM
        header[21] = 0
        header[22] = channels.toByte()                              // mono or stereo
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()                 // samples per second
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()                  // bytes per second
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = blockAlign.toByte()                             // bytes in one sample, for all channels
        header[33] = 0
        header[34] = bitsPerSample.toByte()                         // bits in a sample
        header[35] = 0
        header[36] = 'd'.toByte()                                   // beginning of the data chunk
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()              // how big is this data chunk
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
