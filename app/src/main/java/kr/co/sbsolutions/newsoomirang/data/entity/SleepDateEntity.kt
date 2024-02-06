package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class SleepDateEntity (
    @SerializedName("result")
    val result: SleepDateResultData?
): BaseEntity()
@Keep
data class SleepDateResultData(
    @SerializedName("data")
    var data: List<SleepDateResult> = arrayListOf(),
    @SerializedName("count")
    var count : Int = 0
)
@Keep
data class SleepDateResult(
    @SerializedName("minute")
    var minute: Int = 0,
    @SerializedName("day")
    var day: String? = null,
    @SerializedName("state")
    var state: Int = 0,
    @SerializedName("sleep_time")
    var sleepTime: Int = 0,
    @SerializedName("asleep_time")
    var asleepTime: Int = 0,
    @SerializedName("apnea_state")
    var apneaState: Int = 0,
)