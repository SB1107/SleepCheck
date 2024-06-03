package kr.co.sbsolutions.sleepcheck.presenter

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.sleepcheck.service.BLEService
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.LogHelper
import kr.co.sbsolutions.sleepcheck.common.RequestHelper
import kr.co.sbsolutions.sleepcheck.presenter.splash.SplashActivity
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            newBackPressed()
        }
    }
    private val reAuthorizeCallBack = object : RequestHelper.ReAuthorizeCallBack {
        override fun reLogin() {
            endAndStartActivity(Intent(this@BaseActivity, SplashActivity::class.java))
        }
    }

    @Inject
    lateinit var  logHelper: LogHelper

    protected fun twiceBackPressed() {
        if (backPressedTime + 2000L > System.currentTimeMillis()) {
            finish()
        } else {
            Toast.makeText(this, R.string.app_finish_message, Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private var backPressedTime = 0L

    abstract fun newBackPressed()
    abstract fun injectViewModel(): BaseViewModel

    private fun progressCanceled() {
        _viewModel.cancelJob()
    }

    private val _viewModel: BaseViewModel by lazy {
        injectViewModel()
    }

    private val progressDialog: Dialog by lazy {
        Dialog(this).apply {
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(ProgressBar(this@BaseActivity))
            setOnCancelListener { progressCanceled() }
            setCanceledOnTouchOutside(false)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onBackPressedDispatcher.addCallback(this, callback)
        _viewModel.setReAuthorizeCallBack(reAuthorizeCallBack)
        _viewModel.setLogHelper(logHelper)
    }

    protected fun showProgress() {
        progressDialog.show()
    }

    protected fun hideProgress() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    protected fun endAndStartActivity(intent: Intent) {
        intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
    }

    protected fun isMyServiceRunning(): Boolean {
        val manager: ActivityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tradingService = manager.getRunningServices(Int.MAX_VALUE).firstOrNull {
            BLEService::class.java.name == it.service.className
        }
        return tradingService?.foreground ?: false
    }

    protected fun getViewModel(): BaseViewModel {
        return _viewModel
    }
}

interface resetActivityCallBack {
    fun reLoading()
}