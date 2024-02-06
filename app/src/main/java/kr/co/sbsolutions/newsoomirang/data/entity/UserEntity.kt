package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep

@Keep
data class UserEntity(
    // 결과
    val result: UserResultData? = null,
) : BaseEntity()
@Keep
data class UserResultData(
    // 토큰
    val access_token: String? = null,
    // 토큰 타입
    val token_type: String? = null,
    // 사용자 정보
    val user: UserData? = null,
    val member: String = "N"
)
@Keep
data class UserData(
    val id: Int? = null,
    // 이메일
    val email: String? = null,
    // 비밀번호
    val password: String? = null,
    // Google FCM Key
    var fcm_key: String? = null,
    // 디바이스 종류. 1: Android, 2: IOS, 3: Other
    var device_type: Int? = null,
    // 사용자 이름
    var name: String? = null,
    // 사용자 닉네임
    val nickname: String? = null,
    // 사용자 전화번호
    val phone: String? = null,
    val unique_key: String? = null,
    // SNS 타입
    var sns_type: String? = null,
    // 토큰
    var token: String? = null,
    // 성별. 0: 남자, 1: 여자
    val gender: String? = null,
    // 생년월일
    val birth: String? = null,
    // 마지막 사용시간
    val last_used_at: String? = null,
    // 서버 데이터 저장 동의 - 0:동의 안함, 1:동의
    var is_app_data: Int? = null,
    // 앱 개선에 데이터 활용 동의 - 0:동의 안함, 1:동의
    var is_server_data: Int? = null,
    var leave_reason: String? = null,
)