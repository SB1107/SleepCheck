package kr.co.sbsolutions.sleepcheck.domain.model

import com.google.gson.annotations.SerializedName

data class SignUpModel(
    @SerializedName("comp_code")
    private val compCode: String,
    @SerializedName("name")
    private val name: String,
    @SerializedName("birth")
    private val birth: String,
    @SerializedName("app_kind")
    private val appKind: String = "R"
)
