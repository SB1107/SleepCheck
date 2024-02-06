package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SleepCreateEntity(
    // 사용자 아이디
    // 결과
    @SerializedName("result")
    val result: SleepCreateEntityData? = null,
) : BaseEntity()
@Keep
data class SleepCreateEntityData(
    // 토큰
    @SerializedName("id")
    val id: Int = -1,
)