package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UpdateUserEntity (
    // 결과
    @SerializedName("result")
    val result: UpdateUserResultData? = null,
) : BaseEntity()

data class UpdateUserResultData (
    @SerializedName("id")
    val id: Int? = null,
    // 이메일
    @SerializedName("name")
    var name: String? = null,
    // 사용자 닉네임
    @SerializedName("birth")
    val birth: String? = null,
    // 사용자 전화번호
    @SerializedName("comp_code")
    val compCode: String? = null,
    // SNS 타입
    @SerializedName("sns_type")
    var snsType: String? = null,
    // 토큰
    @SerializedName("token")
    var token: String? = null,
    // 서버 데이터 저장 동의 - 0:동의 안함, 1:동의
    @SerializedName("is_app_data")
    var isAppData: Int? = null,
    // 성별. 0: 남자, 1: 여자
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("leave_reason")
    val leaveReason: String? = null,
    @SerializedName("leaved_at")
    val leavedAt: String? = null,
    // 마지막 사용시간
    @SerializedName("login_date")
    val loginDate: String? = null,
)