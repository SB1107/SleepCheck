package kr.co.sbsolutions.sleepcheck.presenter.main.history.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.Fourth
import kr.co.sbsolutions.sleepcheck.common.InpuMintoHourMinute
import kr.co.sbsolutions.sleepcheck.common.getLanguage
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.toDate
import kr.co.sbsolutions.sleepcheck.common.toDayString
import kr.co.sbsolutions.sleepcheck.common.toHourMinute
import kr.co.sbsolutions.sleepcheck.common.toHourOrMinute
import kr.co.sbsolutions.sleepcheck.data.model.SleepDetailDTO
import kr.co.sbsolutions.sleepcheck.databinding.ActivityHistoryDetailBinding
import kr.co.sbsolutions.sleepcheck.databinding.DialogInfoMassageBinding
import kr.co.sbsolutions.sleepcheck.databinding.DialogSocreInfoMassageBinding
import kr.co.sbsolutions.sleepcheck.databinding.RowScoreBinding
import kr.co.sbsolutions.sleepcheck.presenter.BaseActivity
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.BottomText
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.GraphsView
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.IconRowTexts
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.LineView
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.PositionComposable
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.RowTexts
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.ScrollToView
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SleepState
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.sleepcheck.presenter.components.GradientBarChart
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
    private val scoreDialogBinding: DialogSocreInfoMassageBinding by lazy {
        DialogSocreInfoMassageBinding.inflate(layoutInflater)
    }


    private val infoDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(infoDialogBinding.root, null)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isFitToContents = true
        }
    }
    private val scoreInfoDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(scoreDialogBinding.root, null)
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
                RootView(SleepDetailDTO())
            }
        }
        intent?.let {
            it.getStringExtra("id")?.let { id ->
                viewModel.getSleepData(id, this.getLanguage())
            }
        }
        setObservers()
    }

    @OptIn(ExperimentalComposeApi::class, ExperimentalComposeUiApi::class)
    @Preview
    @Composable
    fun RootView(data: SleepDetailDTO = SleepDetailDTO()) {
        val scrollState = rememberScrollState()
        val captureController = rememberCaptureController()
        val scope = rememberCoroutineScope()
        val columnModifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .capturable(captureController)

        SoomScaffold(
            R.drawable.back1,
            bgColor = colorResource(id = R.color.color_00296B),
            stringResource(R.string.detail_result),
            topAction = {
                finish()
            },
            row = {
                IconButton(onClick = {
                    scope.launch {
                        val bitmapAsync = captureController.captureAsync()
                        try {
                            val bitmap = bitmapAsync.await()
                            // Do something with `bitmap`.
                            viewModel.sharingImage(this@HistoryDetailActivity, bitmap)
                        } catch (error: Throwable) {
                            Log.e(TAG, "RootView: error")
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = stringResource(R.string.detail_share),
                        tint = Color.White
                    )
                }
            },
            childView = {
                ContentView(data, modifier = columnModifier, scrollState = scrollState)
            })
    }

    @Composable
    private fun ContentView(
        data: SleepDetailDTO,
        modifier: Modifier,
        scrollState: ScrollState = rememberScrollState()
    ) {
        Column {
            Column(
                modifier = modifier.weight(9.4f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorResource(id = R.color.color_back_gradient_start),
                                colorResource(id = R.color.color_back_gradient_end)
                            ),
                        )
                    )
                ) {
                    TopDateView(data = data, scrollState)
                }
            }
            Button(modifier = Modifier
                .height(62.dp)
                .fillMaxWidth()
                .weight(0.6f)
                .padding(horizontal = 24.dp)
                .background(
                    color = colorResource(id = R.color.color_main),
                    shape = RoundedCornerShape(
                        topStart = 60.dp, // 왼쪽 위 코너
                        topEnd = 60.dp,   // 오른쪽 위 코너
                        bottomStart = 0.dp, // 왼쪽 아래 코너
                        bottomEnd = 0.dp    // 오른쪽 아래 코너
                    )
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(id = R.color.color_777777),
                    disabledContentColor = Color.Black
                ), onClick = { viewModel.getLink()}) {
                Text(text = "수면 정보 상세 보기", fontSize = 19.sp, style = TextStyle(fontWeight = FontWeight.Bold))
            }
        }
    }


    @Composable
    private fun TopDateView(
        data: SleepDetailDTO = SleepDetailDTO(),
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
            (startAt?.toDayString("HH:mm") + " ~ " + (endedAt?.toDayString("HH:mm")))

        val milliseconds: Long = (endedAt?.time ?: 0) - (startAt?.time ?: 0)
        val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute(
            LocalConfiguration.current.locales[0]
        )

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
            Spacer(modifier = Modifier.height(24.dp))
            if (durationString.contains("null").not()) {
                Text(
                    text = durationString,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color = colorResource(id = R.color.color_99DFDFDF))
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                RowTexts(startText = stringResource(R.string.detil_time), endText = min)
                data.sleepTime?.let {
                    RowTexts(
                        stringResource(R.string.detil_sleep_time),
                        it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                    )
                }
                data.asleepTime?.let {
                    RowTexts(
                        stringResource(R.string.detail_asleep_time),
                        it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                    )
                }
                data.deepSleepTime?.let {
                    RowTexts(
                        stringResource(R.string.detail_deep_sleep_time),
                        it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                    )
                }
                data.moveCount?.let {
                    RowTexts(
                        stringResource(R.string.detail_turns),
                        stringResource(R.string.detail_times, it)
                    )
                }
            }


//            if (data.type == 1) {
//                data.snoreCount?.let {
//                    RowTexts(
//                        stringResource(R.string.detail_vibration),
//                        stringResource(R.string.detail_times, it)
//                    )
//                }
//            }


            data.normalBreathTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
//                HorizontalDivider(thickness = 1.dp, color = Color.White)

                HeaderTitleView(
                    title = stringResource(R.string.detial_normal_b),
                    detailText = stringResource(R.string.detail_normal_breathing_text)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = colorResource(id = R.color.color_99DFDFDF))
                        .padding(start = 16.dp, end = 16.dp)
                ) {
                    RowTexts(
                        stringResource(R.string.detial_normal_b_t),
                        it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    )
                    data.avgNormalBreath?.let {
                        RowTexts(
                            stringResource(R.string.detail_average_b),
                            if (it == 0) "-" else stringResource(R.string.detail_min, it),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            data.apneaCount?.let {
                //todo 비정상 호흡 시간으로 변경 필요
                HeaderTitleView(title = stringResource(R.string.detail_a_b))
                /*data.unstableBreath?.let {
                    RowTexts("비정상 호흡 시간", it.InpuMintoHourMinute())
                }*/

                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = colorResource(id = R.color.color_0064F5))
                        .padding(16.dp),

                    ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "호흡 없음 구간", color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GraphsView(
                        listData = data.nobreath_idx,
                        drawColors = listOf(
                            colorResource(id = R.color.color_E6F6FF),
                            colorResource(id = R.color.color_1DAEFF),
                            colorResource(id = R.color.color_FDABFF),
                            colorResource(id = R.color.color_FF4F37)
                        ),
                        startText = startAt?.toDayString("HH:mm") ?: "",
                        endText = endedAt?.toDayString("HH:mm") ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                BreathingGraphView(
                    stringResource(R.string.detail_no_signal),
                    stringResource(R.string.detail_total_score2, it),
                    listOf(
                        Fourth(
                            stringResource(R.string.detail_no_signal_10_secs),
                            stringResource(R.string.detail_count, data.apnea10 ?: 0),
                            colorResource(id = R.color.color_gray1),
                            colorResource(id = R.color.color_1DAEFF)
                        ),
                        Fourth(
                            stringResource(R.string.detail_no_signal_30_secs),
                            stringResource(R.string.detail_count, data.apnea30 ?: 0),
                            colorResource(id = R.color.color_gray2),
                            colorResource(id = R.color.color_FDABFF)
                        ),
                        Fourth(
                            stringResource(R.string.detail_no_signal_60_secs),
                            stringResource(R.string.detail_count, data.apnea60 ?: 0),
                            colorResource(id = R.color.color_gray3),
                            colorResource(id = R.color.color_FF4F37)
                        )
                    )
                )
            }
            val lists: ArrayList<Fourth<Pair<String, String>, Pair<String, String>, Color, Color>> =
                ArrayList()
            data.fastBreath?.let { fastBreath ->
                data.avgFastBreath?.let { avgFastBreath ->
                    lists.add(
                        Fourth(
                            Pair(
                                stringResource(R.string.detail_fast_breathing),
                                stringResource(R.string.detail_average_respiratory_rate)
                            ),
                            Pair(
                                stringResource(R.string.detail_minutes, fastBreath),
                                stringResource(R.string.detail_count, avgFastBreath)
                            ),
                            colorResource(id = R.color.color_gray1),
                            colorResource(id = R.color.color_FDABFF)
                        )
                    )
                }
            }
            data.slowBreath?.let { slowBreath ->
                data.avgSlowBreath?.let { avgSlowBreath ->
                    lists.add(
                        Fourth(
                            Pair(
                                stringResource(R.string.detail_slow_breathing),
                                stringResource(R.string.detail_average_respiratory_rate)
                            ),
                            Pair(
                                stringResource(R.string.detail_minutes, slowBreath),
                                stringResource(R.string.detail_count, avgSlowBreath)
                            ),
                            colorResource(id = R.color.color_gray2),
                            colorResource(id = R.color.color_1DAEFF)
                        )
                    )
                }
            }
            if (lists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                val totalCount =
                    (data.fastBreath ?: 0) + (data.slowBreath ?: 0) + (data.unstableBreath ?: 0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = colorResource(id = R.color.color_0064F5))
                        .padding(16.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "호흡불안정 구간", color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GraphsView(
                        listData = data.unstableIdx,
                        drawColors = listOf(
                            colorResource(id = R.color.color_E6F6FF),
                            colorResource(id = R.color.color_FDABFF),
                            colorResource(id = R.color.color_1DAEFF),
                            colorResource(id = R.color.color_FF4F37)
                        ),
                        startText = startAt?.toDayString("HH:mm") ?: "",
                        endText = endedAt?.toDayString("HH:mm") ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                RespiratoryInstabilityGraphView(
                    title = stringResource(R.string.detail_unstable_breathing),
                    stringResource(R.string.detail_total_min, totalCount),
                    lists
                )
            }
            data.snoreTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HeaderTitleView(title = stringResource(R.string.detail_snoring))
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = colorResource(id = R.color.color_0064F5))
                        .padding(16.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "코골이 / 기침 구간", color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GraphsView(
                        listData = data.snoring_idx,
                        drawColors = listOf(
                            colorResource(id = R.color.color_E6F6FF),
                            colorResource(id = R.color.color_FDABFF),
                            colorResource(id = R.color.color_1DAEFF),
                            colorResource(id = R.color.color_FF4F37)
                        ),
                        startText = startAt?.toDayString("HH:mm") ?: "",
                        endText = endedAt?.toDayString("HH:mm") ?: ""
                    )
                }

                IconRowTexts(
                    color = colorResource(id = R.color.color_FDABFF),
                    stringResource(R.string.detail_snoring_time),
                    it.InpuMintoHourMinute(LocalConfiguration.current.locales[0])
                )
            }
            data.coughCount?.let {
                IconRowTexts(
                    color = colorResource(id = R.color.color_1DAEFF),
                    stringResource(R.string.detil_cough),
                    stringResource(R.string.detail_times, it)
                )

            }
            Spacer(modifier = Modifier.height(16.dp))
            data.straightPer?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HeaderTitleView(
                    title = stringResource(R.string.detail_sleep_position),
                    detailText = stringResource(R.string.detail_sleep_position_text)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            PositionComposable(data)

            if (data.remSleepTime != null || data.lightSleepTime != null || data.deepSleepTime != null) {
//                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView(
                    title = stringResource(R.string.detail_sleep_stage),
                    detailText = getString(R.string.detail_sleep_stages_text)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                data.remSleepTime?.let {
                    SleepState(
                        label = stringResource(R.string.detail_rem_sleep),
                        sleepTime = it,
                        data.sleepTime ?: 0
                    )
                }
                data.lightSleepTime?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    SleepState(
                        label = stringResource(R.string.detail_light_sleep),
                        sleepTime = it,
                        data.sleepTime ?: 0
                    )
                }
                data.deepSleepTime?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    SleepState(
                        label = stringResource(R.string.detail_deep_sleep),
                        sleepTime = it,
                        data.sleepTime ?: 0
                    )
                }
            }

            Column {
                Spacer(modifier = Modifier.height(16.dp))
                if(data.remIdx.isNotEmpty()) {
                    LineView(
                        stringResource(R.string.detail_rem_sleep),
                        data.remIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_1DAEFF),
                            colorResource(id = R.color.color_1DAEFF)
                        )
                    )
                }
                if(data.lightIdx.isNotEmpty()) {
                    LineView(
                        stringResource(R.string.detail_light_sleep),
                        data.lightIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_9ACF40),
                            colorResource(id = R.color.color_9ACF40)
                        )
                    )
                }
                if(data.deepIdx.isNotEmpty()){
                    LineView(
                        stringResource(R.string.detail_deep_sleep),
                        data.deepIdx,
                        drawColors = listOf(
                            Color.Transparent,
                            colorResource(id = R.color.color_FF6008),
                            colorResource(id = R.color.color_FF6008)
                        ), isLast = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BottomText(
                        Modifier
                            .padding(start = 50.dp)
                            .fillMaxWidth(),
                        startAt?.toDayString("HH:mm") ?: "",
                        endedAt?.toDayString("HH:mm") ?: ""
                    )
                }

            }
            if (data.movement.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HeaderTitleView(title = "수면 중 움직임")
                Spacer(modifier = Modifier.height(16.dp))
                GradientBarChart(
                    data = data.movement,
                    gradientColor = listOf(
                        colorResource(id = R.color.color_F44E4E), colorResource(
                            id = R.color.color_main
                        )
                    ), defaultColor = colorResource(id = R.color.color_main), threshold = 3
                )
                BottomText(modifier =  Modifier
                    .fillMaxWidth(),
                    startAt?.toDayString("HH:mm") ?: "",
                    endedAt?.toDayString("HH:mm") ?: ""
                )
            }


            Text(
                text = data.ment ?: "",
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
    fun HeaderTitleView(
        backColor: Color = colorResource(id = R.color.color_gray3),
        title: String,
        detailText: String? = null
    ) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp, start = 0.dp, end = 0.dp)
                .fillMaxWidth()
                .height(53.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backColor)
                .padding(16.dp, 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .align(alignment = Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .align(Alignment.CenterVertically),
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
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
        totalValue: String = stringResource(R.string.detail_total_score),
        rightBoxValue: List<Fourth<String, String, Color, Color>> = emptyList()

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
                        textAlign = TextAlign.Center
                    )
                    Image(
                        modifier = Modifier

                            .padding(start = 5.dp)
                            .clickable {
                                viewModel.sendInfoMessage(
                                    title,
                                    getString(R.string.detail_no_signal_breathing_text)
                                )
                            },
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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
//                    LottieLoading(modifier =
//                    Modifier
//                        .size(108.dp)
//                        .offset(y = 15.dp)
//                        .clickable {
//
//                        })
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.weight(7f)
                        ) {
                            Text(
                                text = value.first,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                modifier = Modifier.weight(8f)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(value.fourth)
                            )
                        }

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
    private fun RespiratoryInstabilityGraphView(
        title: String,
        totalValue: String = stringResource(R.string.detail_total_score),
        rightBoxValue: List<Fourth<Pair<String, String>, Pair<String, String>, Color, Color>> = emptyList()
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
                        modifier = Modifier
                            .weight(8f),
                        text = title, color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Image(
                        modifier = Modifier
                            .weight(2f)
                            .padding(start = 5.dp)
                            .clickable {
                                viewModel.sendInfoMessage(
                                    title,
                                    getString(R.string.detail_instability_breathing_text)
                                )
                            },
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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.weight(7f)
                        ) {
                            Text(
                                text = value.first.first,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                modifier = Modifier.weight(8f)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(value.fourth)
                            )
                        }
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
                                text = if (value.second.second == stringResource(R.string.detail_0_count)) "-" else value.second.second,
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
                        infoDialogBinding.btConnect.setOnSingleClickListener {
                            infoDialog.dismiss()
                        }
                        showConnectDialog()
                    }
                }
                launch {
                    viewModel.scoreInfoMessage.collectLatest {
                        scoreDialogBinding.tvInfoText.text = it.msg
                        scoreDialogBinding.tvTitleInfoText.text = it.score.toString().plus("점")
                        scoreDialogBinding.tvTitleInfoText.setTextColor(getScoreColor(it.score))
                        scoreDialogBinding.llContent.removeAllViews()
                        it.data.map { data ->
                            val scoreInfoBinding = RowScoreBinding.inflate(layoutInflater)
                            Glide.with(this@HistoryDetailActivity)
                                .load(data.image)
                                .fitCenter()
                                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                                .into(scoreInfoBinding.ivImage)
                            scoreInfoBinding.tvInfoDesText.text = data.title
                            scoreInfoBinding.ivImage.setOnSingleClickListener {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(data.link)
                                startActivity(intent)
                            }
                            scoreDialogBinding.llContent.addView(scoreInfoBinding.root)
                        }
                        scoreDialogBinding.btConnect.setOnSingleClickListener { scoreInfoDialog.dismiss() }
                        scoreInfoDialog.show()
                    }
                }
                launch {
                    viewModel.connectLink.collectLatest {
                        if (it.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(it)
                            startActivity(intent)
                        }
                    }
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


    private fun getScoreColor(score: Int): Int {
        return when (score) {
            in 0..40 -> getColor(R.color.color_CA0000)
            in 41..80 -> getColor(R.color.color_FFDB1C)
            else -> getColor(R.color.color_0DAD13)
        }
    }

}
