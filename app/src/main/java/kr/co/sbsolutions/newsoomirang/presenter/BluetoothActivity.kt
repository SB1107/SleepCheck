package kr.co.sbsolutions.newsoomirang.presenter

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel

abstract class BluetoothActivity : BaseActivity() {

    private lateinit var bluetoothActivityResultLauncher: ActivityResultLauncher<Intent>

//    private lateinit var gpsActivityResultLauncher: ActivityResultLauncher<Intent>

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        this.applicationContext?.getSystemService(BluetoothManager::class.java)?.run {
            return@run adapter
        }
    }

    /*private val locationManager: LocationManager by lazy {
        (this.applicationContext?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?: throw IllegalStateException("Location manager not available")
    }*/

//    private var isGpsEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBluetoothActive()
//        onGpsActive()
//        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: $bluetoothAdapter")
        bluetoothAdapter?.let {
            if (!it.isEnabled) {
                showAlertDialogWithCancel(
                    R.string.common_title, "블루투스를 활성화가 필요합니다. \n활성화 하시겠습니까?",
                    confirmButtonText = R.string.setting_bluetooth_connect,
                    confirmAction = {
                        requestBluetoothActivation()
                    },
                    cancelAction = {
                        finish()
                    }
                )
            }
        } ?: showAlertDialog(
            R.string.common_title, "블루투스 사용이 불가한 기기입니다\n 어플리케이션 을 종료 합니다.",
            cancelable = false,
            buttonText = R.string.common_ok,
            confirmAction = {
                finish()
            }
        )

        /*Log.d(TAG, "onResume11111111: $isGpsEnabled")
        if (!isGpsEnabled) {
            showAlertDialogWithCancel(
                R.string.common_title, "블루투스 스캔을 위해 GPS활성화가 필요합니다. \n활성화 하시겠습니까?",
                confirmButtonText = R.string.setting_bluetooth_connect,
                confirmAction = {
                    requestGpsActivation()
                },
                cancelAction = {
                    finish()
                }
            )
        }*/
    }


    private fun onBluetoothActive() {
        bluetoothActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // 사용자가 블루투스를 활성화했을 때의 처리

                } else {
                    // 사용자가 블루투스를 활성화하지 않았을 때의 처리
                    finish()
                }
            }
//        requestBluetoothActivation()
    }

    private fun requestBluetoothActivation() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothActivityResultLauncher.launch(enableBluetoothIntent)
    }

    /*private fun onGpsActive() {
        gpsActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // 사용자가 GPS를 활성화했을 때의 처리
                } else {
                    // 사용자가 GPS를 활성화하지 않았을 때의 처리
                    finish()
                }
            }
    }*/

    /*private fun requestGpsActivation() {
        val enableBluetoothIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        gpsActivityResultLauncher.launch(enableBluetoothIntent)
    }*/
}
