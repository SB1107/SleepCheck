package kr.co.sbsolutions.newsoomirang.domain.model

import com.google.gson.annotations.SerializedName

data class CheckSensor(
    @SerializedName("number")
    private val sensorName: String,
    @SerializedName("app_kind")
    private val appKind: String = "C"
)