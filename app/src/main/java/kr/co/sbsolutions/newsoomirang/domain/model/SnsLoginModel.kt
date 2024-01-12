package kr.co.sbsolutions.newsoomirang.domain.model

data class SnsLoginModel(val sns_type : String , val token : String ,val device_type : String = "1"  , val  fcm_key : String)
