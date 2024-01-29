package kr.co.sbsolutions.newsoomirang.presenter

sealed class ActionMessage(val msg: String) {
    object StartSBService : ActionMessage(startSBService)
    object StopSBService : ActionMessage(stopSBService)

    object CancelSbService : ActionMessage(cancelSBService)
    object StopSBServiceForced : ActionMessage(stopSBServiceForced)
    object OperateRealtimeSBSensor : ActionMessage(operateRealtimeSBSensor)
    object OperateDelayedSBSensor : ActionMessage(operateDelayedSBSensor)
    object OperateDownloadSBSensor : ActionMessage(operateDownloadSBSensor)
    object OperateDeleteSectorSBSensor : ActionMessage(operateDeleteSectorSBSensor)
    object OperateDeleteAllSBSensor : ActionMessage(operateDeleteAllSBSensor)
    /*
    // Job Scheduler 미사용 - Doze Issue
    object UploadToServerSBSensor : ActionMessage(uploadToServerSBSensor)
    */

    companion object {
        private const val startSBService = "startSBService"
        private const val stopSBService = "stopSBService"
        private const val stopSBServiceForced = "stopSBServiceForced"
        private const val cancelSBService = "cancelSBService"
//        private const val startSBSensor = "startSBSensor"
//        private const val stopSBSensor = "stopSBSensor"
        private const val operateRealtimeSBSensor = "operateRealtimeSBSensor"
        private const val operateDelayedSBSensor = "operateDelayedSBSensor"
        private const val operateDownloadSBSensor = "operateDownloadSBSensor"
        private const val operateDeleteSectorSBSensor = "operateDeleteSectorSBSensor"
        private const val operateDeleteAllSBSensor = "operateDeleteAllSBSensor"
        /*
        // Job Scheduler 미사용 - Doze Issue
        private const val uploadToServerSBSensor = "uploadToServerSBSensor"
        */

        fun getMessage(msg: String): ActionMessage? {
            return when (msg) {
                startSBService -> StartSBService
                stopSBService -> StopSBService
                stopSBServiceForced -> StopSBServiceForced
//                    startSBSensor -> StartSBSensor
//                    stopSBSensor -> StopSBSensor
                cancelSBService -> CancelSbService
                operateRealtimeSBSensor -> OperateRealtimeSBSensor
                operateDelayedSBSensor -> OperateDelayedSBSensor
                operateDownloadSBSensor -> OperateDownloadSBSensor
                operateDeleteSectorSBSensor -> OperateDeleteSectorSBSensor
                operateDeleteAllSBSensor -> OperateDeleteAllSBSensor
                /*
                // Job Scheduler 미사용 - Doze Issue
                uploadToServerSBSensor -> UploadToServerSBSensor
                */
                else -> null
            }
        }
    }
}