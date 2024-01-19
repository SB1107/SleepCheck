package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

data class UploadingEntity (
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("result")
    val result: Result
)

data class Result(
    @SerializedName("data")
    val data: List<DetailData>,

    @SerializedName("count")
    val count: Int
)
data class DetailData(
    @SerializedName("day")
    val day: String,

    @SerializedName("started_at")
    val started_at: String,

    @SerializedName("minute")
    val minute: Int,

    @SerializedName("apnea_10")
    val apnea_10: Int,

    @SerializedName("apnea_30")
    val apnea_30: Int,

    @SerializedName("apnea_60")
    val apnea_60: Int,

    @SerializedName("sleep_time")
    val sleep_time: Int,

    @SerializedName("state")
    val state: Int,

    @SerializedName("ended_at")
    val ended_at: String

)