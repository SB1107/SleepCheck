package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

data class NoSeringResultEntity(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("result")
    val result: NoSeringResult?
)

data class NoSeringResult(
    @SerializedName("started_at")
    var startedAt: String? = null,
    @SerializedName("ended_at")
    var endedAt: String? = null,
    @SerializedName("state")
    var state: Int = 0,
    @SerializedName("snore_time")
    var noSeringTime: Int = 0,
    @SerializedName("asleep_time")
    var asleepTime: Int = 0,
    @SerializedName("apnea_state")
    var apneaState: Int = 0,

)
