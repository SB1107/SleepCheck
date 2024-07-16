package kr.co.sbsolutions.sleepcheck.service

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons.SNORING_VIBRATION_DELAYED_START_TIME
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.NoseRingHelper
import kr.co.sbsolutions.sleepcheck.data.db.NoseRingEntity
import kr.co.sbsolutions.sleepcheck.domain.audio.AudioClassificationHelper
import kr.co.sbsolutions.sleepcheck.domain.db.NoseRingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import org.tensorflow.lite.support.label.Category
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.timerTask

class NoseRingUseCase(
    private val context: Context,
    private var lifecycleScope: LifecycleCoroutineScope,
    private val noseRingHelper: NoseRingHelper,
    private val settingDataRepository: SettingDataRepository,
    private val dataManager: DataManager,
    private val noseRingDataRepository: NoseRingDataRepository
) : INoseRingHelper{
    private val audioClassificationHelper: AudioClassificationHelper by lazy {
        AudioClassificationHelper(context, object : AudioClassificationHelper.AudioClassificationListener {
            override fun onError(error: String?) {
            }

            override fun onResult(results: List<Category?>?, inferenceTime: Long?) {
                noseRingHelper.noSeringResult(results, inferenceTime)
            }
        })
    }

    private var timerOfStartAudio: Timer? = null

    fun setCallVibrationNotifications(callback :  (intensity: Int) -> Unit) {
        noseRingHelper.setCallVibrationNotifications {
            lifecycleScope.launch(IO) {
                val onOff = settingDataRepository.getSnoringOnOff()
                if (onOff) {
                    callback.invoke(settingDataRepository.getSnoringVibrationIntensity())
                }
            }
        }
        noseRingHelper.setInferenceTimeCallback {  inferenceTime ->
            lifecycleScope.launch(IO) {
                settingDataRepository.getDataId()?.let { dataId ->
                    val nowTime = System.currentTimeMillis()
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(nowTime)
                    val data = NoseRingEntity(time, inferenceTime.toString(), dataId)
                    noseRingDataRepository.insertNoseRingData(data)
                }
            }
        }
    }

    override fun getSnoreTime(): Long {
        return (noseRingHelper.getSnoreTime() / 1000) / 60
    }

    override fun getSnoreCount(): Int {
        return noseRingHelper.getSnoreCount()
    }

    override fun getCoughCount(): Int {
        return noseRingHelper.getCoughCount()
    }

    override fun stopAudioClassification() {
        timerOfStartAudio?.cancel()
        audioClassificationHelper.stopAudioClassification()
    }

    override fun snoreCountIncrease() {
        noseRingHelper.snoreCountIncrease()
    }

    fun startAudioClassification() {
        timerOfStartAudio?.cancel()
//        timerOfStartAudio = Timer().apply {
//            schedule(timerTask {
//                audioClassificationHelper.startAudioClassification()
//            }, SNORING_VIBRATION_DELAYED_START_TIME)
//        }
        audioClassificationHelper.startAudioClassification()
    }

    fun clearData() {
        noseRingHelper.clearData()
    }

    private fun setSnoreTime() {
        lifecycleScope.launch(IO) {
            val time = dataManager.getNoseRingTimer().first()
            val noseRingCount = dataManager.getNoseRingCount().first()
            val coughCount = dataManager.getCoughCount().first()
            noseRingHelper.setSnoreTime(time)
            noseRingHelper.setSnoreCount(noseRingCount)
            noseRingHelper.setCoughCount(coughCount)
        }
    }

    fun setNoseRingDataAndStart() {
        setSnoreTime()
        startAudioClassification()
    }
}


