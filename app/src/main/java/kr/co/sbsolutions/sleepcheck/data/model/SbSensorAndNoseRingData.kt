package kr.co.sbsolutions.sleepcheck.data.model

data class SbSensorAndNoseRingData(
    val index: Int = 0,
    var time: String,
    val capacitance: Int,
    val calcAccX: String,
    val calcAccY: String,
    val calcAccZ: String,
    val dataId: Int,
    val noseRingTime: String,
    val noseRingInferenceTime: String,
    val coughTime : String,
    val breathingTime : String
) {
    fun toArray(): Array<String> {
        return arrayOf(index.toString(), time, capacitance.toString(), calcAccX, calcAccY, calcAccZ, dataId.toString(), noseRingTime, noseRingInferenceTime, coughTime, breathingTime)
    }
}