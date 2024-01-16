package kr.co.sbsolutions.newsoomirang.presenter

sealed class ActionMessage(val msg: String) {
    object StartBreathingService : ActionMessage(startBreathingService)
    object StopBreathingService : ActionMessage(stopBreathingService)
    object StartNoSeringService : ActionMessage(startNoSeringSensor)
    object StopNoSeringService : ActionMessage(stopNoSeringSensor)
    object StopBreathingServiceForced : ActionMessage(stopBreathingServiceForced)
    object StopNoSeringServiceForced : ActionMessage(stopNoSeringServiceForced)
    object OperateBreathingRealtimeSBSensor : ActionMessage(operateBreathingRealtimeSBSensor)
    object OperateNoSeringRealtimeSBSensor : ActionMessage(operateNoSeringRealtimeSBSensor)
    object OperateBreathingDelayedSBSensor : ActionMessage(operateBreathingDelayedSBSensor)
    object OperateNoSeringDelayedSBSensor : ActionMessage(operateNoSeringDelayedSBSensor)
    object OperateBreathingDownloadSBSensor : ActionMessage(operateBreathingDownloadSBSensor)
    object OperateNoSeringDownloadSBSensor : ActionMessage(operateNoSeringDownloadSBSensor)
    object OperateBreathingDeleteSectorSBSensor : ActionMessage(operateBreathingDeleteSectorSBSensor)
    object OperateNoSeringDeleteSectorSBSensor : ActionMessage(operateNoSeringDeleteSectorSBSensor)
    object OperateBreathingDeleteAllSBSensor : ActionMessage(operateBreathingDeleteAllSBSensor)
    object OperateNoSeringDeleteAllSBSensor : ActionMessage(operateNoSeringDeleteAllSBSensor)
    /*
    // Job Scheduler 미사용 - Doze Issue
    object UploadToServerSBSensor : ActionMessage(uploadToServerSBSensor)
    */

    companion object {
        private const val startBreathingService = "startBreathingService"
        private const val stopBreathingService = "stopBreathingService"
        private const val stopBreathingServiceForced = "stopBreathingServiceForced"
        private const val startNoSeringSensor = "startNoSeringSensor"
        private const val stopNoSeringSensor = "stopNoSeringSensor"
        private const val stopNoSeringServiceForced = "stopNoSeringServiceForced"
        private const val operateBreathingRealtimeSBSensor = "operateBreathingRealtimeSBSensor"
        private const val operateNoSeringRealtimeSBSensor = "operateNoSeringRealtimeSBSensor"
        private const val operateBreathingDelayedSBSensor = "operateBreathingDelayedSBSensor"
        private const val operateNoSeringDelayedSBSensor = "operateNoSeringDelayedSBSensor"
        private const val operateBreathingDownloadSBSensor = "operateBreathingDownloadSBSensor"
        private const val operateNoSeringDownloadSBSensor = "operateNoSeringDownloadSBSensor"
        private const val operateBreathingDeleteSectorSBSensor = "operateBreathingDeleteSectorSBSensor"
        private const val operateNoSeringDeleteSectorSBSensor = "operateNoSeringDeleteSectorSBSensor"
        private const val operateBreathingDeleteAllSBSensor = "operateBreathingDeleteAllSBSensor"
        private const val operateNoSeringDeleteAllSBSensor = "operateNoSeringDeleteAllSBSensor"
        /*
        // Job Scheduler 미사용 - Doze Issue
        private const val uploadToServerSBSensor = "uploadToServerSBSensor"
        */

        fun getMessage(msg: String): ActionMessage? {
            return when (msg) {
                startBreathingService -> StartBreathingService
                stopBreathingService -> StopBreathingService
                stopBreathingServiceForced -> StopBreathingServiceForced
                startNoSeringSensor -> StartNoSeringService
                stopNoSeringSensor -> StopNoSeringService
                stopNoSeringServiceForced -> StopNoSeringServiceForced
                operateBreathingRealtimeSBSensor -> OperateBreathingRealtimeSBSensor
                operateNoSeringRealtimeSBSensor -> OperateNoSeringRealtimeSBSensor
                operateBreathingDelayedSBSensor -> OperateBreathingDelayedSBSensor
                operateNoSeringDelayedSBSensor -> OperateNoSeringDelayedSBSensor
                operateBreathingDownloadSBSensor -> OperateBreathingDownloadSBSensor
                operateNoSeringDownloadSBSensor -> OperateNoSeringDownloadSBSensor
                operateBreathingDeleteSectorSBSensor -> OperateBreathingDeleteSectorSBSensor
                operateNoSeringDeleteSectorSBSensor -> OperateNoSeringDeleteSectorSBSensor
                operateBreathingDeleteAllSBSensor -> OperateBreathingDeleteAllSBSensor
                operateNoSeringDeleteAllSBSensor -> OperateNoSeringDeleteAllSBSensor
                /*
                // Job Scheduler 미사용 - Doze Issue
                uploadToServerSBSensor -> UploadToServerSBSensor
                */
                else -> null
            }
        }
    }
}