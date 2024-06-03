package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class SleepDataRemoveModel(
    @SerializedName("id")
    private var id: Int = -1
)
