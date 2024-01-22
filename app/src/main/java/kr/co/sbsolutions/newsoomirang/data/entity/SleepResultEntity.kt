package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

data class SleepResultEntity(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("result")
    val result: SleepResult?
)

data class SleepResult(
    @SerializedName("started_at")
    var startedAt: String? = null,
    @SerializedName("ended_at")
    var endedAt: String? = null,
    @SerializedName("state")
    var state: Int = 0,
    @SerializedName("sleep_time")
    var sleepTime: Int = 0,
    @SerializedName("asleep_time")
    var asleepTime: Int = 0,
    @SerializedName("apnea_state")
    var apneaState: Int = 0,
)