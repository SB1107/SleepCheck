package kr.co.sbsolutions.newsoomirang.domain.audio

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import org.tensorflow.lite.task.core.BaseOptions
import java.io.IOException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AudioClassificationHelper(var context: Context, var listener: AudioClassificationListener) {
    interface AudioClassificationListener {
        fun onError(error: String?)
        fun onResult(results: List<Category?>?, inferenceTime: Long?)
    }

    var currentModel = AUDIO.YAMNET_MODEL
    var classificationThreshold = AUDIO.DISPLAY_THRESHOLD
    var overlap = AUDIO.DEFAULT_OVERLAP_VALUE
    var numOfResults = AUDIO.DEFAULT_NUM_OF_RESULTS
    var currentDelegate = 0
    var numThreads = 2
    lateinit var classifier: AudioClassifier
    private var tensorAudio: TensorAudio? = null
    private var recorder: AudioRecord? = null
    private var executor: ScheduledThreadPoolExecutor? = null
    private val classifyRunnable = Runnable { classifyAudio() }

    init {
        initClassifier()
    }

    fun initClassifier() {
        val baseOptionsBuilder: BaseOptions.Builder = BaseOptions.builder().setNumThreads(numThreads)
        if (currentDelegate == AUDIO.DELEGATE_CPU) {
        } else if (currentDelegate == AUDIO.DELEGATE_NNAPI) {
            baseOptionsBuilder.useNnapi()
        }
        val options: AudioClassifier.AudioClassifierOptions = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()
        try {
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            startAudioClassification()
        } catch (e: IOException) {
            listener.onError(e.localizedMessage)
        }
    }

    fun startAudioClassification() {
        if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        recorder?.startRecording()
        executor = ScheduledThreadPoolExecutor(1)
        val lengthInMilliSeconds = (classifier.requiredInputBufferSize * 1.0f / classifier.getRequiredTensorAudioFormat().getSampleRate() * 1000).toLong()
        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()
        executor!!.scheduleAtFixedRate(classifyRunnable, 0, interval, TimeUnit.MILLISECONDS)
    }

    private fun classifyAudio() {
        tensorAudio?.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val output: List<Classifications> = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        listener.onResult(output[0].categories, inferenceTime)
    }

    fun stopAudioClassification() {
        recorder?.stop()
        executor?.shutdownNow()
    }

    object AUDIO {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val YAMNET_MODEL = "yamnet.tflite"
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
    }
}
