package kr.co.sbsolutions.sleepcheck.data.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ScoreEntity(
    @SerializedName("result")
    val result: ScoreResultData? = null
): BaseEntity()

@Keep
data class ScoreResultData(
    @SerializedName("data")
    val data: List<ScoreData> = emptyList(),
    @SerializedName("msg")
    val msg: String = "",
    val score : Int = 0,
)

@Keep
data class ScoreData(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("link")
    val link: String = "",
    @SerializedName("img")
    val image: String = "",
)


