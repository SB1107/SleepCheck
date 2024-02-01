package kr.co.sbsolutions.newsoomirang.data.entity

data class SleepCreateEntity(
    // 사용자 아이디
    // 결과
    val result: SleepCreateEntityData? = null,
) : BaseEntity()
data class SleepCreateEntityData(
    // 토큰
    val id: Int = -1,
)