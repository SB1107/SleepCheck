package kr.co.sbsolutions.withsoom.domain.bluetooth.entity

sealed class SBBluetoothDevice(val type: Sensor) {
    object SB_BREATHING_SENSOR : SBBluetoothDevice(SBBreathingSensor)
    object SB_NO_SERING_SENSOR : SBBluetoothDevice(SBNoSeringSensor)

    object SB_SPO2_SENSOR : SBBluetoothDevice(SBSpO2Sensor)

    object SB_EEG_SENSOR : SBBluetoothDevice(SBEEGSensor)

    companion object {
        private val SBBreathingSensor = Sensor.SB_BREATHING_SENSOR
        private val SBNoSeringSensor = Sensor.SB_NO_SERING_SENSOR
        private val SBSpO2Sensor = Sensor.SB_SPO2_SENSOR
        private val SBEEGSensor = Sensor.SB_EEG_SENSOR

        fun getDeviceFromType(type: Sensor): SBBluetoothDevice {
            return when (type) {
                Sensor.SB_BREATHING_SENSOR -> SB_BREATHING_SENSOR
                Sensor.SB_NO_SERING_SENSOR -> SB_NO_SERING_SENSOR
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
    SB_BREATHING_SENSOR, SB_NO_SERING_SENSOR, SB_SPO2_SENSOR, SB_EEG_SENSOR
}