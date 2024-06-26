package kr.co.sbsolutions.sleepcheck.data.server

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("code")
    val code: String,
)
