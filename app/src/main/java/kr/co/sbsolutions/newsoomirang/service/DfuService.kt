package kr.co.sbsolutions.newsoomirang.service

import android.app.Activity
import kr.co.sbsolutions.newsoomirang.presenter.firmware.FirmwareUpdateActivity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService: DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        return FirmwareUpdateActivity::class.java
    }
}