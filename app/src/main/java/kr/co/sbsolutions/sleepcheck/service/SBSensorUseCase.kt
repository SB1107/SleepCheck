package kr.co.sbsolutions.sleepcheck.service

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.text.SimpleDateFormat
import java.util.Locale

class SBSensorUseCase(
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val settingDataRepository: SettingDataRepository,
    private val sbSensorBlueToothUseCase: SBSensorBlueToothUseCase?,
    private val lifecycleScope: LifecycleCoroutineScope,
) {

    fun listenChannelMessage() {
        lifecycleScope.launch(IO) {
            launch {
                sbSensorBlueToothUseCase?.getSbSensorChannel()?.collectLatest { data ->
                    sbSensorDBRepository.insert(data)
                }
            }
        }
    }

    private suspend fun setDataFlowDBInsert(
        firstData: SBSensorData?,
        data: SBSensorData,
        isUpdate: Boolean = false
    ) {
        settingDataRepository.getDataId()?.let { dataId ->

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val newTime = firstData?.time?.let { format.parse(it) }
            val time = format.format((newTime?.time ?: 0) + (200 * data.index))
//                Log.d(TAG, "listenChannelMessage111: $format")
//                Log.d(TAG, "listenChannelMessage111: $newTime")
//                Log.d(TAG, "listenChannelMessage111: $time1")
            val item = data.copy(dataId = dataId, time = time)
            if (isUpdate) {
                sbSensorDBRepository.updateSleepData(item)
                return@let
            }
            sbSensorDBRepository.insert(item)
        }
    }
}