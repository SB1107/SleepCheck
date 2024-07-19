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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDetailResult
import kr.co.sbsolutions.sleepcheck.data.model.SleepDetailDTO
import kr.co.sbsolutions.sleepcheck.databinding.ActivityHistoryDetailBinding
import kr.co.sbsolutions.sleepcheck.databinding.DialogInfoMassageBinding
import kr.co.sbsolutions.sleepcheck.databinding.DialogSocreInfoMassageBinding
import kr.co.sbsolutions.sleepcheck.databinding.RowScoreBinding
import kr.co.sbsolutions.sleepcheck.presenter.BaseActivity
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.GraphsView
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.LottieLoading
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.ScrollToView
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SoomScaffold
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

        Box {
            Column(
                modifier = modifier
            ) {
                Box(modifier = Modifier.background(brush = Brush.verticalGradient(
                    colors = listOf(
                        colorResource(id = R.color.color_back_gradient_start),
                        colorResource(id = R.color.color_back_gradient_end)
                    ),
                ))) {

                    TopDateView(data = data, scrollState)
                }
            }
            ScrollToView(scrollState.value >= 200, scrollState)
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(color = colorResource(id = R.color.color_99DFDFDF))
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
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
                    title = stringResource( R.string.detial_normal_b),
                   detailText =  stringResource(R.string.detail_normal_breathing_text)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color = colorResource(id = R.color.color_99DFDFDF))
                    .padding(start = 16.dp, end = 16.dp)){
                    RowTexts(
                        stringResource(R.string.detial_normal_b_t,),
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
                Column(modifier = Modifier
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
                            Color.Transparent,
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
            val lists: ArrayList<Fourth<Pair<String, String>, Pair<String, String>, Color,Color>> =
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

                Column(modifier = Modifier
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
                            Color.Transparent,
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
                Column(modifier = Modifier
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
                            Color.Transparent,
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

            data.straightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                HeaderTitleView(
                    title = stringResource(R.string.detail_sleep_position),
                    detailText = stringResource(R.string.detail_sleep_position_text)
                )
                Spacer(modifier = Modifier.height(16.dp))

                VerticalGraphView(
                    percentValue = (data.straightPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = stringResource(R.string.detail_supine),
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    endTextSize = 19.sp
                )
            }
            data.leftPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.leftPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = stringResource(R.string.detail_left),
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    endTextSize = 19.sp
                )
            }
            data.rightPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.rightPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = stringResource(R.string.detail_right),
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    endTextSize = 19.sp
                )
            }
            data.downPositionTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.downPer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = stringResource(R.string.detail_prone),
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    endTextSize = 19.sp
                )
            }
            data.wakeTime?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VerticalGraphView(
                    percentValue = (data.wakePer ?: 0).toFloat(),
                    isPercentText = true,
                    startText = stringResource(R.string.detail_standup),
                    startTextSize = 19.sp,
                    endText = it.InpuMintoHourMinute(LocalConfiguration.current.locales[0]),
                    endTextSize = 19.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (data.remSleepTime != null || data.lightSleepTime != null || data.deepSleepTime != null) {
//                HorizontalDivider(thickness = 1.dp, color = Color.White)
                HeaderTitleView(
                    title = stringResource(R.string.detail_sleep_stage),
                    detailText = getString(R.string.detail_sleep_stages_text)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly

            ) {

                data.remSleepTime?.let {
                    BarChartView(
                        stringResource(R.string.detail_rem_sleep),
                        data.sleepTime ?: 0,
                        it,
                        scrollState
                    )
                }
                data.lightSleepTime?.let {
                    BarChartView(
                        stringResource(R.string.detail_light_sleep),
                        data.sleepTime ?: 0,
                        it,
                        scrollState
                    )
                }
                data.deepSleepTime?.let {
                    BarChartView(
                        stringResource(R.string.detail_deep_sleep),
                        data.sleepTime ?: 0,
                        it,
                        scrollState
                    )
                }
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
    private fun IconRowTexts(color: Color, startText: String, endText: String) {
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
    private fun RowTexts(startText: String, endText: String, textColor: Color = Color.Black) {
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
    private fun HeaderTitleView(backColor: Color = Color.Transparent,title: String, detailText: String? = null) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp, start = 50.dp, end = 50.dp)
                .fillMaxWidth()
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
                    LottieLoading(modifier =
                    Modifier
                        .size(108.dp)
                        .offset(y = 15.dp)
                        .clickable {

                        })
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
                            modifier =   Modifier.weight(7f)) {
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
    private fun RespiratoryInstabilityGraphView(
        title: String,
        totalValue: String = stringResource(R.string.detail_total_score),
        rightBoxValue: List<Fourth<Pair<String, String>, Pair<String, String>, Color,Color>> = emptyList()
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
                            modifier =   Modifier.weight(7f)) {
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
                    text = time.toHourOrMinute(LocalConfiguration.current.locales[0]),
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

    private fun getScoreColor(score: Int): Int {
        return when (score) {
            in 0..40 -> getColor(R.color.color_CA0000)
            in 41..80 -> getColor(R.color.color_FFDB1C)
            else -> getColor(R.color.color_0DAD13)
        }
    }

}
