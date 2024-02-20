package kr.co.sbsolutions.newsoomirang.domain.model

import com.google.gson.annotations.SerializedName

data class SleepCreateModel(
    @SerializedName("number")
    private  val deviceName : String?,
    @SerializedName("type")
    private  val type : String = SleepType.Breathing.ordinal.toString()
)

enum class SleepType (type : Int) {
    Breathing (0), NoSering(1)

}