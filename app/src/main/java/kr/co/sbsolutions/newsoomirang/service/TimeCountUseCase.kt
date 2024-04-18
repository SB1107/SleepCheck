package kr.co.sbsolutions.newsoomirang.service

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.service.BLEService.Companion.FOREGROUND_SERVICE_NOTIFICATION_ID
import java.util.Locale

class TimeCountUseCase(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val timeHelper: TimeHelper,
    private val dataManager: DataManager,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManager,
    private val noseRingHelper: NoseRingHelper,
) {

    fun listenTimer() {
        lifecycleScope.launch(IO) {
            timeHelper.measuringTimer.collectLatest {
                notificationBuilder.setContentText(String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third))
                notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build())
                dataManager.setTimer(timeHelper.getTime())
                dataManager.setNoseRingTimer(noseRingHelper.getSnoreTime())
                dataManager.setCoughCount(noseRingHelper.getCoughCount())
                dataManager.setNoseRingCount(noseRingHelper.getSnoreCount())
            }
        }
    }
}