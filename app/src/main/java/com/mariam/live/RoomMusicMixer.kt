package com.mariam.live

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import io.livekit.android.audio.MixerAudioBufferCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Decodes a selected audio file to PCM and mixes it into the room microphone track.
 * Music is therefore published to every participant through the existing LiveKit track.
 */
class RoomMusicMixer(
    context: Context,
    private val uri: Uri,
    private val onError: (String) -> Unit,
    private val onEnded: () -> Unit
) : MixerAudioBufferCallback() {

    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    private val decoderFinished = AtomicBoolean(false)
    private val chunks = ArrayBlockingQueue<ShortArray>(48)
    @Volatile var paused: Boolean = false
    @Volatile var microphoneEnabled: Boolean = true
    @Volatile var volume: Float = 0.22f
    private var decoderThread: Thread? = null
    private var currentChunk: ShortArray? = null
    private var currentIndex = 0
    private var reusableMixBuffer: ByteBuffer? = null
    @Volatile private var callbackCount = 0L
    @Volatile private var decodedBytes = 0L
    @Volatile private var queuedChunks = 0L
    @Volatile private var consumedSamples = 0L
    @Volatile private var underrunCount = 0L
    @Volatile private var lastAudioFormat = -1
    @Volatile private var lastSampleRate = 0
    @Volatile private var lastChannelCount = 0
    @Volatile private var lastBytesRead = 0

    fun diagnostics(): String =
        "استدعاءات مسار الصوت: $callbackCount\\n" +
        "بيانات مفكوكة: $decodedBytes بايت\\n" +
        "مقاطع أضيفت للطابور: $queuedChunks\\n" +
        "عينات موسيقى أُرسلت: $consumedSamples\\n" +
        "مرات فراغ الطابور: $underrunCount\\n" +
        "صيغة الصوت: $lastAudioFormat | التردد: $lastSampleRate | القنوات: $lastChannelCount | bytesRead: $lastBytesRead\\n" +
        "المقاطع المتبقية: ${chunks.size} | يعمل: ${running.get()} | متوقف مؤقتاً: $paused"

    fun start() {
        if (!running.compareAndSet(false, true)) return
        decoderThread = Thread({ decodeFile() }, "MariamRoomMusicDecoder").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        decoderFinished.set(true)
        decoderThread?.interrupt()
        decoderThread = null
        chunks.clear()
        currentChunk = null
        currentIndex = 0
    }

    override fun onBufferRequest(
        originalBuffer: ByteBuffer,
        audioFormat: Int,
        channelCount: Int,
        sampleRate: Int,
        bytesRead: Int,
        captureTimeNs: Long
    ): BufferResponse? {
        callbackCount++
        lastAudioFormat = audioFormat
        lastSampleRate = sampleRate
        lastChannelCount = channelCount
        lastBytesRead = bytesRead
        if (!microphoneEnabled) {
            for (i in 0 until originalBuffer.capacity()) originalBuffer.put(i, 0)
        }

        if (paused || !running.get() || channelCount <= 0 ||
            (audioFormat != AudioFormat.ENCODING_PCM_16BIT &&
             audioFormat != AudioFormat.ENCODING_DEFAULT &&
             audioFormat != AudioFormat.ENCODING_PCM_FLOAT)
        ) return null

        // LiveKit can provide either PCM16 or PCM_FLOAT depending on its audio device.
        // The mixer must return samples in the exact same format or the SDK will interpret
        // the music bytes incorrectly (which can result in silence).
        val bytesPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val byteCount = originalBuffer.capacity() - (originalBuffer.capacity() % bytesPerSample)
        if (byteCount <= 0) return null

        var output = reusableMixBuffer
        if (output == null || output.capacity() != byteCount) {
            output = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
            reusableMixBuffer = output
        }
        output.clear()
        val frames = (byteCount / bytesPerSample) / channelCount
        if (audioFormat == AudioFormat.ENCODING_PCM_FLOAT) {
            val samples = output.asFloatBuffer()
            for (frame in 0 until frames) {
                val sample = (nextMusicSample() / 32768f * volume.coerceIn(0f, 1f))
                    .coerceIn(-1f, 1f)
                repeat(channelCount) { samples.put(sample) }
            }
        } else {
            val samples = output.asShortBuffer()
            for (frame in 0 until frames) {
                val sample = (nextMusicSample() * volume.coerceIn(0f, 1f))
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                repeat(channelCount) { samples.put(sample) }
            }
        }
        output.position(0)
        output.limit(byteCount)
        return BufferResponse(output)
    }

    private fun nextMusicSample(): Int {
        while (currentChunk == null || currentIndex >= (currentChunk?.size ?: 0)) {
            currentChunk = chunks.poll()
            currentIndex = 0
            if (currentChunk == null) {
                underrunCount++
                if (decoderFinished.get() && running.compareAndSet(true, false)) onEnded()
                return 0
            }
        }
        consumedSamples++
        return currentChunk!![currentIndex++].toInt()
    }

    private fun decodeFile() {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(appContext, uri, null)

            var selectedFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(index)
                    selectedFormat = format
                    break
                }
            }
            val sourceFormat = selectedFormat ?: throw IllegalArgumentException("الملف لا يحتوي على مسار صوتي")
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("صيغة الصوت غير مدعومة")
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(sourceFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var outputFormat = sourceFormat

            while (running.get() && !outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex)
                        if (input != null) {
                            input.clear()
                            val size = extractor.readSampleData(input, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEnded = true
                            } else {
                                val presentationTime = extractor.sampleTime
                                codec.queueInputBuffer(inputIndex, 0, size, presentationTime, 0)
                                extractor.advance()
                            }
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        outputFormat = codec.outputFormat
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val output = codec.getOutputBuffer(outputIndex)
                        if (output != null && info.size > 0) {
                            decodedBytes += info.size
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            val data = ByteArray(info.size)
                            output.get(data)
                            val sampleRate = outputFormat.getIntegerOrDefault(
                                MediaFormat.KEY_SAMPLE_RATE,
                                sourceFormat.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, 48000)
                            )
                            val channels = outputFormat.getIntegerOrDefault(
                                MediaFormat.KEY_CHANNEL_COUNT,
                                sourceFormat.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2)
                            )
                            val pcmEncoding = if (android.os.Build.VERSION.SDK_INT >= 24) {
                                outputFormat.getIntegerOrDefault(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                            } else AudioFormat.ENCODING_PCM_16BIT
                            enqueueResampledMono(data, pcmEncoding, channels, sampleRate)
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (eos) outputEnded = true
                    }
                }
            }
        } catch (error: Exception) {
            if (running.get()) {
                running.set(false)
                onError(error.message ?: "تعذر قراءة ملف الموسيقى")
            }
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            decoderFinished.set(true)
        }
    }

    private fun enqueueResampledMono(
        bytes: ByteArray,
        encoding: Int,
        channels: Int,
        sourceRate: Int
    ) {
        if (channels <= 0 || sourceRate <= 0) return
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val frameCount = bytes.size / (bytesPerSample * channels)
        if (frameCount <= 0) return

        val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val mono = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            var sum = 0f
            repeat(channels) {
                sum += if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                    input.float
                } else {
                    input.short / 32768f
                }
            }
            mono[frame] = sum / channels
        }

        val outputCount = max(1, (frameCount.toDouble() * 48000.0 / sourceRate).toInt())
        val converted = ShortArray(outputCount)
        for (i in 0 until outputCount) {
            val position = i.toDouble() * sourceRate / 48000.0
            val left = min(floor(position).toInt(), frameCount - 1)
            val right = min(left + 1, frameCount - 1)
            val fraction = (position - left).toFloat()
            val value = mono[left] * (1f - fraction) + mono[right] * fraction
            converted[i] = (value.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }

        var offset = 0
        while (offset < converted.size && running.get()) {
            val end = min(offset + 960, converted.size)
            val piece = converted.copyOfRange(offset, end)
            if (!chunks.offer(piece, 100, TimeUnit.MILLISECONDS)) continue
            queuedChunks++
            offset = end
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, fallback: Int): Int =
        try { getInteger(key) } catch (_: Exception) { fallback }
}
