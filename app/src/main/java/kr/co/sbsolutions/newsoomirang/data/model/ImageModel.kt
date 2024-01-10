package kr.co.sbsolutions.newsoomirang.data.model


data class ImageModel(
    // 이미지 아이디
    val id: Int? = null,
    // 이미지 경로
    val path: String? = null,
    // 이미지 가로 길이
    val width: Int? = null,
    // 이미지 세로 길이
    val height: Int? = null,
    val result: ImageModel? = null

)