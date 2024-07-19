package kr.co.sbsolutions.sleepcheck.presenter.sensor

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import com.skydoves.balloon.overlay.BalloonOverlayRoundRect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.getChangeDeviceName
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.databinding.ActivitySensorBinding
import kr.co.sbsolutions.sleepcheck.databinding.DialogConnectDeviceBinding
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import kr.co.sbsolutions.sleepcheck.presenter.BluetoothActivity
import kr.co.sbsolutions.sleepcheck.presenter.firmware.FirmwareUpdateActivity
import java.util.Timer
import kotlin.concurrent.timerTask


@SuppressLint("MissingPermission")
@AndroidEntryPoint
class SensorActivity : BluetoothActivity() {
    private var timer: Timer? = null
    private val connectDeviceBinding: DialogConnectDeviceBinding by lazy {
        DialogConnectDeviceBinding.inflate(layoutInflater)
    }

    private val connectDeviceDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(connectDeviceBinding.root, null)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isFitToContents = true
        }
    }

    override fun newBackPressed() {
        finish()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                newBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }


    private val viewModel: SensorViewModel by viewModels()

    private val binding: ActivitySensorBinding by lazy {
        ActivitySensorBinding.inflate(layoutInflater)
    }
    private val bleAdapter: SensorBluetoothAdapter by lazy {
        SensorBluetoothAdapter(bleClickListener)
    }


    private val bleClickListener: (BluetoothDevice) -> Unit = { bluetoothDevice ->
        lifecycleScope.launch {
            viewModel.registerDevice(bluetoothDevice)
        }
    }

    private val animator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.btSearch, "rotation", 0f, 0f).apply {
            duration = 1000L
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bindViews()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDeviceScan()
    }


    @SuppressLint("SetTextI18n")
    private fun setConnectDeviceDialog(device: BluetoothDevice) {
        connectDeviceBinding.tvBleInfoText.text = getString(R.string.sensor_ask_connect_message, device.name.getChangeDeviceName())
        connectDeviceBinding.btConnect.setOnSingleClickListener {
            bleClickListener.invoke(device)
        }

        timer = Timer().apply {
            schedule(timerTask {
                lifecycleScope.launch(Dispatchers.Main) {
                    connectDeviceDialog.show()
                }
            }, 1000L)
        }
    }

    private fun bindViews() {
        with(binding) {
            deviceRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@SensorActivity, LinearLayoutManager.VERTICAL, false)
                adapter = bleAdapter
            }

            actionBar.appBar.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            actionBar.toolbarTitle.text = getString(R.string.sensor_registered_device)

            actionBar.backButton.setOnSingleClickListener {
                newBackPressed()
            }
            btDiss.setOnSingleClickListener {
                viewModel.bleDisconnect()
            }
            binding.btSearch.setOnSingleClickListener {
                viewModel.bleConnect()
            }

            /*disconnectButton.setOnSingleClickListener {
                btSearch.visibility = View.VISIBLE
                disconnectButton.visibility = View.GONE
                deviceNameTextView.text = "연결된 기기가 없습니다."
            }*/
        }

        with(viewModel) {
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    launch {
                        viewModel.bleName.collectLatest { text ->
                            text?.let {
                                binding.deviceNameTextView.text = text.getChangeDeviceName()
                                binding.btDiss.visibility = View.VISIBLE
                                binding.deviceSelectMessage.visibility = View.GONE
                            } ?: run {
                                binding.deviceNameTextView.text = getString(R.string.sensor_unregister_info_message)
                                binding.deviceSelectMessage.visibility = View.VISIBLE
                                binding.btDiss.visibility = View.GONE
                            }
                        }
                    }

                    launch {
                        viewModel.isScanning.collectLatest {
                            it?.let {
                                if (it) {
                                    Toast.makeText(this@SensorActivity, getString(R.string.sensor_scanning), Toast.LENGTH_SHORT).show()
//                                    showToolTip()
                                    return@collectLatest
                                }
                            }
                        }
                    }

                    launch {
                        viewModel.isBleProgressBar.collectLatest {
                            binding.icBleProgress.clProgress.visibility = if (it) View.GONE else View.VISIBLE
                            if (it) {
                                connectDeviceDialog.dismiss()
                                newBackPressed()
                            }

                        }
                    }
                    launch {
                        viewModel.updateCheckResult.collectLatest {
                            when (it) {
                                true -> {
                                    binding.icBleProgress.clProgress.visibility = View.GONE
                                    showAlertDialog(message = "숨이랑 펌웨어\n업데이트가 필요합니다.\n\n업데이트 화면으로\n 이동합니다.", confirmAction = {
                                        startActivity(Intent(this@SensorActivity, FirmwareUpdateActivity::class.java))
                                        connectDeviceDialog.dismiss()
                                        newBackPressed()
                                    })
                                }
                                false -> {}
                            }
                        }
                    }
                    launch {
                        viewModel.bleStateResultText.collectLatest {
                            it?.let { resultText ->
                                binding.icBleProgress.tvDeviceId.text = resultText.getChangeDeviceName()
                            }
                        }
                    }

                    launch {
                        scanList.filter { it.isNotEmpty() }.map { list ->
                            list.filter { it.name.isNullOrEmpty().not() }
                            .filter {
                                it.name.uppercase().startsWith("AA")
                                        || it.name.uppercase().startsWith("AB")
                                        || it.name.uppercase().startsWith("AC")
                                        || it.name.uppercase().startsWith("AP")
                                        || it.name.uppercase().startsWith("BR")
                                        || it.name.uppercase().startsWith("BS")
                            }.sortedBy { it.name }
                        }.collectLatest { list ->
                            if (list.size == 1) {
                                setConnectDeviceDialog(list.first())

                            } else {
                                timer?.cancel()
                                connectDeviceDialog.dismiss()
                                bleAdapter.submitList(list)
                            }
                        }
//                        scanList.collectLatest { list ->
//                            bleAdapter.submitList(list)
//                        }
                    }

                    launch {
                        viewModel.errorMessage.collectLatest {
                            showAlertDialog(R.string.common_title, it)
                        }
                    }
                    launch {
                        viewModel.checkSensorResult.collectLatest {
                            it?.let {
                                showAlertDialog(message = it)
                            }
                        }
                    }
                }
            }
        }
    }

}