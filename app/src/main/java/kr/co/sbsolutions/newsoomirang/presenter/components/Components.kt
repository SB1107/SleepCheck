package kr.co.sbsolutions.newsoomirang.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kr.co.sbsolutions.newsoomirang.R


object Components  {
    @Composable
    fun LottieLoading() {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ic_loading))
        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.color_80000000))
            , contentAlignment = Alignment.Center
        ){
            LottieAnimation(composition = composition,  iterations = LottieConstants.IterateForever,)

        }
    }

}