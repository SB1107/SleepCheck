package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class FirmwareEntity(
    @SerializedName("result")
    val result: FirmwareEntityData? = null
) : BaseEntity()

@Keep
data class FirmwareEntityData (
    @SerializedName("ver")
    val newFirmVer: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("desc")
    val desc: String? = null,
    @SerializedName("sensor_ver")
    val sensorVer: String? = null
)