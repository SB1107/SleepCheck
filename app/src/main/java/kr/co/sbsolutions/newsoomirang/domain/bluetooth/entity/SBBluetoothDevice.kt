package kr.co.sbsolutions.withsoom.domain.bluetooth.entity

sealed class SBBluetoothDevice(val type: String) {
    object SB_SOOM_SENSOR : SBBluetoothDevice(SBSoomSensor)

    object SB_SPO2_SENSOR : SBBluetoothDevice(SBSpO2Sensor)

    object SB_EEG_SENSOR : SBBluetoothDevice(SBEEGSensor)

    companion object {
        private const val SBSoomSensor = "bluetooth_sb_sensor"
        private const val SBSpO2Sensor = "bluetooth_spo2_sensor"
        private const val SBEEGSensor = "bluetooth_eeg_sensor"

        fun getDeviceFromType(type: String) : SBBluetoothDevice? {
            return when(type) {
                SBSoomSensor -> SB_SOOM_SENSOR
                SBSpO2Sensor -> SB_SPO2_SENSOR
                SBEEGSensor -> SB_EEG_SENSOR
                else -> null
            }
        }
    }

    override fun toString(): String {
        return type
    }
}