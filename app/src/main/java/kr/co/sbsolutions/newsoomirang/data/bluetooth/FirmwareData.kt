package kr.co.sbsolutions.newsoomirang.data.bluetooth

data class FirmwareData(val firmwareVersion: String , val deviceName : String , val deviceAddress: String)
data class FirmwareDataModel(val isShow: Boolean = false,val firmwareFileName : String ,val firmwareVersion: String , val deviceName : String , val deviceAddress: String)
