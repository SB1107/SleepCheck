package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class SensorFirmVersion(
    @SerializedName("number")
    private val number: String,
    @SerializedName("version")
    private val version: String,
    @SerializedName("app_kind")
    private val appKind: String = "C"
)
