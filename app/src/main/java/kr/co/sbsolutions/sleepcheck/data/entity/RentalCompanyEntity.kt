package kr.co.sbsolutions.sleepcheck.data.entity

import com.google.gson.annotations.SerializedName

data class RentalCompanyEntity(
    @SerializedName("result")
    val result: RentalCompanyResultData? = null,
) : BaseEntity()


data class RentalCompanyResultData(
    @SerializedName("data")
    val data: List<RentalCompanyItemData> = emptyList(),
    // 이메일
    @SerializedName("cnt")
    var count: Int? = null,
)

data class RentalCompanyItemData(
    @SerializedName("name")
    val name: String = "",
    // 이메일
    @SerializedName("code")
    var code: String = "",
)