import Foundation
import NitroModules
import AVFoundation
import AudioToolbox

struct AQRecordState {
    var mDataFormat = AudioStreamBasicDescription()
    var mQueue: AudioQueueRef?
    var mBuffers = [AudioQueueBufferRef?](repeating: nil, count: 3)
    var mAudioFile: AudioFileID?
    var bufferByteSize: UInt32 = 0
    var mCurrentPacket: Int64 = 0
    var mIsRunning = false
}

class HybridNitroAudioRecord: HybridNitroAudioRecordSpec {
    private var recordState = AQRecordState()
    private var filePath: String = ""
    private var onDataCallback: ((ArrayBuffer) -> Void)?
    
    func setup(options: AudioRecordOptions) throws {
        let sampleRate = options.sampleRate
        let bitsPerSample = UInt32(options.bitsPerSample)
        let channels = UInt32(options.channels)
        
        recordState.mDataFormat.mSampleRate = sampleRate
        recordState.mDataFormat.mBitsPerChannel = bitsPerSample
        recordState.mDataFormat.mChannelsPerFrame = channels
        recordState.mDataFormat.mBytesPerPacket = (bitsPerSample / 8) * channels
        recordState.mDataFormat.mBytesPerFrame = recordState.mDataFormat.mBytesPerPacket
        recordState.mDataFormat.mFramesPerPacket = 1
        recordState.mDataFormat.mReserved = 0
        recordState.mDataFormat.mFormatID = kAudioFormatLinearPCM
        recordState.mDataFormat.mFormatFlags = bitsPerSample == 8 ? kLinearPCMFormatFlagIsPacked : (kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked)
        
        recordState.bufferByteSize = 2048
        
        let fileName = options.wavFile ?? "audio.wav"
        let docDir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
        filePath = "\(docDir)/\(fileName)"
    }
    
    func start() throws {
        // Set AudioSession
        try AVAudioSession.sharedInstance().setCategory(.record)
        try AVAudioSession.sharedInstance().setActive(true)
        
        recordState.mIsRunning = true
        recordState.mCurrentPacket = 0
        
        let url = URL(fileURLWithPath: filePath)
        let status = AudioFileCreateWithURL(url as CFURL, kAudioFileWAVEType, &recordState.mDataFormat, .eraseFile, &recordState.mAudioFile)
        if status != noErr {
            throw NSError(domain: "NitroAudioRecord", code: Int(status), userInfo: [NSLocalizedDescriptionKey: "Failed to create audio file"])
        }
        
        let selfPointer = Unmanaged.passUnretained(self).toOpaque()
        
        AudioQueueNewInput(&recordState.mDataFormat, { (inUserData, inAQ, inBuffer, inStartTime, inNumPackets, inPacketDesc) in
            let recorder = Unmanaged<HybridNitroAudioRecord>.fromOpaque(inUserData!).takeUnretainedValue()
            
            if !recorder.recordState.mIsRunning { return }
            
            var numPackets = inNumPackets
            if AudioFileWritePackets(recorder.recordState.mAudioFile!, false, inBuffer.pointee.mAudioDataByteSize, inPacketDesc, recorder.recordState.mCurrentPacket, &numPackets, inBuffer.pointee.mAudioData) == noErr {
                recorder.recordState.mCurrentPacket += Int64(numPackets)
            }
            
            // Notify callback
            if let callback = recorder.onDataCallback {
                let data = Data(bytes: inBuffer.pointee.mAudioData, count: Int(inBuffer.pointee.mAudioDataByteSize))
                // Create ArrayBuffer from Data using the helper extension
                do {
                    let arrayBuffer = try ArrayBuffer.copy(data: data)
                    callback(arrayBuffer)
                } catch {
                    print("Failed to notify audio data: \(error)")
                }
            }
            
            AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, nil)
        }, selfPointer, nil, nil, 0, &recordState.mQueue)
        
        for i in 0..<3 {
            AudioQueueAllocateBuffer(recordState.mQueue!, recordState.bufferByteSize, &recordState.mBuffers[i])
            AudioQueueEnqueueBuffer(recordState.mQueue!, recordState.mBuffers[i]!, 0, nil)
        }
        
        AudioQueueStart(recordState.mQueue!, nil)
    }
    
    func stop() throws -> Promise<String> {
        if recordState.mIsRunning {
            recordState.mIsRunning = false
            AudioQueueStop(recordState.mQueue!, true)
            AudioQueueDispose(recordState.mQueue!, true)
            AudioFileClose(recordState.mAudioFile!)
        }
        
        let promise = Promise<String>()
        promise.resolve(withResult: filePath)
        return promise
    }
    
    func onData(callback: @escaping (ArrayBuffer) -> Void) throws {
        onDataCallback = callback
    }
}
