package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

data class FirmwareEntity(
    @SerializedName("ver")
    val newFirmVer: String? = null,
    @SerializedName("url")
    val url: String? = null
): BaseEntity()
