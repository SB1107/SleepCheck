package kr.co.sbsolutions.newsoomirang.domain.repository

import kr.co.sbsolutions.newsoomirang.BLEService
import javax.inject.Inject

class BleRepository @Inject constructor(){
    fun getBleService() : BLEService? {
        return  BLEService.getInstance()
    }
}