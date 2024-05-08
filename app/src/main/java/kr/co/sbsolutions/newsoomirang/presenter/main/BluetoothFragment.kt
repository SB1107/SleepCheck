package kr.co.sbsolutions.newsoomirang.presenter.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.databinding.DialogConnectInfoBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel

abstract class BluetoothFragment : Fragment() {
    abstract val  viewModel: BaseServiceViewModel

    private val connectInfoBinding: DialogConnectInfoBinding by lazy {
        DialogConnectInfoBinding.inflate(layoutInflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (viewModel as? BaseViewModel)?.let { baseViewModel ->
            (requireActivity() as? MainActivity)?.let { mainActivity ->
                baseViewModel.setLogHelper(mainActivity.logHelper)
            }
        }
        
    }

    private val connectInfoDialog by lazy {
        BottomSheetDialog(requireContext()).apply {
            setContentView(connectInfoBinding.root, null)
            connectInfoBinding.btConnect.setOnSingleClickListener {
                viewModel.connectClick()
                this.dismiss()
              
            }
            connectInfoBinding.btLater.setOnSingleClickListener {
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
            "연결", "연결 끊김" -> BluetoothState.Disconnected
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
    fun  getText(context : Context) : String {
        return when(this) {
            Connected ->  context.getString(R.string.connected)
            Reconnected -> context.getString(R.string.reconnected)
            Disconnected -> context.getString(R.string.disconnected)
        }
    }

    fun getStartButtonText(context : Context) : String {
        return when(this) {
            Connected -> context.getString(R.string.start)
            Reconnected -> context.getString(R.string.start)
            Disconnected ->  context.getString(R.string.connect)
        }

    }
}