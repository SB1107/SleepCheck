package kr.co.sbsolutions.newsoomirang.common

import android.Manifest.permission.*
import android.os.Build

object Cons {
    const val TAG = "HoHo"

    const val PERMISSION_REQUEST_CODE = 1107

    const val BASE_URL_VERSION = "api"

    const val EXTRA_KEY_USER_KEY = "${TAG}_userKey"
    const val EXTRA_KEY_BLE_KEY = "${TAG}_bleKey"

    val PERMISSIONS_ALL = arrayOf(
        ACCESS_FINE_LOCATION,
        BLUETOOTH
    )
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
}