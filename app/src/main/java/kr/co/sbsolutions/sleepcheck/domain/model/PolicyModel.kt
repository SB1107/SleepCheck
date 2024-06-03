package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class PolicyModel(
    @SerializedName("is_server_data")
    val isServerData: Int = 1,

    @SerializedName("is_app_data")
    val isAppData: Int = 1,
)