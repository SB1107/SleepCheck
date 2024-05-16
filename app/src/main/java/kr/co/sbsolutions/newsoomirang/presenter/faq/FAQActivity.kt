package kr.co.sbsolutions.newsoomirang.presenter.faq

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.getLanguage
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.data.entity.FAQContentsData
import kr.co.sbsolutions.newsoomirang.data.entity.FAQResultData
import kr.co.sbsolutions.newsoomirang.databinding.ActivityFaqBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.ScrollToView
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold
import java.util.Locale

@AndroidEntryPoint
class FAQActivity : BaseActivity() {
    private val viewModel: FAQViewModel by viewModels()
    private val binding: ActivityFaqBinding by lazy {
        ActivityFaqBinding.inflate(layoutInflater)
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
            setContent { RootView()}
        }
        viewModel.getFAQList(this.getLanguage())
        setObservers()
    }

    @Preview
    @Composable
    fun RootView(data: FAQResultData? = FAQResultData()) {
        val scrollState = rememberLazyListState()
        val showButton = remember { derivedStateOf { scrollState.firstVisibleItemIndex > 0 } }
        SoomScaffold(R.drawable.back1, stringResource(R.string.faq_title), topAction = {
            finish()
        }, childView = {
            Box {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    state = scrollState
                ) {

                    itemsIndexed(data?.data ?: emptyList()) { index, item ->

                        FaqItemRow(item)
                        HorizontalDivider(thickness = 1.dp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))

                    }
                }
                ScrollToView(showButton.value, scrollState)
            }
        })
    }

    @Composable
    private fun FaqItemRow(item: FAQContentsData) {
        var isExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        isExpanded = !isExpanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question ?: "",
                    modifier = Modifier.weight(9f),
                    style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 21.sp, color = Color.White))

                Crossfade(targetState = isExpanded, label = stringResource(R.string.faq_icon), modifier = Modifier.padding(start = 8.dp)) {
                    when(it){
                        true -> {
                            Image(
                                painter = painterResource(id = R.drawable.ic_faq_up ),
                                contentDescription = stringResource(R.string.faq_kr_icon)
                            )
                        }
                        false ->{
                            Image(
                                painter = painterResource(id = R.drawable.ic_faq_down),
                                contentDescription = stringResource(R.string.faq_kr_icon))
                        }
                        }
                    }

                }

            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(visible = isExpanded ) {
                Column {

                    HorizontalDivider(thickness = 1.dp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.answer ?: "",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .background(color = colorResource(id = R.color.color_yellow), shape = RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 21.sp, color = Color.Black))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.faqData.collectLatest {
                        binding.composeView.apply {
                            setContent {
                                RootView(it)
                            }
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(R.string.common_title, it)
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