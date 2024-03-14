package kr.co.sbsolutions.newsoomirang.presenter.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.presenter.components.capture.ScreenCaptureOptions


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

    @Composable
    fun ShowAlertDialog(
        isShow: Boolean,
        onDismiss: (() -> Unit)? = null,
        onConfirmation: () -> Unit,
        dialogTitle: String,
        dialogText: String,
        confirmText: String = "확인",
        dissmissText: String = "취소"
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

    @Composable
    fun ScrollToView(isVisiable: Boolean, scrollableState: ScrollableState) {
        val coroutineScope = rememberCoroutineScope()

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(visible = isVisiable , enter = fadeIn(), exit = fadeOut()){
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            when (scrollableState) {
                                is LazyListState -> scrollableState.animateScrollToItem(0)
                                is ScrollState -> scrollableState.animateScrollTo(0)
                            }
                        }
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_scroll_up),
                        contentDescription = "스크롤"
                    )
                }
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SoomScaffold(bgImage : Int = R.drawable.back1 , topText : String = "결과" ,
                     topAction : () -> Unit,  row  : @Composable RowScope.() -> Unit = {},
                     childView : @Composable () -> Unit){
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = bgImage),
                contentDescription = "배경",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(
                Modifier
                    .fillMaxSize()
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(id = android.R.color.transparent),
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            text = topText,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = topAction) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_left),
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    },
                    actions = row
                )
                childView()
            }
        }
    }

    @Composable
    fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
        val eventHandler = rememberUpdatedState(onEvent)
        val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

        DisposableEffect(lifecycleOwner.value) {
            val lifecycle = lifecycleOwner.value.lifecycle
            val observer = LifecycleEventObserver { owner, event ->
                eventHandler.value(owner, event)
            }

            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
    }
}