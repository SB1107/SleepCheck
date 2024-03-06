package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class SleepDetailEntity (
    @SerializedName("result")
    val result: SleepDetailResult?
): BaseEntity()
@Keep
data class SleepDetailResult(
    @SerializedName("id")
    var id: Int? = 0,
    @SerializedName("user_id")
    var userId: Int? = 0,
    @SerializedName("number")
    var number: String? = null,
    @SerializedName("dirname")
    var dirName: String? = null,
    @SerializedName("filename")
    var fileName: String? = null,
    @SerializedName("asleep_time")
    var asleepTime: Int? = 0,
    @SerializedName("type")
    var type: Int = 0,
    @SerializedName("snore_time")
    var snoreTime: Int? = 0,
    @SerializedName("apnea_state")
    var apneaState: Int? = null,
    @SerializedName("apnea_count")
    var apneaCount: Int? = 0,
    @SerializedName("apnea_10")
    var apnea10: Int? = 0,
    @SerializedName("apnea_30")
    var apnea30: Int? = 0,
    @SerializedName("apnea_60")
    var apnea60: Int? = 0,
    @SerializedName("straight_position")
    var straightPositionTime: Int? = 0,
    @SerializedName("left_position")
    var leftPositionTime: Int? = 0,
    @SerializedName("right_position")
    var rightPositionTime: Int? = 0,
    @SerializedName("down_position")
    var downPositionTime: Int? = 0,
    @SerializedName("wake_time")
    var wakeTime: Int? = 0,
    @SerializedName("straight_per")
    var straightPer: Int? = 0,
    @SerializedName("left_per")
    var leftPer: Int? = 0,
    @SerializedName("right_per")
    var rightPer: Int? = 0,
    @SerializedName("down_per")
    var downPer: Int? = 0,
    @SerializedName("wake_per")
    var wakePer: Int? = 0,
    @SerializedName("sleep_pattern")
    var sleepPattern: String? = null,
    @SerializedName("started_at")
    var startedAt: String? = null,
    @SerializedName("ended_at")
    var endedAt: String? = null,
    @SerializedName("sleep_time")
    var sleepTime: Int? = 0,
    @SerializedName("state")
    var state: Int? = null,
    @SerializedName("deep_sleep_time")
    var deepSleepTime: Int? = 0,
    @SerializedName("move_count")
    var moveCount: Int? = 0
)