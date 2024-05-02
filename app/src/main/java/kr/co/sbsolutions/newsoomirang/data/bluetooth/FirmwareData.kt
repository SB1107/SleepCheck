package kr.co.sbsolutions.newsoomirang.data.bluetooth

import no.nordicsemi.android.dfu.DfuServiceInitiator

data class FirmwareData(val firmwareVersion: String, val deviceName: String, val deviceAddress: String)
data class FirmwareDataModel(val isShow: Boolean = false, val dfuServiceInitiator: DfuServiceInitiator? = null ,val firmwareVersion: String = "", val deviceName: String = "", val deviceAddress: String = "" , val serverFirmwareVersion : String = "1.0.0")
