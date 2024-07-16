package kr.co.sbsolutions.sleepcheck.data.model

data class SbSensorAndNoseRingData(
    val index: Int = 0,
    var time: String,
    val capacitance: Int,
    val calcAccX: String,
    val calcAccY: String,
    val calcAccZ: String,
    val dataId: Int,
    var noseRingTime: String,
    var noseRingInferenceTime: String
) {
    fun toArray(): Array<String> {
        return arrayOf(index.toString(), time, capacitance.toString(), calcAccX, calcAccY, calcAccZ, dataId.toString(), noseRingTime, noseRingInferenceTime)
    }
}