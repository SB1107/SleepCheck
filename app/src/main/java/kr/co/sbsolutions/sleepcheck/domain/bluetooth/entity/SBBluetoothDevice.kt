package kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity

sealed class SBBluetoothDevice(val type: Sensor) {
    object SB_SOOM_SENSOR : SBBluetoothDevice(SBSoomSensor)
    object SB_SPO2_SENSOR : SBBluetoothDevice(SBSpO2Sensor)

    object SB_EEG_SENSOR : SBBluetoothDevice(SBEEGSensor)

    companion object {
        private val SBSoomSensor = Sensor.SB_SOOM_SENSOR
        private val SBSpO2Sensor = Sensor.SB_SPO2_SENSOR
        private val SBEEGSensor = Sensor.SB_EEG_SENSOR

        fun getDeviceFromType(type: Sensor): SBBluetoothDevice {
            return when (type) {
                Sensor.SB_SOOM_SENSOR -> SB_SOOM_SENSOR
                Sensor.SB_SPO2_SENSOR -> SB_SPO2_SENSOR
                Sensor.SB_EEG_SENSOR -> SB_SPO2_SENSOR
            }
        }
    }

    override fun toString(): String {
        return type.name
    }
}

enum class Sensor {
    SB_SOOM_SENSOR, SB_SPO2_SENSOR, SB_EEG_SENSOR
}