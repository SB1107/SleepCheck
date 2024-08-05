package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SleepDetailEntity(
    @SerializedName("result")
    val result: SleepDetailResult?
) : BaseEntity()

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
    var apneaCount: Int? = null,
    @SerializedName("apnea_10")
    var apnea10: Int? = 0,
    @SerializedName("apnea_30")
    var apnea30: Int? = 0,
    @SerializedName("apnea_60")
    var apnea60: Int? = 0,
    @SerializedName("straight_position")
    var straightPositionTime: Int? = null,
    @SerializedName("left_position")
    var leftPositionTime: Int? = null,
    @SerializedName("right_position")
    var rightPositionTime: Int? = null,
    @SerializedName("down_position")
    var downPositionTime: Int? = null,
    @SerializedName("wake_time")
    var wakeTime: Int? = null,
    @SerializedName("straight_per")
    var straightPer: Int? = null,
    @SerializedName("left_per")
    var leftPer: Int? = null,
    @SerializedName("right_per")
    var rightPer: Int? = null,
    @SerializedName("down_per")
    var downPer: Int? = null,
    @SerializedName("wake_per")
    var wakePer: Int? = null,
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
    @SerializedName("avg_snore_count")
    var avgSnoreCount: String? = null,
    @SerializedName("deep_sleep_time")
    var deepSleepTime: Int? = null,

    @SerializedName("move_count")
    var moveCount: Int? = null,

    @SerializedName("rem_sleep_time")
    var remSleepTime: Int? = null,

    @SerializedName("light_sleep_time")
    var lightSleepTime: Int? = null,

    @SerializedName("wake_sleep_time")
    var wakeSleepTime: Int? = null,

    @SerializedName("fast_breath")
    var fastBreath: Int? = null,

    @SerializedName("slow_breath")
    var slowBreath: Int? = null,

    @SerializedName("unstable_breath")
    var unstableBreath: Int? = null,

    @SerializedName("avg_normal_breath")
    var avgNormalBreath: Int? = null,

    @SerializedName("normal_breath_time")
    var normalBreathTime: Int? = null,

    @SerializedName("description")
    val description: String? = "",

    @SerializedName("avg_fast_breath")
    val avgFastBreath: Int? = null,

    @SerializedName("avg_slow_breath")
    val avgSlowBreath: Int? = null,

    @SerializedName("snore_count")
    val snoreCount: Int? = null,

    @SerializedName("cough_count")
    val coughCount: Int? = null,

    @SerializedName("breath_score")
    val breathScore: Int? = null,

    @SerializedName("snore_score")
    val snoreScore: Int? = null,

    @SerializedName("ment")
    val ment: String? = null,

    @SerializedName("unstable_idx")
    val unstableIdx: String? = null,

    @SerializedName("nobreath_idx")
    val nobreath_idx: String? = null,

    @SerializedName("snoring_idx")
    val snoring_idx: String? = null,

    @SerializedName("cough_idx")
    val coughIdx: String? = null,

    @SerializedName("supine_idx")
    val supineIdx: String? = null,

    @SerializedName("left_idx")
    val leftIdx: String? = null,

    @SerializedName("right_idx")
    val rightIdx: String? = null,

    @SerializedName("prone_idx")
    val proneIdx: String? = null,

    @SerializedName("rem_idx")
    val remIdx: String? = null,

    @SerializedName("wake_idx")
    val wakeIdx: String? = null,

    @SerializedName("light_idx")
    val lightIdx: String? = null,

    @SerializedName("deep_idx")
    val deepIdx: String? = null,

    @SerializedName("movement_idx")
    val movement: String? = null,

    )