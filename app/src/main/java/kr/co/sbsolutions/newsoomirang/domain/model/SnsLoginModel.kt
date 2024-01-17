package kr.co.sbsolutions.newsoomirang.domain.model

import com.google.gson.annotations.SerializedName

data class SnsLoginModel(
//    val sns_type: String,
//    val token: String,
//    val device_type: Int = 1,
//    val fcm_key: String,
//    val name: String

    @SerializedName("sns_type")
    val sns_Type: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("name")
    val name: String,

    /*@SerializedName("is_server_data")
    val is_server_data: Int = 1,

    @SerializedName("is_app_data")
    val is_app_data: Int = 1,*/

    @SerializedName("device_type")
    val device_type : Int = 1,

    @SerializedName("fcm_key")
    val fcm_key: String,

    @SerializedName("app_kind")
    val app_kind : String = "C"

)
