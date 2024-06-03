package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class FAQEntity(
    @SerializedName("result")
    val result: FAQResultData? = null
): BaseEntity()

@Keep
data class FAQResultData(
    @SerializedName("data")
    val data: List<FAQContentsData> = emptyList(),
    @SerializedName("count")
    val count: Int? = null
)

@Keep
data class FAQContentsData(
    @SerializedName("sequence")
    val sequence: String = "",
    @SerializedName("question")
    val question: String? = null,
    @SerializedName("answer")
    val answer: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
)