package kr.co.sbsolutions.newsoomirang.data.entity

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
    val data: List<FAQContentsData> = arrayListOf(FAQContentsData(question = "테스트", answer = "테스트"),FAQContentsData(question = "테스트1", answer = "테스트1"),FAQContentsData(question = "테스트2", answer = "테스트2")),
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