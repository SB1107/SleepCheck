package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class NoSeringResultEntity(

    @SerializedName("result")
    val result: NoSeringResult?
) : BaseEntity()
@Keep
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
    @SerializedName("deep_sleep_time")
    var deepSleepTime: Int = 0,
    @SerializedName("move_count")
    var moveCount: Int = 0
)
