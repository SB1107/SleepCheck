package kr.co.sbsolutions.newsoomirang.presenter.question

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.data.entity.ContactData
import kr.co.sbsolutions.newsoomirang.data.entity.ContactEntity
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.question.contactDetail.ContactDetailActivity
import kr.co.sbsolutions.newsoomirang.presenter.question.contactUs.ContactUsActivity

@AndroidEntryPoint
class QuestionActivity : BaseServiceActivity() {
    private val viewModel: QuestionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val contactResultList by viewModel.contactResultData.collectAsState(initial = ContactEntity())
            DefaultPreview(contactResultList)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun DefaultPreview(contactData: ContactEntity = ContactEntity()) {
        val text by remember { mutableStateOf("") }

//        val contactResultList by viewModel.contactResultData.collectAsState(this)
//        Log.d(TAG, "DefaultPreview: $contactResultList")

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(LocalContext.current.getColor(R.color.color_061629)),
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            text = "문의하기",
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.color_FFFFFF)
                            ),
                            fontSize = 16.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    modifier = Modifier,
                    onClick = {
                        startActivity(
                            Intent(
                                this@QuestionActivity,
                                ContactUsActivity::class.java
                            )
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "문의하기")
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .background(color = colorResource(R.color.color_061629))
                    .padding(horizontal = 30.dp, vertical = 0.dp)
                    .fillMaxSize()
                    .padding(innerPadding),
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
                    LazyColumn() {
                        items(contactData.result.data ?: emptyList()) { data ->
                            ContactList(data)
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun ContactList(data: ContactData) {
        val endedAt = data.createdAt?.toDate("yy-MM-dd HH:mm")
        val titleDate = endedAt?.toDayString("MM월 dd일")

        HorizontalDivider(thickness = 1.dp, color = Color.White)
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .background(
                                color = if (data.answer == "Y") colorResource(id = R.color.md_green_600) else colorResource(
                                    id = R.color.color_78899F
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(vertical = 5.dp),
                        text = if (data.answer == "Y") "답변있음" else "답변없음",
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
                Text(
                    text = data.title ?: "",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Column {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = {
                        startActivity(
                            Intent(
                                this@QuestionActivity,
                                ContactDetailActivity::class.java
                            ).apply {
                                putExtra("date", data.createdAt)
                                putExtra("title", data.title,)
                                putExtra("content", data.content,)
                                putExtra("ansContent", data.ansContent)
                                putExtra("ansCreatedAt", data.ansCreatedAt,)

                            })
                    },
                    Modifier
                        .size(74.dp, 45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = colorResource(id = R.color.color_yellow))
                ) {
                    Text(
                        text = "보기",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                }
            }
        }
    }
}