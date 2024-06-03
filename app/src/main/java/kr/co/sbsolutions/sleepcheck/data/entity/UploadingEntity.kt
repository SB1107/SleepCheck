package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class UploadingEntity (
    @SerializedName("result")
    val result: Result
): BaseEntity()
@Keep
data class Result(
    @SerializedName("FunctionName")
    val functionName: String,

    @SerializedName("InvocationType")
    val invocationType: String,

    @SerializedName("Payload")
    val payload: Int,

    @SerializedName("LogType")
    val logType: String,
)