package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
open class BaseEntity {
    @SerializedName("success")
    val success: Boolean = false

    @SerializedName("message")
    val message: String = ""
}