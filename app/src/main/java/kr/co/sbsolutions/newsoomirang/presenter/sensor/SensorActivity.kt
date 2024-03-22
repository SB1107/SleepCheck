package kr.co.sbsolutions.newsoomirang.presenter.sensor

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.databinding.ActivitySensorBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BluetoothActivity

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class SensorActivity : BluetoothActivity() {

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

    private fun bindViews() {
        with(binding) {
            deviceRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@SensorActivity, LinearLayoutManager.VERTICAL, false)
                adapter = bleAdapter
            }

            actionBar.backButton.setOnClickListener {
                newBackPressed()
            }

            /*disconnectButton.setOnClickListener {
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
                                binding.deviceNameTextView.text = text
                            }
                        }
                    }

                    launch {
                        viewModel.searchBtnName.collectLatest { text->
                            text?.let {
                                binding.btSearch.text = text
                            }
                        }
                    }

                    launch {
                        binding.btSearch.setOnClickListener {
                            viewModel.bleConnectOrDisconnect()
                        }
                    }

                    launch {
                        viewModel.isScanning.collectLatest {
                            it?.let {
                                if (it) {
                                    Toast.makeText(this@SensorActivity, "스캔중", Toast.LENGTH_SHORT).show()
                                    return@collectLatest
                                }
                            }
                        }
                    }

                    /*launch {
                        isScanning.collectLatest { isScanning ->
                            animator.also {
                                if (isScanning) it.start() else it.cancel()
                            }
                        }
                    }*/

                    /*launch {
                        isRegistered.collectLatest {
                            if (it) {
//                                delay(1000)
                                newBackPressed()

                            } else {
                                Toast.makeText(this@SensorActivity, "재연결이 필요합니다. ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }*/
                    launch {
                        viewModel.isBleProgressBar.collectLatest {
                            binding.icBleProgress.clProgress.visibility = if (it) View.GONE else View.VISIBLE
                            if (it) newBackPressed()

                        }
                    }

                    launch {
                        viewModel.bleStateResultText.collectLatest {
                            it?.let { resultText ->
                                binding.icBleProgress.tvDeviceId.text = resultText
                            }
                        }
                    }

                    launch {
                        scanList.collectLatest { list ->
                            bleAdapter.submitList(list)
                        }
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