package kr.co.sbsolutions.newsoomirang.data.model

data class NoticeModel(
    // 공지사항 아이디
    var id: Int? = null,
    // 공지사항 타겟 - 0 : 사용자 , 1 : 파트너
    var target: Int? = null,
    // 공지사항 제목
    val title: String? = null,
    // 공지사항 내용
    val content: String? = null,
    // 결과
    val result: NoticeModel? = null,
    // 목록
    val data: ArrayList<NoticeModel>? = null,
)
