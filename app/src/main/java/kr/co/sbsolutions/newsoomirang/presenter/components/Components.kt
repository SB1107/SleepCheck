package kr.co.sbsolutions.newsoomirang.presenter.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R


object Components {

    @Composable
    fun LottieLoading() {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ic_loading))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.color_80000000)), contentAlignment = Alignment.Center
        ) {
            LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowAlertDialog(
        isShow: Boolean,
        onDismiss: (() -> Unit)? = null,
        onConfirmation: () -> Unit,
        dialogTitle: String,
        dialogText: String,
        confirmText : String = "확인",
        dissmissText : String = "취소"
    ) {
        var openAlertDialog by remember { mutableStateOf(isShow) }
        when {
            openAlertDialog -> {
                AlertDialog(
                    title = {
                        Text(text = dialogTitle)
                    },
                    text = {
                        Text(text = dialogText)
                    },
                    onDismissRequest = {
                        onDismiss?.let {
                            run {
                                openAlertDialog = false
                                it()
                            }
                        }
                    },

                    confirmButton = {
                        TextButton(
                            onClick = {
                                openAlertDialog = false
                                onConfirmation()
                            }
                        ) {
                            Text(confirmText)
                        }
                    },
                    dismissButton = {
                        onDismiss?.let {
                            TextButton(
                                onClick = {
                                    openAlertDialog = false
                                    it()
                                }
                            ) {
                                Text(dissmissText)
                            }
                        }
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ScrollToView(isVisiable: Boolean ,scrollableState: ScrollState){
        val coroutineScope = rememberCoroutineScope()
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            if (isVisiable) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        if (scrollableState is LazyListState){
                            (scrollableState as LazyListState).animateScrollToItem(0)
                        } else{
                            scrollableState.animateScrollTo(0)
                        }
                    }
                }) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_scroll_up),
                        contentDescription = "스크롤"
                    )
                }
            }
        }
    }
}