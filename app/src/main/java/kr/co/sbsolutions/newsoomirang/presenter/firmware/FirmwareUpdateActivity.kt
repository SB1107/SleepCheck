package kr.co.sbsolutions.newsoomirang.presenter.firmware

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.databinding.ActivityFirmwaveUpdateBinding
import kr.co.sbsolutions.newsoomirang.databinding.DialogConnectInfoBinding
import kr.co.sbsolutions.newsoomirang.databinding.DialogFirmwareUpdateBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BluetoothActivity
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.newsoomirang.service.DfuService
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper


@AndroidEntryPoint
class FirmwareUpdateActivity : BluetoothActivity() {
    private val viewModel: FirmwareUpdateViewModel by viewModels()
    private val binding: ActivityFirmwaveUpdateBinding by lazy {
        ActivityFirmwaveUpdateBinding.inflate(layoutInflater)
    }
    private val firmwareUpdateBinding: DialogFirmwareUpdateBinding by lazy {
        DialogFirmwareUpdateBinding.inflate(layoutInflater)
    }
    private val firmwareUpdateDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(firmwareUpdateBinding.root, null)
        }
    }

    override fun newBackPressed() {
        finish()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.composeView.apply {
            setContent { RootView() }
        }

        viewModel.getFirmwareVersion(cacheDir.path)
        setObservers()
    }

    @Preview
    @Composable
    fun RootView() {
        SoomScaffold(
            topAction = { finish() },
            topText = "숨이랑 소프트웨어 업데이트",
            bgImage = R.drawable.back1,
            childView = {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {

                    }
                }
            }
        )
    }


    @SuppressLint("SetTextI18n")
    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.checkFirmWaveVersion.collectLatest { firmware ->
                        binding.updateProgress.clProgress.visibility = if (firmware.isShow) View.VISIBLE else View.GONE
                        binding.updateProgress.tvCurrentVersion1.text = "현재 버전 : ${firmware.firmwareVersion}"
                        binding.updateProgress.tvCurrentVersion2.text = "업데이트 버전 : ${firmware.serverFirmwareVersion}"
                        if (firmware.isShow) {
                            firmware.dfuServiceInitiator?.let {
                                updateFirmware(it)
                            }
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
                        binding.actionProgress.clProgress.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.firmwareUpdate.collectLatest {
                        it?.let { data ->
                            if (firmwareUpdateDialog.isShowing) {
                                firmwareUpdateDialog.dismiss()
                            }
                            firmwareUpdateBinding.tvCurrentVersion.text ="현재 버전 v${data.firmwareVersion}"
                            firmwareUpdateBinding.tvUpdateVersion.text ="업데이트 버전 v${data.updateVersion}"
                            firmwareUpdateBinding.btDone.setOnSingleClickListener {
                                viewModel.firmwareUpdateCall(data)
                                firmwareUpdateDialog.dismiss()
                            }
                            firmwareUpdateDialog.show()
                        }
                    }
                }
            }
        }
    }

    private fun updateFirmware(dfuServiceInitiator: DfuServiceInitiator) {
        dfuServiceInitiator.start(this, DfuService::class.java)
        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener)
        DfuServiceInitiator.createDfuNotificationChannel(this)
    }


    override fun onDestroy() {
        DfuServiceListenerHelper.unregisterProgressListener(this@FirmwareUpdateActivity, dfuProgressListener)
        super.onDestroy()
    }

    private val dfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDfuProcessStarting(deviceAddress: String) {
            super.onDfuProcessStarting(deviceAddress)
        }

        override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)
            binding.updateProgress.lpProgress.setProgressCompat(percent, true)
        }

        override fun onDeviceDisconnected(deviceAddress: String) {
            super.onDeviceDisconnected(deviceAddress)
            lifecycleScope.launch {
                viewModel.deviceConnect().collectLatest {
                    delay(1000)
                    finish()
                    return@collectLatest
                }
            }
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            Log.d(TAG, "onError: $message")
            viewModel.sendErrorMessage(message.toString())
        }

        override fun onDfuCompleted(deviceAddress: String) {
            super.onDfuCompleted(deviceAddress)
            viewModel.sendFirmwareUpdate()
        }

    }
}