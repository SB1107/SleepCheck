package kr.co.sbsolutions.newsoomirang.presenter.firmware

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.databinding.ActivityFirmwaveUpdateBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BluetoothActivity
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomDetailText
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.newsoomirang.service.DfuService
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper


@AndroidEntryPoint
class FirmwareUpdateActivity  : BluetoothActivity(){
    private val viewModel: FirmwareUpdateViewmodel by viewModels()
    private val binding: ActivityFirmwaveUpdateBinding by lazy {
        ActivityFirmwaveUpdateBinding.inflate(layoutInflater)
    }
    private val dfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDfuProcessStarting(deviceAddress: String) {
            super.onDfuProcessStarting(deviceAddress)
        }
        
        override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)
            
            binding.actionProgress.lpProgress.setProgressCompat(percent,true)
        }
        
        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            Log.d(TAG, "onError: $message")
        }
        
        override fun onDfuCompleted(deviceAddress: String) {
            super.onDfuCompleted(deviceAddress)
            finish()
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
        
//        resources.assets.openNonAssetFd("soomwith_update.zip")
        val assetsUri = Uri.parse("file:///android_asset/soomwith_update.zip")
        Log.d(TAG, "onCreate: ${assetsUri.path}")
        
        val starter = DfuServiceInitiator("E9:DE:B5:7C:B3:2A")
            .setDeviceName("AAC-7CB5")
            .setKeepBond( true)
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
        starter.setPrepareDataObjectDelay(300L)
        starter.setZip(assetsUri)
        starter.start(this, DfuService::class.java)
        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener)
        DfuServiceInitiator.createDfuNotificationChannel(this)
        
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
    
    @Composable
    fun InsertButton() {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.color_yellow),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            
            onClick = {},
        ) {
            SoomDetailText(text = "다운로드 및 설치", textSize = 16, color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
}