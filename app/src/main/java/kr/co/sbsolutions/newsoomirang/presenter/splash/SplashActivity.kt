package kr.co.sbsolutions.newsoomirang.presenter.splash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.PERMISSION_REQUEST_CODE
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.addFlag
import kr.co.sbsolutions.newsoomirang.common.getPermissionResult
import kr.co.sbsolutions.newsoomirang.common.isIgnoringBatteryOptimizations
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.databinding.ActivitySplashBinding
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginActivity
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }
    private val viewModel: SplashViewModel by viewModels()
    private  val appUpdateManager : AppUpdateManager  by lazy {
        AppUpdateManagerFactory.create(this)
    }
    private val contract: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissionViews()
    }
    private  val appUpdateLauncher : ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.versionCheck(true)
            viewModel.insertLog("Update success")
        } else {
            viewModel.insertLog("Update failed")
            Log.d(TAG, "Update failed")

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        setContentView(binding.root)
        splashScreen.setKeepOnScreenCondition{true}

        lifecycleScope.launch {
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.isVersionCheck.collect {
                        if (it) {
                            viewModel.whereLocation()
                        }
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.nextProcess.collect {
                        refreshPermissionViews()
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.whereActivity.collectLatest {
                        when (it) {
                            WHERE.None, WHERE.Policy -> {}
                            WHERE.Login -> {

                                startActivity(Intent(this@SplashActivity, LoginActivity::class.java).addFlag())
                            }

                            WHERE.Main -> {
                                startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
                                    putExtra("data",intent.getIntExtra("data", -1))
                                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                })
                                finish()
                            }
                        }
                    }
                }
            }

        }
    }

    private  fun appUpdateCheck() {
        val appUpdateInfo = appUpdateManager.appUpdateInfo
        appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                showAlertDialogWithCancel(message =  "숨이랑  새로운 버전이 출시  되었어요!\n업데이트 를 진행해 주세요.", confirmAction = {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        appUpdateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }, confirmButtonText = R.string.common_update, cancelButtonText = R.string.common_finish, cancelAction = {
                    finish()
                })

            }else if(info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS){
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    appUpdateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
        appUpdateInfo.addOnFailureListener { e ->
            Log.e(TAG, "addOnFailureListener: ${e.message}",)
        }
        appUpdateInfo.addOnCompleteListener {command  ->
            viewModel.versionCheck(true)
            command.exception?.let {
//                viewModel.insertLog("${it}")
                Log.e(TAG, "addOnCompleteListener: $it")
            }
            }
    }
    private fun refreshPermissionViews() {
        val deniedPermissions = getPermissionResult()

        if (deniedPermissions.isNotEmpty()) {
            requestPermissions(deniedPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)

            if (!isIgnoringBatteryOptimizations()) {
                deniedPermissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            }
        } else if (!isIgnoringBatteryOptimizations()) {
            checkBatteryOptimization()
        } else {
            appUpdateCheck()
//            viewModel.whereLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val hasDeniedPermission = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                if (hasDeniedPermission) {
                    val twiceDenied = grantResults.filterIndexed { index, it -> it == PackageManager.PERMISSION_DENIED && !shouldShowRequestPermissionRationale(permissions[index]) }.isNotEmpty()
                    if (twiceDenied) {
                        showAlertDialogWithCancel(
                            title = R.string.permission_confirm,
                            message = getString(R.string.permission_rationale_msg),
                            confirmAction = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", packageName, null)
                                contract.launch(intent)
                            }
                        )
                    }
                } else {
                    if (!isIgnoringBatteryOptimizations()) {
                        checkBatteryOptimization()
                    } else {
                        appUpdateCheck()
//                        viewModel.whereLocation()
                    }
                }
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
        contract.launch(intent)
    }
}