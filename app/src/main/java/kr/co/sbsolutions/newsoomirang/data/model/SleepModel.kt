package kr.co.sbsolutions.newsoomirang.data.model

import kotlinx.coroutines.flow.MutableSharedFlow

data class SleepModel(
    var id: Int? = null,
    var user_id: Int? = null,
    var number: String? = null,
    var dirname: String? = null,
    var filename: String? = null,
    var asleep_time: Int? = null,
    var type: Int? = null,
    var snore_time: Int? = null,
    var apnea_state: Int? = null,
    var apnea_count: Int? = null,
    var apnea_10: Int? = null,
    var apnea_30: Int? = null,
    var apnea_60: Int? = null,
    var straight_position: Int? = null,
    var left_position: Int? = null,
    var right_position: Int? = null,
    var down_position: Int? = null,
    var wake_time: Int? = null,
    var sleep_pattern: String? = null,
    var started_at: String? = null,
    var ended_at: String? = null,
    var sleep_time: Int? = null,
    var state: Int? = null,
    var day: String? = null,
    var minute: Int? = null,
    var result: SleepModel? = null,
    var data: ArrayList<SleepModel>? = null
)
