package kr.co.sbsolutions.newsoomirang.domain.model

import com.google.gson.annotations.SerializedName

data class ContactDetail(
    @SerializedName("title")
    private val title: String,
    @SerializedName("content")
    private val detail: String,
    @SerializedName("app_kind")
    private val appKind: String = "C"
)
