package kr.co.sbsolutions.sleepcheck.domain.model

data class NoSeringDataResultModel(
    val endDate : String,
    val duration : String,
    val resultTotal : String,
    val resultReal : String,
    val apneaState : Int
)
