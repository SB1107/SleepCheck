package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.common.toHourOrMinute
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.databinding.ActivityHistoryDetailBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components
import kr.co.sbsolutions.newsoomirang.presenter.components.capture.ScreenCapture
import kr.co.sbsolutions.newsoomirang.presenter.components.capture.ScreenCaptureOptions
import kr.co.sbsolutions.newsoomirang.presenter.components.capture.rememberScreenCaptureState
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HistoryDetailActivity : BaseActivity() {
    private val viewModel: HistoryDetailViewModel by viewModels()
    private val binding: ActivityHistoryDetailBinding by lazy {
        ActivityHistoryDetailBinding.inflate(layoutInflater)
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
            setContent {
                RootView(SleepDetailResult())
            }
        }
        intent?.let {
            it.getStringExtra("id")?.let { id ->
                viewModel.getSleepData(id)
            }
        }
        setObservers()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RootView(data: SleepDetailResult = SleepDetailResult()) {
        val state = rememberScreenCaptureState()
        val localView = LocalView.current
        ScreenCapture(screenCaptureState = state) {
            ContentView(data, true)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg2),
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
                            text = "결과",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_left),
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            state.capture(options = ScreenCaptureOptions(height = localView.measuredHeight * 4))
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = "공유하기",
                                tint = Color.White
                            )
                        }
                    }
                )
                state.bitmap?.let {
                    viewModel.sharingImage(this@HistoryDetailActivity, it)
                }
                ContentView(data)
            }
        }
    }

    @Composable
    private fun ContentView(data: SleepDetailResult, isBack: Boolean = false) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                if (isBack) {
                    Box(modifier = Modifier.background(color = colorResource(id = R.color.color_purple))) {
                        Image(
                            painter = painterResource(id = R.drawable.bg2),
                            contentDescription = "배경",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        TopDateView(data = data)
                    }
                } else {
                    TopDateView(data = data)
                }
            }
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                if (scrollState.value >= 200) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    }) {
                        Image(painter = painterResource(id = R.drawable.ic_scroll_up), contentDescription = "스크롤")
                    }
                }
            }
        }

    }


    @Composable
    private fun TopDateView(data: SleepDetailResult = SleepDetailResult()) {
        val endedAt = data.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val titleDate = endedAt?.toDayString("M월 d일 E요일")
        val startAt = data.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val durationString =
            (startAt?.toDayString("HH:mm") + " ~ " + (endedAt?.toDayString("HH:mm"))).plus(" ").plus(if (data.type == 0) "수면" else "코골이")
        val milliseconds: Long = (endedAt?.time ?: 0) - (startAt?.time ?: 0)
        val totalTime = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60)
        val min = totalTime.toHourMinute()

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(21.dp),
                    painter = painterResource(id = if (data.type == 0) R.drawable.ic_br else R.drawable.ic_sn),
                    contentDescription = "측정 타입",
                    alignment = Alignment.Center
                )
                Text(
                    text = titleDate ?: "", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (durationString.contains("null").not()) {
                Text(
                    text = durationString, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
            }


            data.apneaCount?.let {
                HeaderTitleView("수면 중  비정상 호흡 수")
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(it.toFloat(), startText = "0", centerText = "50", endText = "100+")

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView("비정상 호흡수")
                Spacer(modifier = Modifier.height(32.dp))
                BreathingGraphView(
                    "이상호흡", "총${it}회", listOf(
                        Triple("이상호흡 10초", "${data.apnea10}회", colorResource(id = R.color.color_gray1)),
                        Triple("이상호흡 30초", "${data.apnea30}회", colorResource(id = R.color.color_gray2)),
                        Triple("이상호흡 60초", "${data.apnea60}회", colorResource(id = R.color.color_gray2))
                    )
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.White)

            RowTexts("총 수면시간", min)
            data.asleepTime?.let {
                RowTexts("잠들때까지 걸린 시간", it.toHourMinute())
            }
            data.snoreTime?.let {
                RowTexts("코고는 시간", it.toHourMinute())
            }
            data.deepSleepTime?.let {
                RowTexts("깊은잠 시간", it.toHourMinute())
            }
            data.moveCount?.let {
                RowTexts("뒤척임 횟수", it.toHourMinute())
            }
            data.straightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HeaderTitleView("수면 자세")
                VerticalGraphView(percentValue = (data.straightPer ?: 0).toFloat(), startText = "바른자세", startTextSize = 19.sp, endText = it.toHourMinute(), endTextSize = 19.sp)
            }
            data.leftPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(percentValue = (data.leftPer ?: 0).toFloat(), startText = "왼쪽으로 누운 자세", startTextSize = 19.sp, endText = it.toHourMinute(), endTextSize = 19.sp)
            }
            data.rightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(percentValue = (data.rightPer ?: 0).toFloat(), startText = "오른쪽으로 누운 자세", startTextSize = 19.sp, endText = it.toHourMinute(), endTextSize = 19.sp)
            }
            data.downPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(percentValue = (data.downPer ?: 0).toFloat(), startText = "업드린 자세", startTextSize = 19.sp, endText = it.toHourMinute(), endTextSize = 19.sp)
            }
            data.wakePer?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(percentValue = (data.wakePer ?: 0).toFloat(), startText = "수면중 일어남", startTextSize = 19.sp, endText = it.toHourMinute(), endTextSize = 19.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (data.remSleepTime != null || data.lightSleepTime != null || data.deepSleepTime != null) {
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView("수면 깊이")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly

            ) {

                data.remSleepTime?.let {
                    BarChartView("램수면", totalTime, it)
                }
                data.lightSleepTime?.let {
                    BarChartView("얕은수면", totalTime, it)
                }
                data.deepSleepTime?.let {
                    BarChartView("깊은수면", totalTime, it)
                }
            }

        }
    }

    @Composable
    private fun RowTexts(startText: String, endText: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            Text(
                text = startText, color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = endText, color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun HeaderTitleView(title: String) {
        val emptyTextSize = if (title.length < 14) 11 else title.length
        val emptyText = " ".repeat(emptyTextSize)
        val startText = emptyText
        val endText = emptyText

        val tempString = if (title.length < 13) startText.plus(title).plus(endText) else title
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(id = R.color.color_dark_yellow))
                .padding(16.dp, 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tempString,
                color = Color.Black,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun BreathingGraphView(
        title: String,
        totalValue: String = "총-회",
        rightBoxValue: List<Triple<String, String, Color>> = emptyList()
    ) {
        var size by remember { mutableStateOf(IntSize(0, 0)) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (rightBoxValue.size >= 3) Alignment.CenterVertically else Alignment.Bottom
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title, color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    StrokeCircle(
                        modifier = Modifier
                            .size(108.dp)
                            .onGloballyPositioned { coordinates ->
                                size = coordinates.size
                            },
                        radius = 54.dp,
                        color = Color.White
                    )
                    Text(
                        text = totalValue, color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                rightBoxValue.mapIndexed { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(59.dp)
                            .background(shape = RoundedCornerShape(11.dp), color = Color.Transparent)
                            .border(width = 1.dp, color = colorResource(id = R.color.color_stroke_line), shape = RoundedCornerShape(11.dp))
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(text = value.first, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Normal)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(52.dp)
                                .background(color = value.third, shape = RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.second, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (index < rightBoxValue.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }


        }

    }

    @Composable
    fun StrokeCircle(
        modifier: Modifier = Modifier,
        radius: Dp,
        color: Color,
        strokeWidth: Dp = 1.dp
    ) {
        Canvas(modifier = modifier) {
            drawCircle(
                color = color,
                radius = radius.toPx() - strokeWidth.toPx() / 2,
                style = Stroke(strokeWidth.toPx())
            )
            drawCircle(
                color = color,
                radius = ((radius.toPx() - 14) - strokeWidth.toPx() / 2),
                style = Stroke(strokeWidth.toPx())
            )
        }
    }

    @Composable
    private fun VerticalGraphView(
        percentValue: Float,
        startText: String,
        startTextSize: TextUnit = 14.sp,
        centerText: String = "",
        centerTextSize: TextUnit = 14.sp,
        endText: String,
        endTextSize: TextUnit = 14.sp
    ) {
        var width by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Box(contentAlignment = Alignment.Center) {
                val percent = width * ((percentValue / 100f))
                Image(
                    modifier = Modifier.padding(start = percent),
                    painter = painterResource(id = getPercentImage(percentValue)), contentDescription = ""
                )

                Text(
                    modifier = Modifier
                        .padding(start = percent)
                        .offset(y = (-5).dp),
                    text = "${percentValue.toInt()}",
                    color = colorResource(id = R.color.md_grey_800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .padding(18.dp, 0.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorResource(id = R.color.white),
                                colorResource(id = R.color.color_gradient_center),
                                colorResource(id = R.color.color_gradient_end)
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
                    .onGloballyPositioned { coordinates ->
                        width = with(density) {
                            coordinates.size.width.toDp()
                        }
                    }
            ) {
                Spacer(modifier = Modifier.height(25.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier
                        .offset(x = (-5).dp)
                        .padding(top = 4.dp),
                    text = startText,
                    color = Color.White,
                    fontSize = startTextSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier
                        .offset(x = (5).dp)
                        .padding(top = 4.dp),
                    text = centerText, color = Color.White, fontSize = centerTextSize, fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier
                        .offset(x = (-10).dp)
                        .padding(top = 4.dp),
                    text = endText,
                    color = Color.White,
                    fontSize = endTextSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun BarChartView(titleText: String, totalTime: Int, time: Int) {
        var height by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        val percentValue = (time / totalTime.toFloat()) * 100f
        val percent = height * ((percentValue / 100f))
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(61.dp, 136.dp)
                    .border(width = 1.dp, color = colorResource(id = R.color.color_stroke_line), shape = RoundedCornerShape(15.dp))
                    .clip(RoundedCornerShape(15.dp))
                    .background(color = colorResource(id = R.color.color_gray2))
                    .onGloballyPositioned { coordinates ->
                        height = with(density) {
                            coordinates.size.height.toDp()
                        }
                    }, contentAlignment = Alignment.BottomCenter

            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(percent)
                        .border(width = 1.dp, color = colorResource(id = R.color.color_stroke_line), shape = RoundedCornerShape(15.dp))
                        .clip(RoundedCornerShape(15.dp))
                        .background(color = colorResource(id = R.color.color_gray3)), contentAlignment = Alignment.Center
                ) {
                }
                Text(
                    text = time.toHourOrMinute(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = titleText,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

        }

    }


    private fun getPercentImage(percent: Float): Int {
        return when {
            percent < 40 -> {
                R.drawable.ic_green_value
            }

            percent < 60 -> {
                R.drawable.ic_yallow_value
            }

            percent < 80 -> {
                R.drawable.ic_orange_value
            }

            else -> {
                R.drawable.ic_red_value
            }
        }
    }


    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
//                        Log.e(TAG, "isProgressBar: ${it}")
                        binding.actionProgress.clProgress.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.sleepDataDetailData.collectLatest {
//                        setSleepDataDetailData(it)
                        binding.composeView.apply {
                            setContent {
                                RootView(it)
                            }
                        }
                    }
                }
            }
        }
    }
}
