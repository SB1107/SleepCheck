package kr.co.sbsolutions.sleepcheck.presenter.components

import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import kr.co.sbsolutions.sleepcheck.common.InpuMintoHourMinute
import kr.co.sbsolutions.sleepcheck.common.toDate
import kr.co.sbsolutions.sleepcheck.common.toDayString
import kr.co.sbsolutions.sleepcheck.common.toHourOrMinute
import kr.co.sbsolutions.sleepcheck.data.model.SleepDetailDTO
import kr.co.sbsolutions.sleepcheck.presenter.main.history.detail.HistoryDetailViewModel
import kotlin.math.cos
import kotlin.math.sin


object Components {

    @Composable
    fun LottieLoading(modifier: Modifier) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.question_new))
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                contentScale = ContentScale.FillBounds
            )
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
    fun SoomDetailText(
        text: String,
        textSize: Int,
        color: Color = Color.White,
        fontWeight: FontWeight
    ) {
        Text(
            text = text,
            style = TextStyle(color = color),
            fontSize = textSize.sp,
            fontWeight = fontWeight,
        )
    }

    @Composable
    fun SoomQuestionImage(sheet: Boolean = false) {
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
    fun SoomShowBottomSheet(
        showBottomSheet: Boolean = false,
        title: String,
        explanation: String? = null
    ) {
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
        bgImage: Int = R.drawable.back1, bgColor: Color? = null, topText: String = "결과",
        topAction: () -> Unit, row: @Composable RowScope.() -> Unit = {},
        childView: @Composable () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = bgImage),
                contentDescription = "배경",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            bgColor?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colorResource(id = R.color.color_back_gradient_start),
                                    colorResource(id = R.color.color_back_gradient_end)
                                ),
                            )
                        )
                )
            }
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
    fun SoomImage(viewModel: HistoryDetailViewModel) {
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
        closeSheet: (() -> Unit),
        message: String? = null
    ) {
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

    @Composable
    fun GraphsView(
        listData: List<String> = emptyList(),
        drawColors: List<Color> = listOf(
            Color.Transparent,
            Color.Green,
            Color.Yellow,
            Color.Red,
            Color.Blue
        ),
        startText: String = "", endText: String = ""
    ) {
        Column {
            Box( // 캔버스를 감싸는 Box 추가
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp)) // Box에 라운딩 적용
                    .background(color = Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    val barWidth = size.width / (listData.size + 1) // 바 너비 계산
                    val barSpacing = barWidth / listData.size // 바 간격 계산
                    listData.mapIndexed { index, value ->
                        value.toIntOrNull()?.let {
                            drawRect(
                                color = drawColors[it],
                                topLeft = Offset(barWidth * (index) + barSpacing * index, 0f),
                                size = Size(barWidth - barSpacing, size.height)
                            )

                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = startText, color = Color.White)
                Text(text = endText, color = Color.White)
            }
        }
    }


    @Composable
    fun PositionComposable(data: SleepDetailDTO) {
        Column {
            val positionList: MutableMap<String, Pair<Int, Color>> = mutableMapOf()
            data.straightPer?.let {
                positionList[stringResource(R.string.detail_supine)] =
                    Pair(it, colorResource(id = R.color.color_1DAEFF))
                Spacer(modifier = Modifier.height(16.dp))
            }
            data.leftPer?.let {
                positionList[stringResource(R.string.detail_left)] =
                    Pair(it, colorResource(id = R.color.color_9ACF40))
            }
            data.rightPer?.let {
                positionList[stringResource(R.string.detail_right)] =
                    Pair(it, colorResource(id = R.color.color_FF6008))
            }
            data.downPer?.let {
                positionList[stringResource(R.string.detail_prone)] =
                    Pair(it, colorResource(id = R.color.color_FDABFF))
            }
            data.wakePer?.let {
                positionList[stringResource(R.string.detail_standup)] =
                    Pair(it, colorResource(id = R.color.color_main))
            }
            if (positionList.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(201.dp)
                ) {
                    PieChart(positionList)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            data.straightPositionTime?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_1DAEFF),
                    stringResource(R.string.detail_supine),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            data.leftPositionTime?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_9ACF40),
                    stringResource(R.string.detail_left),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            data.rightPositionTime?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_FF6008),
                    stringResource(R.string.detail_right),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            data.downPositionTime?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_FDABFF),
                    stringResource(R.string.detail_prone),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            data.wakeTime?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_main),
                    stringResource(R.string.detail_standup),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            val startAt = data.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")?.toDayString("HH:mm") ?: ""
            val endedAt = data.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")?.toDayString("HH:mm") ?: ""
            var isBottomView = false
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                if (data.supineIdx.isNotEmpty()) {
                    LineView(
                        stringResource(R.string.detail_supine),
                        data.supineIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_1DAEFF),
                            colorResource(id = R.color.color_1DAEFF)
                        )
                    )
                    isBottomView = true
                }
                if (data.leftIdx.isNotEmpty()) {
                    LineView(
                        "왼쪽 자세",
                        data.leftIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_9ACF40),
                            colorResource(id = R.color.color_9ACF40)
                        )
                    )
                    isBottomView = true
                }
                if (data.rightIdx.isNotEmpty()) {
                    LineView(
                        "오른쪽 자세",
                        data.rightIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_FF6008),
                            colorResource(id = R.color.color_FF6008)
                        )
                    )
                    isBottomView = true
                }
                if (data.proneIdx.isNotEmpty()) {
                    LineView(
                        stringResource(R.string.detail_prone),
                        data.proneIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_FDABFF),
                            colorResource(id = R.color.color_FDABFF)
                        ),
                        isLast = data.wakeTime == null
                    )
                    isBottomView = true
                }
                if (data.wakeTime != null) {
                    LineView(
                        stringResource(R.string.detail_standup),
                        data.unstableIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_main),
                            colorResource(id = R.color.color_main)
                        ),
                        isLast = true
                    )
                    isBottomView = true
                }
                if (isBottomView) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BottomText(modifier = Modifier
                        .padding(start = 50.dp)
                        .fillMaxWidth() ,startAt, endedAt)
                }
            }
        }
    }

    @Composable
    fun IconRowTexts(color: Color, startText: String, endText: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            RowTexts(startText = startText, endText = endText, textColor = Color.White)
        }
    }

    @Composable
    fun RowTexts(startText: String, endText: String, textColor: Color = Color.Black) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            Text(
                text = startText, color = textColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = endText, color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }


    @Composable
    fun PieChart(data: Map<String, Pair<Int, Color>>) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = 0f
            var total = 0f
            data.forEach { (_, value) ->
                total += value.first
            }
            val tempValue = if (total != 100f) {
                100f - total
            } else 0
            val tempData: MutableMap<String, Pair<Int, Color>> = data.toMap().toMutableMap()
            if (tempValue != 0) {
                tempData["other"] = Pair(tempValue.toInt(), Color.White)
            }
            val outerRadius = size.minDimension / 2
            val innerRadius = outerRadius / 2 // 안쪽 원의 반지름을 바깥쪽 원의 절반으로 설정
            val drawingRadius = outerRadius * 0.8f // drawArc 크기 조절 (80%로 설정)

            drawCircle(
                color = Color(0xFF878787),
                radius = (innerRadius * 2) + 5,
            )

            drawCircle(
                color = Color(0xFFC3C3C3),
                radius = (innerRadius * 2),
                center = center
            )

            tempData.filter { it.value.first != 0 }.forEach { (label, value) ->
                val sweepAngle = (value.first / 100f) * 360f
                val angle = startAngle + sweepAngle / 2 // 라벨을 표시할 각도 계산
                drawArc(
                    color = value.second,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(drawingRadius * 2, drawingRadius * 2), // 바깥쪽 원의 크기 설정
                    topLeft = Offset(
                        (size.width - drawingRadius * 2) / 2,
                        (size.height - drawingRadius * 2) / 2
                    ),
                )

                // 라벨 그리기
                val labelRadius = ((drawingRadius + innerRadius) / 2)  // 라벨을 표시할 반지름계산
                val x =
                    center.x + labelRadius * cos(Math.toRadians(angle.toDouble()))
                        .toFloat()
                val y =
                    center.y + labelRadius * sin(Math.toRadians(angle.toDouble()))
                        .toFloat()
                if (value.second != Color.White) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${value.first}%",
                        x,
                        y,
                        Paint().apply {
                            textSize = 12.sp.toPx()
                            color = Color.Black.toArgb()
                            textAlign = Paint.Align.CENTER
                        }
                    )
                }
                startAngle += sweepAngle
            }

            // 안쪽 원 그리기
            drawCircle(
                color = Color(0xFFC3C3C3),
                radius = innerRadius,
                center = center
            )
        }
    }

    @Composable
    fun BottomText(modifier : Modifier = Modifier, startText: String = "12:00", endText: String = "30:00") {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = startText, color = Color.White)
            Text(text = endText, color = Color.White)
        }
    }


    @Composable
    fun LineView(
        lable: String = "바른자세",
        listData: List<String> = mutableListOf(
            "0",
            "1",
            "0",
            "1",
            "0",
            "1",
            "0",
            "1",
            "0",
            "1",
            "0",
            "1"
        ),
        drawColors: List<Color> = listOf(
            Color.Transparent,
            Color.Green,
            Color.Yellow,
            Color.Red,
            Color.Blue
        ), isLast: Boolean = false
    ) {
        var width by remember { mutableStateOf(0.dp) }
        var height by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = lable,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(60.dp),
                maxLines = 2
            )
            Column(modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    width = with(density) {
                        coordinates.size.width.toDp()
                    }
                    height = with(density) {
                        coordinates.size.height.toDp()
                    }
                }) {
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                Canvas(
                    modifier = Modifier
                        .height(44.dp)
                ) {
                    val oneSize = width / listData.size
                    var currentSize = oneSize
                    var currentIndex = 0
                    var previousColorIndex: Int? = null
                    while (currentIndex < listData.size) {
                        val value = listData[currentIndex].toIntOrNull()
                        if (value != null && value != 0) { // 값이 0이 아닌 경우
                            previousColorIndex = value
                            currentSize += oneSize
                        } else if (previousColorIndex != null) { // 값이 0이고 이전에 저장된 값이 있는 경우
                            val color = drawColors.getOrElse(previousColorIndex) { Color.Transparent }
                            drawRect(
                                color = color,
                                topLeft = Offset(oneSize.toPx() * (currentIndex - currentSize / oneSize), 0f),
                                size = Size(currentSize.toPx(), (height.toPx() -1))
                            )
                            currentSize = 0.dp
                            previousColorIndex = null
                        }
                        currentIndex++
                    }
                    // 마지막 값까지 처리
                    if (previousColorIndex != null) {
                        val color = drawColors.getOrElse(previousColorIndex) { Color.Transparent }
                        drawRect(
                            color = color,
                            topLeft = Offset(oneSize.toPx() * (currentIndex - currentSize / oneSize), 0f),
                            size = Size(currentSize.toPx(), (height.toPx() -1))
                        )
                    }
//                    listData.mapIndexed { index, value ->value.toIntOrNull()?.let { colorIndex ->
//                        val color = drawColors.getOrElse(colorIndex) { Color.Gray }
//                        val nextValue = listData.getOrNull(index + 1)?.toIntOrNull()
//                        val size = if (nextValue == 1) { // 다음 값이 1인 경우
//                            Size(oneSize.toPx() * 2, height.toPx()) // 너비를 두 배로 설정
//                        } else {
//                            Size(oneSize.toPx(), height.toPx())
//                        }
//                        drawRect(
//                            color = color,
//                            topLeft = Offset(oneSize * index, 0f),
//                            size = size
//                        )
//                    }
//                    }
                }
                if (isLast) {
                    HorizontalDivider(thickness = 1.dp, color = Color.White)
                }
            }
        }
    }

    @Composable
    fun SleepState(
        label: String,
        sleepTime: Int,
        totalTime: Int
    ) {
        var width by remember { mutableStateOf(0.dp) }
        var textWidth by remember { mutableStateOf(0.dp) }
        var height by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        val percentValue = (sleepTime / totalTime.toFloat()) * 100f
        val percent =
            if (percentValue.isNaN()) 0.dp else (width - textWidth) * ((percentValue / 100f))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    width = with(density) {
                        coordinates.size.width.toDp()
                    }
                    height = with(density) {
                        coordinates.size.height.toDp()
                    }
                },
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .width(percent)
                    .height(43.dp)
                    .clip(RoundedCornerShape(20.dp)) // Box에 라운딩 적용
                    .background(Color(0xff535353)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(percent)
                        .padding(10.dp)
                        .height(43.dp)
                        .clip(RoundedCornerShape(20.dp)) // Box에 라운딩 적용
                        .background(colorResource(id = R.color.color_main)),
                    contentAlignment = Alignment.CenterStart
                ) {}
                Text(
                    text = "${percentValue.toInt()}%",
                    color = Color.Black,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
            Column(modifier = Modifier.onGloballyPositioned { coordinates ->
                textWidth = with(density) {
                    coordinates.size.width.toDp()
                }
            }, horizontalAlignment = Alignment.End) {
                Text(text = label, color = Color.White, fontSize = 15.sp)
//                                    text = sleepTime.toHourOrMinute(LocalConfiguration.current.locales[0]),
                Text(
                    text = sleepTime.toHourOrMinute(LocalConfiguration.current.locales[0]),
                    color = Color.White,
                    fontSize = 21.sp,
                )
            }
        }
    }
}

