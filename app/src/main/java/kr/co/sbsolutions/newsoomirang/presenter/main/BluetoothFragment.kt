package kr.co.sbsolutions.newsoomirang.presenter.main

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.DialogConnectInfoBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel

abstract class BluetoothFragment : Fragment() {
    abstract val  viewModel: BaseServiceViewModel

    private val connectInfoBinding: DialogConnectInfoBinding by lazy {
        DialogConnectInfoBinding.inflate(layoutInflater)
    }


    private val connectInfoDialog by lazy {
        BottomSheetDialog(requireContext()).apply {
            setContentView(connectInfoBinding.root, null)
            connectInfoBinding.btConnect.setOnClickListener {
                viewModel.connectClick()
                this.dismiss()
            }
            connectInfoBinding.btLater.setOnClickListener {
                this.dismiss()
            }
        }
    }
     fun showConnectDialog() {
        if (connectInfoDialog.isShowing) {
            connectInfoDialog.dismiss()
        }
        connectInfoDialog.show()
    }
      fun getBluetoothState(state: String) : BluetoothState {
        return when(state){
            "시작" -> BluetoothState.Connected
            "연결" -> BluetoothState.Disconnected
            else -> BluetoothState.Reconnected
        }
    }


    abstract  fun setBluetoothStateIcon(bluetoothState : BluetoothState)
    abstract fun setBatteryInfo(batteryInfo: String)
}
enum class BluetoothState {
    Connected, Reconnected, Disconnected;
    fun getImage() : Int{
        return when(this){
            Connected -> R.drawable.bluetooth_connected
            Reconnected -> R.drawable.bluetooth_searching
            Disconnected -> R.drawable.bluetooth_disabled
        }
    }
    fun  getText() : String{
        return when(this){
            Connected -> "연결됨"
            Reconnected -> "재 연결중"
            Disconnected -> "연결 안됨"
        }
    }
}