package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

data class SleepResultEntity(
    @SerializedName("result")
    val result: SleepResult?
): BaseEntity()

data class SleepResult(
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("ended_at")
    val endedAt: String? = null,
    @SerializedName("state")
    val state: Int = 0,
    @SerializedName("sleep_time")
    val sleepTime: Int = 0,
    @SerializedName("asleep_time")
    val asleepTime: Int = 0,
    @SerializedName("apnea_state")
    val apneaState: Int = 0,
    @SerializedName("deep_sleep_time")
    val deepSleepTime : Int = 0,
    @SerializedName("move_count")
    val moveCount :Int =0
)