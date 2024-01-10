package kr.co.sbsolutions.newsoomirang.data.model

data class FaqModel(
    // FAQ 아이디
    val id: Int? = null,
    // FAQ 질문
    val title: String? = null,
    // FAQ 답변
    val content: String? = null,
    // 타겟
    var target: Int? = null,
    // 결과
    val result: FaqModel? = null,
    // 목록
    val data: ArrayList<FaqModel>? = null,
)
