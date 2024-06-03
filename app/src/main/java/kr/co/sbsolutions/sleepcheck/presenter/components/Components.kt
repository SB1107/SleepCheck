package kr.co.sbsolutions.sleepcheck.presenter.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.presenter.main.history.detail.HistoryDetailViewModel


object Components {

    @Composable
    fun LottieLoading(modifier: Modifier) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.question_ani))
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
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
            AnimatedVisibility(visible = isVisiable, enter = fadeIn(), exit = fadeOut()) {
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

    @Composable
    fun ScrollToContactView(isVisiable: Boolean, view: @Composable () -> Unit) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = isVisiable, enter = fadeIn(), exit = fadeOut()) {
                view()
            }
        }
    }
    @Composable
    fun SoomDetailText(text: String, textSize: Int, color: Color = Color.White, fontWeight: FontWeight) {
        Text(
            text = text,
            style = TextStyle(color = color),
            fontSize = textSize.sp,
            fontWeight = fontWeight,
        )
    }
    
    @Composable
    fun SoomQuestionImage(sheet: Boolean = false){
        var showBottomSheet by remember { mutableStateOf(sheet) }
        Log.d(TAG, "SoomQuestionImage: $sheet")
        var ssheet: Boolean = sheet
        Image(
            modifier = Modifier.clickable { ssheet = true },
            painter = painterResource(id = R.drawable.question),
            contentDescription = ""
        )
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SoomShowBottomSheet(showBottomSheet: Boolean = false, title: String, explanation: String? = null){
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        
        var soomShowBottomSheet by remember { mutableStateOf(showBottomSheet) }
        
        Log.d(TAG, "SoomShowBottomSheet: $soomShowBottomSheet")
        explanation?.let { explanation ->
            
            if (soomShowBottomSheet) {
                ModalBottomSheet(
                    containerColor = colorResource(id = R.color.color_1A447D),
                    scrimColor = colorResource(id = R.color.color_78899F),
                    contentColor = colorResource(id = R.color.color_78899F),
                    onDismissRequest = {
                        soomShowBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 25.dp, end = 25.dp, bottom = 50.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .padding(16.dp, 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 10.dp)
                                    .align(Alignment.Start),
                                text = title,
                                fontSize = 40.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            
                            Text(
                                modifier = Modifier
                                    .padding(top = 10.dp, bottom = 25.dp),
                                text = explanation,
                                color = Color.White,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start
                            )
                            // Sheet content
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
                                
                                onClick = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            soomShowBottomSheet = false
                                        }
                                    }
                                },
                            ) {
                                SoomDetailText(
                                    text = "확인",
                                    textSize = 16,
                                    color = Color.Black,
                                    FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SoomScaffold(
        bgImage: Int = R.drawable.back1, topText: String = "결과",
        topAction: () -> Unit, row: @Composable RowScope.() -> Unit = {},
        childView: @Composable () -> Unit
    ) {
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
    fun SoomImage(viewModel: HistoryDetailViewModel){
        Image(
            modifier = Modifier.clickable { viewModel },
            painter = painterResource(id = R.drawable.question),
            contentDescription = ""
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SoomBottomSheet(
        modifier: Modifier = Modifier,
        closeSheet: ( () -> Unit),
        message: String? = null
    ){
        val sheetState = rememberModalBottomSheetState()
        
        ModalBottomSheet(
            onDismissRequest = closeSheet,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = colorResource(id = R.color.colorPrimaryDark),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                message?.let {
                    Text(
                        text = it,
                    )
                }
            }
            
        }
    }
    
    @Composable
    fun SoomDetailText(text: String, textSize: Int, color: Color = Color.White) {
        Text(
            text = text,
            style = TextStyle(color = color),
            fontSize = textSize.sp
        )
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