package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UserEntity(
    // 결과
    @SerializedName("result")
    val result: UserResultData? = null,
) : BaseEntity()
@Keep
data class UserResultData(
    // 토큰
    @SerializedName("access_token")
    val access_token: String? = null,
    // 토큰 타입
    @SerializedName("token_type")
    val token_type: String? = null,
    // 사용자 정보
    @SerializedName("user")
    val user: UserData? = null,
    @SerializedName("member")
    val member: String = "N",
)
@Keep
data class UserData(
    @SerializedName("id")
    val id: Int? = null,
    // 이메일
    @SerializedName("email")
    val email: String? = null,
    // 비밀번호
    @SerializedName("password")
    val password: String? = null,
    // Google FCM Key
    @SerializedName("fcm_key")
    var fcm_key: String? = null,
    // 디바이스 종류. 1: Android, 2: IOS, 3: Other
    @SerializedName("device_type")
    var device_type: Int? = null,
    // 사용자 이름
    @SerializedName("name")
    var name: String? = null,
    // 사용자 닉네임
    @SerializedName("nickname")
    val nickname: String? = null,
    // 사용자 전화번호
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("unique_key")
    val unique_key: String? = null,
    // SNS 타입
    @SerializedName("sns_type")
    var sns_type: String? = null,
    // 토큰
    @SerializedName("token")
    var token: String? = null,
    // 성별. 0: 남자, 1: 여자
    @SerializedName("gerder")
    val gender: String? = null,
    // 생년월일
    @SerializedName("birth")
    val birth: String? = null,
    // 마지막 사용시간
    @SerializedName("last_used_at")
    val last_used_at: String? = null,
    // 서버 데이터 저장 동의 - 0:동의 안함, 1:동의
    @SerializedName("is_app_data")
    var is_app_data: Int? = null,
    // 앱 개선에 데이터 활용 동의 - 0:동의 안함, 1:동의
    @SerializedName("is_server_data")
    var is_server_data: Int? = null,
    @SerializedName("leave_reason")
    var leave_reason: String? = null,
)