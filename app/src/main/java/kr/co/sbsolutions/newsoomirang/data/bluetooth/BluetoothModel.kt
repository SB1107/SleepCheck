package kr.co.sbsolutions.newsoomirang.data.bluetooth

import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kotlin.experimental.inv

sealed interface AppToModule {
    object BreathingOperateStart : AppToModule
    object NoSeringOperateStart : AppToModule
    object BreathingOperateStop : AppToModule
    object NoSeringOperateStop : AppToModule
    object VibrationNotificationsWeak :AppToModule
    object VibrationNotificationsNormal :AppToModule
    object VibrationNotificationsStrong :AppToModule
    object OperateChangeProcessRealtime : AppToModule
    object OperateChangeProcessDelayed : AppToModule
    object OperateDownloadContinue : AppToModule
    object OperateDownload : AppToModule
    object OperateDeleteAll : AppToModule
    object OperateDeleteSector : AppToModule

}
sealed interface AppToModuleResponse {
    object RealtimeDataResponseACK : AppToModuleResponse
    object RealtimeDataResponseNAK : AppToModuleResponse
    object DelayedDataResponseACK : AppToModuleResponse
    object DelayedDataResponseNAK : AppToModuleResponse
    object MemoryDataResponseACK : AppToModuleResponse
    object MemoryDataResponseNAK : AppToModuleResponse
    object PowerOffACK : AppToModuleResponse
    object MOTCtrlSetACK : AppToModuleResponse
}

fun AppToModule.getState() : BluetoothState.Connected {
    return when(this){
        AppToModule.BreathingOperateStart,  AppToModule.NoSeringOperateStart-> BluetoothState.Connected.SendStart
        AppToModule.BreathingOperateStop,AppToModule.NoSeringOperateStop -> BluetoothState.Connected.SendStop
        AppToModule.VibrationNotificationsWeak , AppToModule.VibrationNotificationsNormal , AppToModule.VibrationNotificationsStrong->BluetoothState.Connected.MotCtrlSet
        AppToModule.OperateChangeProcessRealtime -> BluetoothState.Connected.SendRealtime
        AppToModule.OperateChangeProcessDelayed -> BluetoothState.Connected.SendDelayed
        AppToModule.OperateDownloadContinue -> BluetoothState.Connected.SendDownloadContinue
        AppToModule.OperateDownload -> BluetoothState.Connected.SendDownload
        AppToModule.OperateDeleteAll, AppToModule.OperateDeleteSector -> BluetoothState.Connected.SendDelete
    }
}

fun AppToModule.getCommandByteArr() : ByteArray {
    return when(this) {
        AppToModule.BreathingOperateStart -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF3.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModule.NoSeringOperateStart -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xFE.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModule.BreathingOperateStop -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF3.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModule.NoSeringOperateStop -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xFE.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModule.VibrationNotificationsWeak -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF9.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
            AppToModule.VibrationNotificationsNormal -> {
                byteArrayOf(
                    // PREFIX
                    0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                    // CMD
                    0xF9.toByte(),
                    // LEN
                    0x01.toByte(),
                    // Payload
                    0x02.toByte()
                ).addCheckSum()
        }
        AppToModule.VibrationNotificationsStrong -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF9.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x03.toByte()
            ).addCheckSum()
        }
        AppToModule.OperateChangeProcessRealtime -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF1.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModule.OperateChangeProcessDelayed -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF1.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModule.OperateDownload, AppToModule.OperateDownloadContinue -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF5.toByte(),
                // LEN
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModule.OperateDeleteAll -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF6.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModule.OperateDeleteSector -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xF6.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
    }
}

fun AppToModuleResponse.getCommandByteArr() : ByteArray {
    return when(this) {
        AppToModuleResponse.RealtimeDataResponseACK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC2.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.RealtimeDataResponseNAK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC2.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.DelayedDataResponseACK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC4.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.DelayedDataResponseNAK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC4.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.MemoryDataResponseACK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC7.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.MemoryDataResponseNAK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC7.toByte(),
                // LEN
                0x01.toByte(),
                // Payload
                0x01.toByte()
            ).addCheckSum()
        }

        AppToModuleResponse.PowerOffACK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xCD.toByte(),
                // LEN
                0x00.toByte()
            ).addCheckSum()
        }
        AppToModuleResponse.MOTCtrlSetACK -> {
            byteArrayOf(
                // PREFIX
                0xFE.toByte(), 0x9B.toByte(), 0x80.toByte(), 0x03.toByte(),
                // CMD
                0xC9.toByte(),
                // LEN
                0x00.toByte()
            ).addCheckSum()
        }
    }
}

sealed interface ModuleToApp {
    object StartStopACK : ModuleToApp
    object NoSeringStopACK : ModuleToApp
    object RealtimeData : ModuleToApp
    @Deprecated("사라짐")
    object DelayedData : ModuleToApp
    object OperateACK : ModuleToApp
    object MOTCtrlSetACK : ModuleToApp
    object MemoryData : ModuleToApp
    object MemoryDataACK : ModuleToApp
    object MemoryDataDeleteACK : ModuleToApp
    object PowerOff : ModuleToApp
    object Error : ModuleToApp
    object BatteryState : ModuleToApp
}

fun String.getCommand() : ModuleToApp {
    return when(this) {
        "C3" -> ModuleToApp.StartStopACK
        "CE" -> ModuleToApp.NoSeringStopACK
        "F2" -> ModuleToApp.RealtimeData
        "C1" -> ModuleToApp.OperateACK
        "F9" -> ModuleToApp.MOTCtrlSetACK
        "F4" -> ModuleToApp.DelayedData
        "F7" -> ModuleToApp.MemoryData
        "C5" -> ModuleToApp.MemoryDataACK
        "C6" -> ModuleToApp.MemoryDataDeleteACK
        "FD" -> ModuleToApp.PowerOff
        "FA" -> ModuleToApp.BatteryState
        else -> ModuleToApp.Error
    }
}

fun ByteArray.addCheckSum() : ByteArray {
    val sum1 = sumOf {
        it.toInt() and 0x00ff
    }
    val sum2 = (sum1 shr 8 and 0xff) + (sum1 and 0xff)
    val sum3 = ((sum2 shr 8 and 0xFF) + (sum2 and 0xFF)).toByte().toShort()
    val checksum = sum3.toByte().inv()

    return plus(checksum)
}

fun ByteArray.verifyCheckSum() : Boolean {
    val receivedList = toMutableList()
    val receivedCheckSum = receivedList.removeLast()

    val sum1 = receivedList.sumOf {
        it.toInt() and 0x00ff
    }
    val sum2 = (sum1 shr 8 and 0xff) + (sum1 and 0xff)
    val sum3 = ((sum2 shr 8 and 0xFF) + (sum2 and 0xFF)).toByte().toShort()
    val checksum = sum3.toByte().inv()

    return receivedCheckSum == checksum
}