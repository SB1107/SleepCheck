package kr.co.sbsolutions.newsoomirang.common

import android.Manifest.permission.*
import android.os.Build
import androidx.annotation.RequiresApi

object Cons {
    const val TAG = "HoHo"

    const val PERMISSION_REQUEST_CODE = 1107

    const val BASE_URL_VERSION = "api"

    const val EXTRA_KEY_USER_KEY = "${TAG}_userKey"
    const val EXTRA_KEY_BLE_KEY = "${TAG}_bleKey"
    const val NOTIFICATION_CHANNEL_ID = "kr.co.sbsolutions.newsoomirang"
    const val NOTIFICATION_CHANNEL_NAME = "BleService channel "
    const val NOTIFICATION_ID = 1248
    const val NOTIFICATION_ACTION = "ACTION_SEND_DATA"
    val PERMISSIONS_ALL = arrayOf(
        ACCESS_FINE_LOCATION,

    )

    @RequiresApi(Build.VERSION_CODES.Q)
    val PERMISSIONS_29 = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(
            USE_FULL_SCREEN_INTENT,
        )
    } else {
        emptyArray()
    }
    val PERMISSIONS_30 = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN
        )
    } else {
        emptyArray()
    }

    val PERMISSIONS_31 =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                BLUETOOTH_SCAN,
                BLUETOOTH_CONNECT
            )
        } else {
            emptyArray()
        }
    val PERMISSIONS_33 =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

/** SERVICE_UUID */
const val SERVICE_STRING = "3a95e1b9-d1a2-4876-8335-02108039b3a2"

/** SEND to BLE */
const val CHARACTERISTIC_COMMAND_STRING = "3a95e1b9-d1a2-4876-8335-02108039b3a2"

/** RECEIVE from BLE */
const val CHARACTERISTIC_RESPONSE_STRING = "3a95e1b8-d1a2-4876-8335-02108039b3a2"

const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

/** UPLOAD CHECK **/
const val MINIMUM_UPLOAD_NUMBER = 300 * 30

/** OnClickInterval**/
const val ON_CLICK_INTERVAL = 1000L

/** Snoring Vibration Start Time**/
const val SNORING_VIBRATION_DELAYED_START_TIME = 1000L * 60 * 30
}