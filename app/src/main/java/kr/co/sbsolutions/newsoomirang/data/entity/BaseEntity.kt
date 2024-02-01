package kr.co.sbsolutions.newsoomirang.data.entity

import com.google.gson.annotations.SerializedName

open class BaseEntity {
    @SerializedName("success")
    val success: Boolean = false

    @SerializedName("message")
    val message: String = ""
}