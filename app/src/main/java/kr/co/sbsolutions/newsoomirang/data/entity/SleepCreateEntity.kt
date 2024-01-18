package kr.co.sbsolutions.newsoomirang.data.entity

data class SleepCreateEntity(
    // API 성공 여부
    val success: Boolean = false,
    // API 메세지
    val message: String = "",
    // 사용자 아이디
    // 결과
    val result: SleepCreateEntityData? = null,
)
data class SleepCreateEntityData(
    // 토큰
    val id: Int = -1,
)