@Preview
@Composable
fun GradientBarChart(
    data: List<String> = emptyList(), threshold: Int = 5,
    gradientColor: List<Color> = listOf(Color.Red, Color.Yellow), defaultColor: Color = Color.Blue
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(100.dp)
    ) {
        val barWidth = size.width / (data.size + 1) // 바 너비 계산
        val barSpacing = barWidth / data.size // 바 간격 계산
        val maxValue = 20 //최대값
        data.forEachIndexed { index, value ->
            val barHeight = (value.toFloat() / maxValue) * size.height
            val topLeft = Offset(
                x = barWidth * (index) + barSpacing * index,
                y = size.height - barHeight
            )
            val barSize = Size(width = barWidth - barSpacing, height = barHeight)
            // 그라데이션 브러시 생성
            val brush = if (value.toInt() >= threshold) {
                Brush.verticalGradient(
                    colors = gradientColor, // 그라데이션 색상 설정
                    startY = size.height - barHeight, // 그라데이션 시작 위치
                    endY = size.height
                )
            } else {
                SolidColor(defaultColor) // 기본 색상
            }
            drawRect(
                brush = brush,
                topLeft = topLeft,
                size = barSize,
                style = Fill
            )
        }
    }
}

fun getColor(value: Float): Color {
    return when {
        value < 0.5 -> Color.Green
        value < 1.0 -> {
            // 그라데이션 색상 적용
            val fraction = (value - 0.5f) / 0.5f // 0.5와 1.0 사이의 비율 계산
            Color(
                red = (1.0f * fraction).coerceIn(0f, 1f), // 빨간색 조정
                green = 1.0f, // 초록색은 항상 1
                blue = 0f // 파란색은 0
            )
        }

        else -> Color.Red
    }
}




