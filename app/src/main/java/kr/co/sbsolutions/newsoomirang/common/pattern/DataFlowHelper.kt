package kr.co.sbsolutions.newsoomirang.common.pattern

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.text.SimpleDateFormat
import java.util.Locale

class DataFlowHelper(
    isUpload: Boolean,
    private val logHelper: LogHelper,
    private val coroutineScope: CoroutineScope,
    settingDataRepository: SettingDataRepository,
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val bluetoothNetworkRepository : IBluetoothNetworkRepository,
    private val callback: (ChainData) -> Unit
) {
    private val dataIdProcessor = DataIdProcessor(settingDataRepository)
    private val itemCheckProcessor = ItemCheckProcessor(bluetoothNetworkRepository, sbSensorDBRepository)
    private val noDataIdItemInsertProcessor = NoDataIdItemInsertProcessor(sbSensorDBRepository)
    private val noDataItemCheckProcessor = NoDataItemCheckProcessor(bluetoothNetworkRepository.sbSensorInfo.value, sbSensorDBRepository)

    init {
        when {
            isUpload -> uploadProcess()
            else -> cancelProcess()
        }
    }

    private fun uploadProcess() {
        logHelper.insertLog("uploadProcess()")
        dataIdProcessor.setNext(itemCheckProcessor)
        itemCheckProcessor.setNext(noDataIdItemInsertProcessor)
        dataIdProcessor.process(logHelper = logHelper, scope = coroutineScope, ChainData(), callback = callback)
    }

    private fun cancelProcess() {
        logHelper.insertLog("cancelProcess()")
        dataIdProcessor.setNext(itemCheckProcessor)
        itemCheckProcessor.setNext(noDataItemCheckProcessor)
        dataIdProcessor.process(logHelper = logHelper, scope = coroutineScope, ChainData(),  callback = callback)
    }
}

interface Chain {
    fun setNext(nextInChain: Chain)
    fun process(logHelper: LogHelper, scope: CoroutineScope, chainData: ChainData, callback: (ChainData) -> Unit)

}

data class ChainData(
    var dataId: Int? = null, var bluetoothInfo: BluetoothInfo? = null , var isSuccess: Boolean = true , var reasonMessage :String = ""
)

class DataIdProcessor(private val settingDataRepository: SettingDataRepository) : Chain {
    private lateinit var nextInChain: Chain

    override fun setNext(nextInChain: Chain) {
        this.nextInChain = nextInChain
    }

    override fun process(logHelper: LogHelper, scope: CoroutineScope, chainData: ChainData, callback: (ChainData) -> Unit) {
        scope.launch {
            chainData.dataId = settingDataRepository.getDataId()
            nextInChain.process(logHelper, scope, chainData, callback)
        }
    }
}

class ItemCheckProcessor(private val networkRepository: IBluetoothNetworkRepository, private val sbSensorDBRepository: SBSensorDBRepository) : Chain {
    private lateinit var nextInChain: Chain
    override fun setNext(nextInChain: Chain) {
        this.nextInChain = nextInChain
    }

    override fun process(logHelper: LogHelper, scope: CoroutineScope, chainData: ChainData, callback: (ChainData) -> Unit) {
        chainData.bluetoothInfo = networkRepository.sbSensorInfo.value

        chainData.dataId?.let {
            scope.launch {
                val size = sbSensorDBRepository.getSelectedSensorDataListCount(it).first()
                Log.e(TAG, "totalCount = ${networkRepository.sbSensorInfo.value.isDataFlow.value.totalCount}"+ " list = ${size}" )
                networkRepository.setDataFlow(true , 0 ,networkRepository.getDataFlowMaxCount().plus(size))
                if (isItemPass(it , networkRepository)) {
                    if (::nextInChain.isInitialized) {
                        nextInChain.process(logHelper, scope, chainData, callback)
                    }
                }
            }
        } ?: run {
            logHelper.insertLog("DataId 가없음")
            chainData.isSuccess = false
            chainData.reasonMessage ="DataId 가없음"
            callback.invoke(chainData)
        }
    }

