package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.InpuMintoHourMinute
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.common.toHourOrMinute
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.databinding.ActivityHistoryDetailBinding
import kr.co.sbsolutions.newsoomirang.databinding.DialogInfoMassageBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.ScrollToView
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold
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
    
    private val infoDialogBinding: DialogInfoMassageBinding by lazy {
        DialogInfoMassageBinding.inflate(layoutInflater)
    }
    
    private val infoDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(infoDialogBinding.root, null)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isFitToContents = true
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

    @Preview
    @Composable
    fun RootView(data: SleepDetailResult = SleepDetailResult()) {
        val state = rememberScreenCaptureState()
        val scrollState = rememberScrollState()
        var contentHeightPx by remember { mutableStateOf(0) }
        val columnModifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .onGloballyPositioned { coordinates ->
                contentHeightPx = coordinates.size.height
            }
        ScreenCapture(screenCaptureState = state) {
            ContentView(data, true, columnModifier, scrollState = scrollState)
        }

        SoomScaffold(R.drawable.bg2, stringResource(R.string.detail_result), topAction = {
            finish()
        }, row = {
            IconButton(onClick = {
                state.capture(options = ScreenCaptureOptions(height = contentHeightPx))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = stringResource(R.string.detail_share),
                    tint = Color.White
                )
            }
        }, childView = {
            state.bitmap?.let {
                viewModel.sharingImage(this@HistoryDetailActivity, it)
            }
            ContentView(data, modifier = columnModifier, scrollState = scrollState)
        })
    }

    @Composable
    private fun ContentView(data: SleepDetailResult, isBack: Boolean = false, modifier: Modifier, scrollState: ScrollState = rememberScrollState()) {

        Box {
            Column(
                modifier = modifier
            ) {
                if (isBack) {
                    Box(modifier = Modifier.background(color = colorResource(id = R.color.color_purple))) {
                        Image(
                            painter = painterResource(id = R.drawable.bg2),
                            contentDescription = "배경",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        TopDateView(data = data, scrollState)
                    }
                } else {
                    TopDateView(data = data, scrollState)
                }
            }
            ScrollToView(scrollState.value >= 200, scrollState)
        }

    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopDateView(
        data: SleepDetailResult = SleepDetailResult(),
        scrollState: ScrollState
    ) {
        val endedAt = data.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val titleDate = if (LocalConfiguration.current.locales[0] == java.util.Locale.KOREA) {
            endedAt?.toDayString("M월 d일 E요일", LocalConfiguration.current.locales[0])
        } else {
            endedAt?.toDayString("MMM d EEEE", LocalConfiguration.current.locales[0])
        }
        val startAt = data.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val durationString =
            (startAt?.toDayString("HH:mm") + " ~ " + (endedAt?.toDayString("HH:mm"))).plus(" ")
                .plus(if (data.type == 0) stringResource(R.string.breating) else stringResource(R.string.nosering))
        val milliseconds: Long = (endedAt?.time ?: 0) - (startAt?.time ?: 0)
        val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()

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
                    text = titleDate ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (durationString.contains("null").not()) {
                Text(
                    text = durationString,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.White)
            
            RowTexts(startText = stringResource(R.string.detil_time), endText = min)
            data.sleepTime?.let {
                RowTexts(stringResource(R.string.detil_sleep_time), it.InpuMintoHourMinute())
            }
            data.asleepTime?.let {
                RowTexts(stringResource(R.string.detail_asleep), it.InpuMintoHourMinute())
            }
            data.snoreTime?.let {
                RowTexts(stringResource(R.string.detail_snoring), it.InpuMintoHourMinute())
            }
            data.deepSleepTime?.let {
                RowTexts(stringResource(R.string.detail_deep_sleep), it.InpuMintoHourMinute())
            }
            data.moveCount?.let {
                RowTexts(stringResource(R.string.detail_turns), stringResource(R.string.detail_times, it))
            }
            
            if (data.type == 1){
                data.snoreCount?.let {
                    RowTexts(stringResource(R.string.detail_vibration), stringResource(R.string.detail_times, it))
                }
                data.coughCount?.let {
                    RowTexts(stringResource(R.string.detil_cough), stringResource(R.string.detail_times, it))
                }
            }
            
            
            (if (data.type == 0) data.breathScore else data.snoreScore)?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                
                MainScoreGraphView(percentValue = it, type = data.type)
            }
            
            
            data.normalBreathTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                
                HeaderTitleView(stringResource(R.string.detial_normal_b), getString(R.string.detail_normal_breathing_text))
                RowTexts(stringResource(R.string.detial_normal_b_t), it.InpuMintoHourMinute())
            }
            data.avgNormalBreath?.let {
                RowTexts(stringResource(R.string.detail_average_b), if (it == 0) "-" else stringResource(R.string.detail_min, it))
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White)
            }
            
            data.apneaCount?.let {
                //todo 비정상 호흡 시간으로 변경 필요
                HeaderTitleView(stringResource(R.string.detail_a_b))
                /*data.unstableBreath?.let {
                    RowTexts("비정상 호흡 시간", it.InpuMintoHourMinute())
                }*/
                Spacer(modifier = Modifier.height(16.dp))
                BreathingGraphView(
                    stringResource(R.string.detail_no_signal), "총${it}회", listOf(
                        Triple(
                            "10초이상 호흡곤란",
                            "${data.apnea10 ?: 0}회",
                            colorResource(id = R.color.color_gray1)
                        ),
                        Triple(
                            "30초이상 호흡곤란",
                            "${data.apnea30 ?: 0}회",
                            colorResource(id = R.color.color_gray2)
                        ),
                        Triple(
                            "60초이상 호흡곤란",
                            "${data.apnea60 ?: 0}회",
                            colorResource(id = R.color.color_gray3)
                        )
                    )
                )
            }
            val lists: ArrayList<Triple<Pair<String,String>, Pair<String,String>, Color>> = ArrayList()
            data.fastBreath?.let { fastBreath ->
                data.avgFastBreath?.let { avgFastBreath ->
                    lists.add(Triple(Pair("빠른호흡","평균 호흡수 (분)"), Pair("${fastBreath}분","${avgFastBreath}회"),colorResource(id = R.color.color_gray1)))
                }
            }
            data.slowBreath?.let { slowBreath ->
                data.avgSlowBreath?.let { avgSlowBreath ->
                    lists.add(Triple(Pair("느린호흡","평균 호흡수 (분)"), Pair("${slowBreath}분","${avgSlowBreath}회"),colorResource(id = R.color.color_gray2)))
                }
            }
            if (lists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                val totalCount =
                    (data.fastBreath ?: 0) + (data.slowBreath ?: 0) + (data.unstableBreath ?: 0)
                RespiratoryInstabilityGraphView(title = "호흡불안정", "총${totalCount}분", lists)
            }

            Spacer(modifier = Modifier.height(32.dp))

            data.straightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView("수면 자세", getString(R.string.detail_sleep_position_text))
                Spacer(modifier = Modifier.height(16.dp))

                VerticalGraphView(
                    percentValue = (data.straightPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = "바른자세",
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(),
                    endTextSize = 19.sp
                )
            }
            data.leftPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.leftPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = "왼쪽으로 누운 자세",
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(),
                    endTextSize = 19.sp
                )
            }
            data.rightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.rightPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = "오른쪽으로 누운 자세",
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(),
                    endTextSize = 19.sp
                )
            }
            data.downPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.downPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = "엎드린 자세",
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(),
                    endTextSize = 19.sp
                )
            }
            data.wakeTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.wakePer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = "수면중 일어남",
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(),
                    endTextSize = 19.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (data.remSleepTime != null || data.lightSleepTime != null || data.deepSleepTime != null) {
                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView("수면 단계", getString(R.string.detail_sleep_stages_text))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly

            ) {

                data.remSleepTime?.let {
                    BarChartView("렘수면", data.sleepTime ?: 0, it, scrollState)
                }
                data.lightSleepTime?.let {
                    BarChartView("얕은수면", data.sleepTime ?: 0, it, scrollState)
                }
                data.deepSleepTime?.let {
                    BarChartView("깊은수면", data.sleepTime ?: 0, it, scrollState)
                }
            }
            Text(
                text = data.ment ?:"",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.description ?: "",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(16.dp))
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
    private fun MainScoreGraphView(
        type: Int? = null,
        percentValue: Int,
        isPercentText: Boolean = false,
        startText: String = "나쁨",
        startTextSize: TextUnit = 14.sp,
        centerText: String = "보통",
        centerTextSize: TextUnit = 14.sp,
        endText: String = "좋음",
        endTextSize: TextUnit = 14.sp
    ) {
        var width by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .padding(16.dp, 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (type == 0)"당신의 수면 중 호흡 점수" else "당신의 수면 중 코골이 점수",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Image(
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .clickable {
                                    if (type == 0) {
                                        viewModel.sendInfoMessage("호흡 점수", getString(R.string.detail_breathing_score_text))
                                    } else {
                                        viewModel.sendInfoMessage("코골이 점수", getString(R.string.detail_breathing_score_text))
                                    }
                                },
                                painter = painterResource(id = R.drawable.question),
                                contentDescription = ""
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$percentValue 점",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Box(contentAlignment = Alignment.Center) {
                val percent: Dp = if (percentValue < 0) 0.dp else width * ((percentValue / 100f))
                Image(
                    modifier = Modifier.padding(start = percent),
                    painter = painterResource(id = getReversPercentImage(percentValue.toFloat())),
                    contentDescription = ""
                )
                
                Text(
                    modifier = Modifier
                        .padding(start = percent)
                        .offset(y = (-5).dp),
                    text = "${percentValue.toInt()}${if (isPercentText) "%" else ""}",
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
                                colorResource(id = R.color.color_EB361B),
                                colorResource(id = R.color.color_FFF33A),
                                colorResource(id = R.color.color_44A64B)
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.color_FFFFFF),
                        shape = RoundedCornerShape(40.dp)
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
                    text = centerText,
                    color = Color.White,
                    fontSize = centerTextSize,
                    fontWeight = FontWeight.Bold
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
    private fun HeaderTitleView(title: String, detailText: String? = null) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp, start = 50.dp, end = 50.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(id = R.color.color_dark_yellow))
                .padding(16.dp, 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .align(Alignment.CenterVertically),
                    text = title,
                    color = Color.Black,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                detailText?.let {
                    Image(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clickable { viewModel.sendInfoMessage(title, it) },
                        painter = painterResource(id = R.drawable.question),
                        contentDescription = ""
                    )
                }
            }
            
            
            /*IconButton(onClick = {
                state.capture(options = ScreenCaptureOptions(height = contentHeightPx))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = "공유하기",
                    tint = Color.White
                )
            }*/
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
                modifier = Modifier.weight(4f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title, color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Image(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clickable { viewModel.sendInfoMessage(title, getString(R.string.detail_no_signal_breathing_text)) },
                        painter = painterResource(id = R.drawable.question),
                        contentDescription = ""
                    )
                }
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
                    .weight(6f)
                    .padding(16.dp, 0.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                rightBoxValue.mapIndexed { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(59.dp)
                            .background(
                                shape = RoundedCornerShape(11.dp),
                                color = Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = colorResource(id = R.color.color_stroke_line),
                                shape = RoundedCornerShape(11.dp)
                            )
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value.first,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            modifier =  Modifier.weight(7f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(52.dp)
                                .weight(3f)
                                .background(
                                    color = value.third,
                                    shape = RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
                                ), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.second,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
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
        isPercentText: Boolean = false,
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
                val percent: Dp = if (percentValue < 0) 0.dp else width * ((percentValue / 100f))
                Image(
                    modifier = Modifier.padding(start = percent),
                    painter = painterResource(id = getPercentImage(percentValue)),
                    contentDescription = ""
                )

                Text(
                    modifier = Modifier
                        .padding(start = percent)
                        .offset(y = (-5).dp),
                    text = "${percentValue.toInt()}${if (isPercentText) "%" else ""}",
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
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.color_FFFFFF),
                        shape = RoundedCornerShape(40.dp)
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
                    text = centerText,
                    color = Color.White,
                    fontSize = centerTextSize,
                    fontWeight = FontWeight.Bold
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
    private fun RespiratoryInstabilityGraphView (
        title: String,
        totalValue: String = "총-회",
        rightBoxValue: List<Triple<Pair<String,String>, Pair<String,String>, Color>> = emptyList()
    ) {
        var size by remember { mutableStateOf(IntSize(0, 0)) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (rightBoxValue.size >= 2) Alignment.CenterVertically else Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(4f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title, color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Image(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clickable { viewModel.sendInfoMessage(title, getString(R.string.detail_instability_breathing_text)) },
                        painter = painterResource(id = R.drawable.question),
                        contentDescription = ""
                    )
                }
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
                    .weight(6f)
                    .padding(16.dp, 0.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            
            ) {
                rightBoxValue.mapIndexed { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(59.dp)
                            .background(
                                color = Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = colorResource(id = R.color.color_stroke_line),
                                shape = RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp)
                            )
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value.first.first,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            modifier = Modifier.weight(7f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(52.dp)
                                .weight(3f)
                                .background(
                                    color = value.third,
                                    shape = RoundedCornerShape(topEnd = 11.dp)
                                ), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.second.first,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(59.dp)
                            .background(
                                color = Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = colorResource(id = R.color.color_stroke_line),
                                shape = RoundedCornerShape(bottomStart = 11.dp, bottomEnd = 11.dp)
                            )
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value.first.second,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            modifier = Modifier.weight(7f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(52.dp)
                                .weight(3f)
                                .background(
                                    color = value.third,
                                    shape = RoundedCornerShape(bottomEnd = 11.dp)
                                ), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if(value.second.second == "0회") "-" else value.second.second,
                                color = Color.White,
                                maxLines = 2,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
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
    private fun BarChartView(
        titleText: String,
        totalTime: Int,
        time: Int,
        scrollState: ScrollState
    ) {
        var height by remember { mutableStateOf(0.dp) }
        var animationPlayed by remember { //애니메이션 트리거를 위한 boolean 값
            mutableStateOf(false)
        }
        when {
            scrollState.canScrollBackward -> {
                animationPlayed = true
            }
        }

        val density = LocalDensity.current
        val percentValue = (time / totalTime.toFloat()) * 100f
        val percent = if (percentValue.isNaN()) 0.dp else height * ((percentValue / 100f))
        val curValue = animateIntAsState(
            targetValue = if (animationPlayed) percent.value.toInt()
            else 0, animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = "애니"
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(61.dp, 136.dp)
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.color_stroke_line),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clip(RoundedCornerShape(15.dp))
                    .background(color = colorResource(id = R.color.color_gray0))
                    .onGloballyPositioned { coordinates ->
                        height = with(density) {
                            coordinates.size.height.toDp()
                        }
                    }, contentAlignment = Alignment.BottomCenter

            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(curValue.value.dp)
                        .border(
                            width = 1.dp,
                            color = colorResource(id = R.color.color_stroke_line),
                            shape = RoundedCornerShape(15.dp)
                        )
                        .clip(RoundedCornerShape(15.dp))
                        .background(color = colorResource(id = R.color.color_gray3)),
                    contentAlignment = Alignment.Center
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
            percent < 31 -> {
                R.drawable.ic_green_value
            }

            percent < 51 -> {
                R.drawable.ic_yallow_value
            }

            percent < 71 -> {
                R.drawable.ic_orange_value
            }

            else -> {
                R.drawable.ic_red_value
            }
        }
    }
    private fun getReversPercentImage(percent: Float): Int {
        return when {
            percent < 31 -> {
                R.drawable.ic_red_value
            }
            
            percent < 51 -> {
                R.drawable.ic_orange_value
            }
            
            percent < 71 -> {
                R.drawable.ic_yallow_value
            }
            
            else -> {
                R.drawable.ic_green_value
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
                        binding.actionProgress.clProgress.visibility =
                            if (it) View.VISIBLE else View.GONE
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
                launch {
                    viewModel.infoMessage.collectLatest {
                        infoDialogBinding.tvTitleInfoText.text = it.first
                        infoDialogBinding.tvInfoText.text = it.second
                        showConnectDialog()
                    }
                }
                infoDialogBinding.btConnect.setOnSingleClickListener {
                    infoDialog.dismiss()
                }
            }
        }
    }
    
    fun showConnectDialog() {
        if (infoDialog.isShowing) {
            infoDialog.dismiss()
        }
        infoDialog.show()
    }
    
    /*private fun breathingScore(apneaCount: Int = 0, noSeringTime: Int = 0, apneaTime: Int = 0, sleepTime: Int = 0): Int{
        var resultScore =
            (60 - ((apneaCount.toFloat() / apneaTime.toFloat()) * 2)) +
                    (30 - ((noSeringTime.toFloat() / apneaTime.toFloat()) * 15)) +
                    (10 - ((apneaTime.toFloat() / apneaTime.toFloat()) * 10))
        when {
            resultScore <= 10 -> resultScore = 10F
            resultScore >= 90 -> resultScore = 90F
            else -> logHelper.insertLog("점수 오류 $resultScore")
        }
        println("${resultScore.toInt()}")
        
        return resultScore.toInt()
    }
    
    fun noseRingScore(noSeringTime: Int = 0, sleepTime: Int = 0,): Int {
        var resultScore = (100 - ((noSeringTime.toFloat() / sleepTime.toFloat()) * 100))
        when {
            resultScore < 10 -> resultScore = 10f
            else -> logHelper.insertLog("점수 오류 $resultScore")
        }
        return resultScore.toInt()
    }*/
    
}
