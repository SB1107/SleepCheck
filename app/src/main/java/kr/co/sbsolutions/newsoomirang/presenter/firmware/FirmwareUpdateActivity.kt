package kr.co.sbsolutions.newsoomirang.presenter.firmware

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.ActivityFirmwaveUpdateBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BluetoothActivity
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.LottieLoading
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomDetailText
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold

@AndroidEntryPoint
class FirmwareUpdateActivity  : BluetoothActivity(){
    private val viewModel: FirmwareUpdateViewmodel by viewModels()
    private val binding: ActivityFirmwaveUpdateBinding by lazy {
        ActivityFirmwaveUpdateBinding.inflate(layoutInflater)
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