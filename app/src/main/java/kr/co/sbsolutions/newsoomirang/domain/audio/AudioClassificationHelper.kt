package kr.co.sbsolutions.newsoomirang.domain.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import org.tensorflow.lite.task.core.BaseOptions
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AudioClassificationHelper(var context: Context, var listener: AudioClassificationListener) {
    interface AudioClassificationListener {
        fun onError(error: String?)
        fun onResult(results: List<Category?>?, inferenceTime: Long?)
    }

    enum class Model(val fileName: String) {
        YAMNET("yamnet.tflite"),
        SPEECH_COMMAND("speech.tflite")
    }

    companion object {
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.2f
        const val NUM_THREADS = 2
    }

    private var currentModel = Model.YAMNET
    private var classificationThreshold = DISPLAY_THRESHOLD
    private var numOfResults = DEFAULT_NUM_OF_RESULTS
    private var overlap = DEFAULT_OVERLAP_VALUE
    private lateinit var classifier: AudioClassifier
    private var tensorAudio: TensorAudio? = null
    private var recorder: AudioRecord? = null
    private val classifyRunnable = Runnable { classifyAudio() }
    private var executor = Executors.newSingleThreadScheduledExecutor() // Single thread for efficient classification
    private var lengthInMilliSeconds: Long = 0 // Cached value
    private var interval: Long = 0 // Cached value

    init {
        initClassifier()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun initClassifier() {
        val baseOptions = BaseOptions.builder().setNumThreads(NUM_THREADS).build()
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptions)
            .build()
        try {
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel.fileName, options)
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            lengthInMilliSeconds = (classifier.requiredInputBufferSize * 1.0f / classifier.getRequiredTensorAudioFormat().getSampleRate() * 1000).toLong()
            interval = (lengthInMilliSeconds * (1 - overlap)).toLong()
        } catch (e: IOException) {
            listener.onError(e.localizedMessage)
        }
    }

    fun startAudioClassification() {
        if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        recorder?.startRecording()
        executor.scheduleAtFixedRate(classifyRunnable, 0, interval, TimeUnit.MILLISECONDS)
    }

    private fun classifyAudio() {
        tensorAudio?.load(recorder)
        val startTime = SystemClock.uptimeMillis()
        val output: List<Classifications> = classifier.classify(tensorAudio)
        val inferenceTime = SystemClock.uptimeMillis() - startTime

        listener.onResult(output[0].categories, inferenceTime)
    }

    fun stopAudioClassification() {
        recorder?.stop()
        executor.shutdown()
    }
}
