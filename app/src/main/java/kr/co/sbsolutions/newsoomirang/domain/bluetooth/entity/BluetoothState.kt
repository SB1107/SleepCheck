package kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity

sealed class BluetoothState(open val state: String) {
    object Unregistered : BluetoothState("Unregistered")
    object Registered : BluetoothState("Registered")
    object Connecting : BluetoothState("Connecting")
    object DisconnectedByUser : BluetoothState("DisconnectedByUser")
    object DisconnectedNotIntent : BluetoothState("DisconnectedNotIntent")
    sealed class Connected(override val state: String) : BluetoothState(state) {
        object Init : Connected("Init")
        object Ready : Connected("Ready")
        object Reconnected : Connected("Reconnected")
        object SendStart : Connected("SendStart")
        object MotCtrlSet : Connected("MotCtrlSet")
        object WaitStart : Connected("WaitStart")
        object SendStop : Connected("SendStop")
        object SendRealtime : Connected("SendRealtime")
        object ReceivingRealtime : Connected("ReceivingRealtime")
        object DataFlow : Connected("DataFlow")
        object DataFlowUploadFinish : Connected("DataFlowUploadFinish")
        object SendDelayed : Connected("SendDelayed")
        object ReceivingDelayed : Connected("ReceivingDelayed")
        object SendDownloadContinue : Connected("SendDownloadContinue")
        object SendDownload : Connected("SendDownload")
        object FinishDownload : Connected("FinishDownload")
        object SendDelete : Connected("SendDelete")
        object Finish : Connected("Finish")
        object End : Connected("End")
        object ForceEnd : Connected("ForceEnd")
    }

    override fun toString() = state
}
