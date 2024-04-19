package kr.co.sbsolutions.newsoomirang.service

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataFlowLogHelper
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.text.SimpleDateFormat
import java.util.Locale

class SBSensorUseCase(
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val settingDataRepository: SettingDataRepository,
    private val sbSensorBlueToothUseCase: SBSensorBlueToothUseCase?,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val dataFlowLogHelper: DataFlowLogHelper
) {

    private var lastIndexCk: Boolean = false

    fun listenChannelMessage() {
        var indexCountCheck = 0
        var firstData: SBSensorData? = null
        var lastData: SBSensorData? = null

        lifecycleScope.launch(IO) {
            val getIndex = async {
                settingDataRepository.getDataId()?.let {
                    firstData = sbSensorDBRepository.getSensorDataIdByFirst(it).first()
                    lastData = sbSensorDBRepository.getSensorDataIdByLast(it).first()
                }
            }
            getIndex.await()

            launch {
                sbSensorBlueToothUseCase?.getSbSensorChannel()?.consumeEach { data ->
                    val item = sbSensorDBRepository.getSensorDataByIndex(data.index)
                    item?.let { item ->
                        if (item.calcAccX == data.calcAccX &&
                            item.calcAccY == data.calcAccY &&
                            item.calcAccZ == data.calcAccZ &&
                            item.capacitance == data.capacitance
                        ) {
                            indexCountCheck += 1
                        }
                    } ?: run {
                        when {
                            indexCountCheck >= 2 && data.dataId == -1 -> {
                                sbSensorBlueToothUseCase.setLastIndexCkDone()
                                setDataFlowDBInsert(firstData, data)
                                dataFlowLogHelper.countCase1()
                            }

                            data.dataId == -1 && data.index - 1 == (lastData?.index ?: 0) -> {
                                dataFlowLogHelper.countCase2()
                                setDataFlowDBInsert(firstData, data)
                                lastIndexCk = true
                                sbSensorBlueToothUseCase.setLastIndexCkDone()
                            }

                            lastIndexCk -> {
                                sbSensorBlueToothUseCase.setLastIndexCkDone()
                                dataFlowLogHelper.countCase3()
                                setDataFlowDBInsert(firstData, data)
                            }

                            data.dataId != -1 && data.time.contains("1970") -> {
                                dataFlowLogHelper.countCase4()
                                setDataFlowDBInsert(firstData, data, true)
                            }

                            else -> {
                                sbSensorDBRepository.insert(data)
                                if (sbSensorBlueToothUseCase.isDataFlowState())
                                    dataFlowLogHelper.countCase5()
                            }
                        }
                    }
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
//            Log.e(TAG, "setDataFlowDBInsert:data = ${item}", )

        }
    }
}