package kr.co.sbsolutions.newsoomirang.common

enum class WebType(val title: String, val url: String) {
    TERMS0("서비스 이용 약관", "api/terms?type=0"),
    TERMS1("개인정보 수집 및 이용 정책", "api/terms?type=1"),
    TERMS2("위치기반 서비스 이용약관", "terms?type=2"),
    AUTH("본인인증", ""),
    TEST_AUTH("본인인증 테스트", "api/verification"),
    ADDRESS("주소검색", "mobile/address/daum"),
    PAYMENT("결제", "")
}
