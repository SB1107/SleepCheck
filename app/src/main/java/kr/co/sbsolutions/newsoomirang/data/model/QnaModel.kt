package kr.co.sbsolutions.newsoomirang.data.model

data class QnaModel(
    // QNA 아이디
    var id: Int? = null,
    // QNA 제목
    var title: String? = null,
    // QNA 내용
    var content: String? = null,
    // QNA 카테고리
    var category: Int? = null,
    // QNA 답변
    val reply_content: String? = null,
    // QNA 답변 여부
    val reply_yn: Int? = null,
    // QNA 답변 날짜
    val reply_date: String? = null,
    // 결과
    val result: QnaModel? = null,
    // 목록
    val data: ArrayList<QnaModel>? = null,
)