    private suspend fun isItemPass(dataId: Int,  networkRepository: IBluetoothNetworkRepository): Boolean {
        var size = 0
        val reCount = 3
        var tempCont = 0

        while (tempCont != reCount) {
            delay(200)
            val itemSize = sbSensorDBRepository.getSelectedSensorDataListCount(dataId).first()
            if (size != itemSize) {
                size = itemSize
                Log.e(TAG, "size: ${size}")
                tempCont = 0
                networkRepository.setDataFlow(true, size , networkRepository.getDataFlowMaxCount())
            } else {
                tempCont += 1
            }
        }
        Log.e(TAG, "isItemPass: 와일종료")
        return true
    }
}

class NoDataItemCheckProcessor(private val info: BluetoothInfo, private val sbSensorDBRepository: SBSensorDBRepository) : Chain {
    private lateinit var nextInChain: Chain
    override fun setNext(nextInChain: Chain) {
        this.nextInChain = nextInChain
    }

    override fun process(logHelper: LogHelper, scope: CoroutineScope, chainData: ChainData, callback: (ChainData) -> Unit) {
        chainData.bluetoothInfo = info
        chainData.dataId?.let {
            scope.launch {
                if (noDataItemPass()) {
                    chainData.isSuccess = false
                    chainData.reasonMessage = "데이터 확인 필요"
                    callback.invoke(chainData)
//                    nextInChain.process(logHelper, scope, chainData, callback)
                }
            }
        } ?: run {
            logHelper.insertLog("DataId 가없음")
        }
    }

    private suspend fun noDataItemPass(): Boolean {
        var size = 0
        val reCount = 3
        var tempCont = 0

        while (tempCont != reCount) {
            delay(200)
            val itemSize = sbSensorDBRepository.getSensorDataIdBy(-1).first().size
            if (size != itemSize) {
                size = itemSize
                Log.e(TAG, "size: ${size}")
                tempCont = 0
            } else {
                tempCont += 1
            }
        }
        Log.e(TAG, "isItemPass: 와일종료")
        return true
    }
}

class NoDataIdItemInsertProcessor(private val sbSensorDBRepository: SBSensorDBRepository) : Chain {
    private lateinit var nextInChain: Chain
    override fun setNext(nextInChain: Chain) {
        this.nextInChain = nextInChain
    }

    override fun process(logHelper: LogHelper, scope: CoroutineScope, chainData: ChainData, callback: (ChainData) -> Unit) {
        scope.launch {
            chainData.dataId?.let { id ->
                sbSensorDBRepository.getSensorDataIdByFirst(id).first()?.let { noDataIdItemInsert(it, id) }
                chainData.isSuccess = true
                callback.invoke(chainData)
            } ?: run {
                logHelper.insertLog("DataId 가없음")
            }
        }
    }

    private suspend fun noDataIdItemInsert(firstData: SBSensorData, id: Int): Boolean {
        val reCount = 3
        var tempCont = 0

        while (tempCont != reCount) {
            delay(200)
            val itemSize = sbSensorDBRepository.getSensorDataIdBy(-1).first()
            if (itemSize.isNotEmpty()) {
                delay(200)

                Log.e(TAG, "itemindex: ${itemSize.size}")
                itemSize.map {
                    setDataFlowDBInsert(firstData, it, true, id)
                }
            } else {
                tempCont += 1
            }
        }
        Log.e(TAG, "isItemPass: -1 업데이트 와일종료")
        return true

    }

    private suspend fun setDataFlowDBInsert(
        firstData: SBSensorData?,
        data: SBSensorData,
        isUpdate: Boolean = false,
        id: Int
    ) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val newTime = firstData?.time?.let { format.parse(it) }
        val time1 = format.format((newTime?.time ?: 0) + (200 * data.index))
        val item = data.copy(dataId = id, time = time1)
        if (isUpdate) {
            sbSensorDBRepository.updateSleepData(item)
            return
        }
        sbSensorDBRepository.insert(item)
    }

}
