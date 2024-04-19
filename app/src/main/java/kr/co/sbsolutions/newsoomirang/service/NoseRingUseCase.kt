package kr.co.sbsolutions.newsoomirang.service

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.domain.audio.AudioClassificationHelper
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import org.tensorflow.lite.support.label.Category

class NoseRingUseCase(
    private val context: Context,
    private var lifecycleScope: LifecycleCoroutineScope,
    private val noseRingHelper: NoseRingHelper,
    private val timeHelper: TimeHelper,
    private val settingDataRepository: SettingDataRepository,
    private val sbSensorBlueToothUseCase: SBSensorBlueToothUseCase?,
) {
    private var audioClassificationHelper: AudioClassificationHelper = AudioClassificationHelper(context, object : AudioClassificationHelper.AudioClassificationListener {
        override fun onError(error: String?) {
        }

        override fun onResult(results: List<Category?>?, inferenceTime: Long?) {
            noseRingHelper.noSeringResult(results, inferenceTime)
        }
    })


    fun setCallVibrationNotifications() {
        noseRingHelper.setCallVibrationNotifications {
            lifecycleScope.launch(IO) {
                val onOff = settingDataRepository.getSnoringOnOff()
                if (onOff) {
                    if (timeHelper.getTime() > (60 * 30)) {
                        callVibrationNotifications(settingDataRepository.getSnoringVibrationIntensity())
                    }
                }
            }
        }
    }

    private fun callVibrationNotifications(intensity: Int) {
        sbSensorBlueToothUseCase?.callVibrationNotifications(intensity)
    }

    fun getSnoreTime(): Long {
        return (noseRingHelper.getSnoreTime() / 1000) / 60
    }

    fun getSnoreCount(): Int {
        return noseRingHelper.getSnoreCount()
    }

    fun getCoughCount(): Int {
        return noseRingHelper.getCoughCount()
    }

    fun stopAudioClassification(){
        audioClassificationHelper.stopAudioClassification()
    }

    fun startAudioClassification(){
        audioClassificationHelper.startAudioClassification()
    }
    fun clearData(){
        noseRingHelper.clearData()
    }
}


