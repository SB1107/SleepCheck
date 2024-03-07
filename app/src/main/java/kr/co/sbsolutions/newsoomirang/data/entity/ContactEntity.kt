package kr.co.sbsolutions.newsoomirang.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ContactEntity(
    // 결과
    @SerializedName("result")
    val result: ContactResultData? = null,
) : BaseEntity()
@Keep
data class ContactResultData(
    // 토큰
    @SerializedName("data")
    var data: List<ContactData> = arrayListOf(),

    @SerializedName("count")
    var count: Int? = null
)
@Keep
data class ContactData(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("app_kind")
    val appKind: String? = null,

    @SerializedName("title")
    var title: String? = null,

    @SerializedName("content")
    var content: String? = null,

    @SerializedName("answer")
    var answer: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("lang")
    val lang: String? = null,

    @SerializedName("ans_content")
    var ansContent: String? = null,

    @SerializedName("ans_content_at")
    var token: String? = null,

    @SerializedName("ans_update_at")
    val ansUpdateAt: String? = null,
)