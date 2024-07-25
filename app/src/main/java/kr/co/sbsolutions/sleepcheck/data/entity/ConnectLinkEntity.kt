package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class ConnectLinkEntity (
    @SerializedName("result")
    val result: ConnectLinkData? = null
) : BaseEntity()

@Keep
data class ConnectLinkData(
    @SerializedName("url")
    val url : String
)