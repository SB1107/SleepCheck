package kr.co.sbsolutions.newsoomirang.domain.model

data class SleepDataResultModel(
      val endDate : String,
      val duration : String,
      val resultTotal : String,
      val resultReal : String,
      val resultAsleep : String,
      val apneaState : Int,
)
