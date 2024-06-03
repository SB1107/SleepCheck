package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class SleepCreateModel(
    @SerializedName("number")
    private val deviceName: String?,
    @SerializedName("type")
    private val type: String = SleepType.Breathing.ordinal.toString(),
    @SerializedName("app_kind")
    private val appKind: String = "C"
)

enum class SleepType(type: Int) {
    Breathing(0), NoSering(1)

}