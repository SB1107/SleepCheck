package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep

@Keep
data class SleepCreateEntity(
    // 사용자 아이디
    // 결과
    val result: SleepCreateEntityData? = null,
) : BaseEntity()
@Keep
data class SleepCreateEntityData(
    // 토큰
    val id: Int = -1,
)