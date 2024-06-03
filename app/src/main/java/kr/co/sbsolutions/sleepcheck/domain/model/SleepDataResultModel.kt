package kr.co.sbsolutions.sleepcheck.domain.model

data class SleepDataResultModel(
      val endDate : String,
      val duration : String,
      val resultTotal : String,
      val resultReal : String,
      val resultAsleep : String,
      val apneaState : Int,
      val moveCount :String,
      val deepSleepTime :String,
      val resultSnoreTime : String,
      val totalApneaCount : String
)
