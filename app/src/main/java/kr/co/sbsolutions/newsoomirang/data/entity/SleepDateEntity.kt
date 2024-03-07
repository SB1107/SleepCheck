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
    val data: List<SleepDateResult> = arrayListOf(),
    @SerializedName("count")
    val count : Int = 0
)
@Keep
data class SleepDateResult(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: Int = 0,
    @SerializedName("started_at")
    val startedAt: String? ,
    @SerializedName("ended_at")
    val endedAt: String? ,
)