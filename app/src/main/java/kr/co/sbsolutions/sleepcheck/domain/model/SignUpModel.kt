package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class SignUpModel(
    @SerializedName("title")
    private val title: String,
    @SerializedName("content")
    private val detail: String,
    @SerializedName("app_kind")
    private val appKind: String = "C"
)
