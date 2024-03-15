package kr.co.sbsolutions.newsoomirang.presenter.question

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.data.entity.ContactData
import kr.co.sbsolutions.newsoomirang.data.entity.ContactEntity
import kr.co.sbsolutions.newsoomirang.databinding.ActivityQuestionBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.ScrollToContactView
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.newsoomirang.presenter.question.contactDetail.ContactDetailActivity
import kr.co.sbsolutions.newsoomirang.presenter.question.contactUs.ContactUsActivity

@AndroidEntryPoint
class QuestionActivity : BaseActivity() {
    private val viewModel: QuestionViewModel by viewModels()
    private val binding: ActivityQuestionBinding by lazy {
        ActivityQuestionBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.composeView.apply {
            setContent {
                val contactResultList by viewModel.contactResultData.collectAsState(initial = ContactEntity())
                DefaultPreview(contactResultList)
            }
            setObservers()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getContactList()

    }

    override fun newBackPressed() {
        finish()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }

    private val clickItem: (String, String) -> Unit = object : (String, String) -> Unit {
        override fun invoke(id: String, date: String) {
            // TODO: Activity 생성후 수정 필요
            startActivity(Intent(this@QuestionActivity, ContactDetailActivity::class.java).apply {
                putExtra("id", id)
                putExtra("date", date)
            })
        }
    }

    @Preview
    @Composable
    fun DefaultPreview(contactData: ContactEntity = ContactEntity()) {
        val scrollState = rememberLazyListState()
        SoomScaffold(
            topText = "문의하기",
            topAction = { finish() },
            childView =
            {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 30.dp, vertical = 0.dp)
                            .fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {


                            if (contactData.result?.data?.isEmpty() != false) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "저장된 이력이 없습니다.",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = scrollState
                                ) {
                                    items(contactData.result.data ?: emptyList()) { data ->
                                        ContactList(data)
                                    }

                                }

                            }

                            ScrollToContactView(!scrollState.isScrollInProgress) {
                                IconButton(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(80.dp),
                                    onClick = {
                                        startActivity(
                                            Intent(
                                                this@QuestionActivity,
                                                ContactUsActivity::class.java
                                            )
                                        )
                                    }

                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.contect_icon),
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
                    }
                }
            })
    }

    @Composable
    fun ContactList(data: ContactData) {
        val endedAt = data.createdAt?.toDate("yy-MM-dd HH:mm")
        val titleDate = endedAt?.toDayString("yy년 MM월 dd일")

        HorizontalDivider(thickness = 1.dp, color = Color.White)
        Row(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .background(
                                color = if (data.answer == "Y") colorResource(id = R.color.color_0086FF) else colorResource(
                                    id = R.color.color_gradient_center
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(vertical = 5.dp),
                        text = if (data.answer == "Y") "답변완료" else "답변전",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = titleDate ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = data.title ?: "",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = {
                            startActivity(
                                Intent(
                                    this@QuestionActivity,
                                    ContactDetailActivity::class.java
                                ).apply {
                                    putExtra("date", data.createdAt)
                                    putExtra("title", data.title)
                                    putExtra("content", data.content)
                                    putExtra("ansContent", data.ansContent)
                                    putExtra("ansCreatedAt", data.ansCreatedAt)

                                })
                        },
                        Modifier
                            .size(33.dp, 33.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.new_arrow_right),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }

    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.contactResultData.collectLatest {
                        binding.composeView.apply {
                            /*setContent {
                                DefaultPreview(it)
                            }*/
                        }
                    }
                }
                launch {
                    viewModel.isProgressBar.collectLatest {
                        binding.actionProgress.clProgress.visibility =
                            if (it) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}