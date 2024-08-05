package kr.co.sbsolutions.sleepcheck.data.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName


data class SleepDetailDTO(
    var id: Int? = 0,
    var userId: Int? = 0,
    var number: String? = null,
    var dirName: String? = null,
    var fileName: String? = null,
    var asleepTime: Int? = 0,
    var type: Int = 0,
    var snoreTime: Int? = 0,
    var apneaState: Int? = null,
    var apneaCount: Int? = null,
    var apnea10: Int? = 0,
    var apnea30: Int? = 0,
    var apnea60: Int? = 0,
    var straightPositionTime: Int? = null,
    var leftPositionTime: Int? = null,
    var rightPositionTime: Int? = null,
    var downPositionTime: Int? = null,
    var wakeTime: Int? = null,
    var straightPer: Int? = null,
    var leftPer: Int? = null,
    var rightPer: Int? = null,
    var downPer: Int? = null,
    var wakePer: Int? = null,
    var sleepPattern: String? = null,
    var startedAt: String? = null,
    var endedAt: String? = null,
    var avgSnoreCount: String? = null,
    var sleepTime: Int? = 0,
    var state: Int? = null,
    var deepSleepTime: Int? = null,
    var moveCount: Int? = null,
    var remSleepTime: Int? = null,
    var lightSleepTime: Int? = null,
    var wakeSleepTime: Int? = null,
    var fastBreath: Int? = null,
    var slowBreath: Int? = null,
    var unstableBreath: Int? = null,
    var avgNormalBreath: Int? = null,
    var normalBreathTime: Int? = null,
    val description: String? = "",
    val avgFastBreath: Int? = null,
    val avgSlowBreath: Int? = null,
    val snoreCount: Int? = null,
    val coughCount: Int? = null,
    val breathScore: Int? = null,
    val snoreScore: Int? = null,
    val ment: String? = null,
    val unstableIdx:  List<String> = emptyList(),
    val nobreathIdx:  List<String> = emptyList(),
    val snoringIdx:  List<String> = emptyList(),
    val coughIdx:  List<String> = emptyList(),
    val wakeIdx:  List<String> = emptyList(),
    val supineIdx : List<String> = emptyList(),
    val leftIdx : List<String> = emptyList(),
    val rightIdx : List<String> = emptyList(),
    val proneIdx : List<String> = emptyList(),
    val remIdx : List<String> = emptyList(),
    val lightIdx : List<String> = emptyList(),
    val deepIdx : List<String> = emptyList(),
    val movement : List<String> = emptyList(),
)

data class SnoringAndCough(
    val snoringIdx:  List<String> = emptyList(),
    val snoringColor : Color = Color.Transparent,
    val coughIdx:  List<String> = emptyList(),
    val coughColor : Color = Color.Transparent,

    )