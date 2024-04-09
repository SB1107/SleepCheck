package kr.co.sbsolutions.newsoomirang.presenter.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.guideAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.databinding.ActivityMainBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowProgressResultBinding
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.history.detail.HistoryDetailActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : BaseServiceActivity() {

    private  var rootHeight : Int = 0

    override fun newBackPressed() {
        twiceBackPressed()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }

    private val viewModel: MainViewModel by viewModels()
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }
    private  val guideAlert : AlertDialog by lazy {
        guideAlertDialog()
    }
    private val appUpdateLauncher: ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.insertLog("Update success")
        } else {
            viewModel.insertLog("Update failed")
            Log.d(TAG, "Update failed")
        }
    }
    private val resultBinding: RowProgressResultBinding by lazy {
        RowProgressResultBinding.inflate(layoutInflater)
    }

    private val resultDialog by lazy {

        val image = resultBinding.root.findViewById<ImageView>(R.id.iv_image)
        image.layoutParams.height = rootHeight
        BottomSheetDialog(this@MainActivity).apply {
            setContentView(resultBinding.root)
            (resultBinding.root.parent as View).setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.transparent))
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            behavior.isFitToContents = false
            setCanceledOnTouchOutside(false)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Cons.NOTIFICATION_ACTION) {
                getBroadcastData()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        gotoFragment(intent)
        super.onNewIntent(intent)
    }

    private fun gotoFragment(intent: Intent?) {
        val value = intent?.getIntExtra("data", -1)
        if (value == 1) {
            binding.navBottomView.selectedItemId = R.id.navigation_no_sering
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        guideAlert.show()
        binding.root.viewTreeObserver.addOnGlobalLayoutListener { rootHeight = binding.root.height }
        val logTime = SimpleDateFormat("MM월 dd일 HH시 mm분 ss초", Locale.getDefault()).format(Date(System.currentTimeMillis()))
        logWorkerHelper.insertLog("[M] Model Name: " + Build.MODEL + "  Device Name: " + Build.DEVICE + " 시간 :" + logTime)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Cons.NOTIFICATION_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        binding.navBottomView.apply {
            setupWithNavController(fragment.navController)
            itemIconTintList = null
        }
        gotoFragment(intent)

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.serviceCommend.collectLatest {
                        when (it) {
                            ServiceCommend.START -> startSBService(ActionMessage.StartSBService)
                            ServiceCommend.STOP -> startSBService(ActionMessage.StopSBService)
                            ServiceCommend.CANCEL -> startSBService(ActionMessage.CancelSbService)
                        }
                    }
                }
                launch {
                    viewModel.guideAlert.collectLatest {
                        when(it){
                            true -> guideAlert.show()
                            false -> {
                                guideAlert.dismiss()
                            }
                        }
                    }
                }
                launch {
                    viewModel.isResultProgressBar.collectLatest {
                        Log.e(TAG, "onCreate: isResultProgressBar = ${it.first.toString() + " :" + it.second}")
                        /*binding.actionProgressResult.clProgress.visibility = if (it) View.VISIBLE else View.GONE*/
                        delay(500)
                        resultDialog.run {
                            if (it.second) show() else dismiss()
                        }
                        if (it.first != -1 ) {
                                startActivity(Intent(this@MainActivity, HistoryDetailActivity::class.java).apply {
                                    putExtra("id", it.first.toString())
                                })
                            viewModel.stopResultProgressBar()
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        viewModel.stopResultProgressBar()
                        showAlertDialog(message = it)
                    }
                }
                launch {
                    viewModel.dataFlowInfoMessage.collect {
                        binding.icBleProgress.apply {
                            tvDeviceId.text = "이전 데이터를 찾았습니다.\n 데이터 정리하고 있습니다.\n잠시만 기다려주세요."
                            root.visibility = if (it) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    fun getBroadcastData() {
        viewModel.sendMeasurementResults()
    }

    override fun onResume() {
        super.onResume()
        appUpdateCheck()
    }
    private  fun appUpdateCheck() {
        val appUpdateInfo = appUpdateManager.appUpdateInfo
        appUpdateInfo.addOnSuccessListener { info ->
            Log.e(TAG, "addOnSuccessListener: ${info.availableVersionCode()}")
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                showAlertDialogWithCancel(message =  "숨이랑  새로운 버전이 출시  되었어요!\n업데이트 를 진행해 주세요.", confirmAction = {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        appUpdateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
//                    val url = "https://play.google.com/store/apps/details?id=kr.co.sbsolutions.newsoomirang&pcampaignid=web_share"
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse(url)
//                    startActivity(intent)
//                    finish()
                }, confirmButtonText = R.string.common_update, cancelButtonText = R.string.common_next_update, cancelable = false)

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

    }

    override fun onDestroy() {
        super.onDestroy()
        guideAlert.dismiss()
        Log.d(TAG, "onDestroy: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    }
}