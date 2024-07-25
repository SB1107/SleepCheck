package kr.co.sbsolutions.sleepcheck.presenter.main.history

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint.Style
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.toDate
import kr.co.sbsolutions.sleepcheck.common.toDayString
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDateEntity
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDateResult
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDateResultData
import kr.co.sbsolutions.sleepcheck.databinding.FragmentHistoryBinding
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.ScrollToView
import kr.co.sbsolutions.sleepcheck.presenter.main.history.detail.HistoryDetailActivity
import java.time.LocalDate

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private val binding: FragmentHistoryBinding by lazy {
        FragmentHistoryBinding.inflate(layoutInflater)
    }
    private val clickItem: (String) -> Unit = object : (String) -> Unit {
        override fun invoke(id: String) {
            requireActivity().startActivity(Intent(requireActivity(), HistoryDetailActivity::class.java).apply {
                putExtra("id", id)
            })
        }
    }
    private var mSelectedDate: LocalDate = LocalDate.now()

    //    private val adapter = HistoryAdapter(clickItem)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.apply {
            setContent {
                RootView(SleepDateEntity(null))
            }
        }
        bindViews()
        setObservers()
        viewModel.getYearSleepData(mSelectedDate.year.toString())
    }


    @Preview
    @Composable
    fun RootView(yearData: SleepDateEntity = SleepDateEntity(null), showProgressBar: Boolean = true) {
        val scrollState = rememberLazyListState()
        val showButton = remember { derivedStateOf { scrollState.firstVisibleItemIndex > 0 } }

        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colorResource(id = R.color.color_back_gradient_start),
                                colorResource(id = R.color.color_back_gradient_end)
                            ),
                        )
                    )
            ) {

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 48.dp, 16.dp, 16.dp)
                            .background(color = colorResource(id = android.R.color.transparent)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopYearView()
                    }
//                    HorizontalDivider(thickness = 1.dp, color = Color.White)
                    Box {
//                        if (showProgressBar) {
//                            Components.LottieLoading()
//                        }
                        if (yearData.result?.data?.isEmpty() != false) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_history), fontSize = 21.sp, fontWeight = FontWeight.Normal, color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            Box {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = scrollState
                                ) {
                                    itemsIndexed(yearData.result?.data ?: emptyList()) { index, item ->
                                        SleepItemRow(item)
//                                        if (index < (yearData.result?.data ?: emptyList()).lastIndex) {
//                                            HorizontalDivider(thickness = 1.dp, color = Color.White)
//                                        }
                                    }
                                }
                                ScrollToView(showButton.value, scrollState)
                            }
                        }
                    }

                }
            }

        }
    }


    @Composable
    private fun SleepItemRow(data: SleepDateResult) {
        val endedAt = data.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val titleDate = if (LocalConfiguration.current.locales[0] == java.util.Locale.KOREA) {
            endedAt?.toDayString("M월 d일 E요일", LocalConfiguration.current.locales[0])
        } else {
            endedAt?.toDayString("MMM d EEEE", LocalConfiguration.current.locales[0])
        }
        val startAt = data.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
        val durationString =
            (startAt?.toDayString("HH:mm") + "~" + (endedAt?.toDayString("HH:mm"))).plus(" ").plus(if (data.type == 0) stringResource(R.string.breating) else stringResource(R.string.nosering))
        Row(modifier = Modifier
            .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = colorResource(id = R.color.color_99DFDFDF))
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            .clickable {
                clickItem.invoke(data.id)
            }) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 8.dp, 0.dp)
                            .size(21.dp),
                        painter = painterResource(id = if (data.type == 0) R.drawable.ic_br else R.drawable.ic_sn),
                        contentDescription = "측정 타입",
                        alignment = Alignment.Center
                    )
                    Text(
                        text = titleDate ?: "", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.Black,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = durationString, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                )
            }

            Column {
                IconButton(
                    onClick = { clickItem.invoke(data.id) },
                    Modifier
                        .size(84.dp, 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = colorResource(id = R.color.color_main))
                ) {
                    Text(text = stringResource(R.string.detail), fontSize = 19.sp, fontWeight = FontWeight.Normal, color = Color.White)
                }
            }
        }
    }

    @Composable
    private fun TopYearView() {
        var yearText by remember { mutableIntStateOf(LocalDate.now().year) }
        YearButton(imageResource = R.drawable.arrow_left, click = {
            yearText = prevYear(yearText)
        })
        Text(
            text = yearText.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp, 0.dp)
        )
        if (yearText != LocalDate.now().year) {
            YearButton(imageResource = R.drawable.arrow_right, click = {
                yearText = nextYear(yearText)
            })
        }
    }

    private fun prevYear(currentYear: Int): Int {
        val minYear = 2022
        var tempYear = currentYear
        if (currentYear > minYear) {
            tempYear = currentYear.minus(1)
        }
        if (currentYear == minYear) {
            return currentYear
        }
        viewModel.getYearSleepData(tempYear.toString())
        return tempYear
    }

    private fun nextYear(currentYear: Int): Int {
        val maxYear = LocalDate.now().year
        var tempYear = currentYear
        if (currentYear < maxYear) {
            tempYear = currentYear.plus(1)
        }
        if (currentYear == maxYear) {
            return currentYear
        }
        viewModel.getYearSleepData(tempYear.toString())
        return tempYear
    }

    @Composable
    fun YearButton(imageResource: Int, click: () -> Unit) {
//        OutlinedButton(onClick = click, modifier = Modifier
//            .width(41.dp)
//            .height(41.dp),
//            shape = RoundedCornerShape(10.dp),
//            border = BorderStroke(1.dp , colorResource(id = R.color.colorPrimaryDark)),
//            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
//        ) {
//            Icon(
//                painter = painterResource(id = imageResource),
//                contentDescription = "",
//                tint = Color.White
//            )
//        }
        IconButton(
            modifier = Modifier
                .width(41.dp)
                .height(41.dp)
                .clip(RoundedCornerShape(15.dp))
                .drawBehind {
                    drawRoundRect(
                        color = Color.White,
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(15.dp.toPx())
                    )
                }, onClick = click
        ) {
            Icon(
                painter = painterResource(id = imageResource),
                contentDescription = "",
                tint = Color.White
            )
        }
    }


    @SuppressLint("SetTextI18n")
    private fun bindViews() {
//        binding.dateTextView.text = mSelectedDate.year.toString()
//        binding.dateTextView.setOnSingleClickListener {
//            requireActivity().showYearDialog(mSelectedDate.year, null) {
//                mSelectedDate = LocalDate.of(it, 1, 1)
//                binding.dateTextView.text = mSelectedDate.year.toString()
//                viewModel.getYearSleepData(mSelectedDate.year.toString())
//            }
//        }
//        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext, LinearLayoutManager.VERTICAL, false)
//
//        //adpter 작업 필요함
//        binding.historyRecyclerView.adapter = adapter

    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sleepYearData.collectLatest {
//                        it.result?.data?.let { list ->
//                            adapter.submitList(list.toMutableList())
                        binding.composeView.apply {
                            setContent {
//                                    val yearData by viewModel.sleepYearData.collectAsStateWithLifecycle(initialValue = SleepDateEntity(null))
                                RootView(it)
//                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        requireActivity().showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
                        binding.actionProgress.clProgress.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}