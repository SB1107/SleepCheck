package kr.co.sbsolutions.newsoomirang.common

enum class WebType( val titleKo: String,val titleEn: String, val url: String) {
    TERMS0("서비스 이용 약관", "Terms of Service","api/terms?type=0"),
    TERMS1("개인정보 수집 및 이용 정책", "Privacy Policy for Data Collection and Use","api/terms?type=1"),
    TERMS2("사용 설명서", "User Guide","https://sb-solutions.co.kr/main/m/manual/app_manual.html"),
    TERMS2EN("사용 설명서", "User Guide","https://sb-solutions.co.kr/manual/app_manual_en.html"),
}
