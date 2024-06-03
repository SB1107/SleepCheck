package kr.co.sbsolutions.sleepcheck.service

import android.app.Activity
import kr.co.sbsolutions.sleepcheck.presenter.firmware.FirmwareUpdateActivity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService: DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        return FirmwareUpdateActivity::class.java
    }
}