package kr.co.sbsolutions.newsoomirang.presenter.question.contactDetail

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
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
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold

@AndroidEntryPoint
class ContactDetailActivity : BaseServiceActivity() {
    private val viewModel: ContactDetailViewModel by viewModels()
    override fun newBackPressed() {
        finish()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultPreview(intent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun DefaultPreview(intent: Intent?) {
        SoomScaffold(
            topText = stringResource(R.string.setting_qna),
            topAction = { finish() },
            childView =
            {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                ) {
                    Contact(intent)
                }
            }
        )
    }

    //문의하기 화면
    @Composable
    fun Contact(intent: Intent?) {
        val createDate = intent.let { it ->
            it?.getStringExtra("date")
        } ?: ""
        val title = intent.let { it ->
            it?.getStringExtra("title")
        } ?: ""
        val content = intent.let { it ->
            it?.getStringExtra("content")
        } ?: ""
        val ansContent = intent.let { it ->
            it?.getStringExtra("ansContent")
        } ?: "답변 없음"
        val ansCreatedAt = intent.let { it ->
            it?.getStringExtra("ansCreatedAt")
        } ?: ""
        val answer = intent.let { it ->
            it?.getStringExtra("answer")
        } ?: ""

        val createdAt = createDate.toDate(stringResource(R.string.date_time_format))?.toDayString(stringResource(R.string.date_time_format))
        val endAt = if (ansCreatedAt == "") "" else ansCreatedAt.toDate(stringResource(R.string.date_time_format))
            ?.toDayString(stringResource(R.string.date_time_format))

//        val titleDate = endedAt?.toDayString("yy년 MM월 dd일")

        /*Log.d(TAG, "createDate: $createDate")
        Log.d(TAG, "title: $title")
        Log.d(TAG, "Contact: $content")
        Log.d(TAG, "ansContent: $ansContent")
        Log.d(TAG, "ansCreatedAt: $ansCreatedAt")*/

        SpacerHeight(size = 10)
        Row {
            //답변 상태
            Text(
                modifier = Modifier
                    .width(80.dp)
                    .background(
                        color = if (answer == "Y") colorResource(id = R.color.color_0086FF) else colorResource(
                            id = R.color.color_gradient_center
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 5.dp),
                text = if (answer == "Y") stringResource(R.string.qna_answer_complete) else stringResource(R.string.qna_before_answering),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                ),
            )

            //작성일
            Text(
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(2f),
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontSize = 16.sp
                ),
                text = "$createdAt",
            )
        }
        SpacerHeight(size = 15)
        Box(
            modifier = Modifier
                .fillMaxWidth()

        ) {
            //문의 제목
            Text(
                textAlign = TextAlign.Start,
                text = title,
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
            )
        }
        SpacerHeight(size = 20)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                textAlign = TextAlign.Start,
                text = content,
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontSize = 21.sp
                )
            )
        }


        SpacerHeight(size = 20)
        HorizontalDivider(thickness = 1.dp, color = Color.White)
        SpacerHeight(size = 20)
        if (answer == "Y") AnsState(endAt.toString(), ansContent)
        SpacerHeight(size = 10)
    }

    @Composable
    fun NoAnsState() {
        SpacerHeight(size = 30)
        Text(
            modifier = Modifier
            .fillMaxWidth(),
            style = TextStyle(
                color = colorResource(id = R.color.color_FFFFFF),
                textAlign = TextAlign.Center,
                fontSize = 21.sp
            ),
            text = stringResource(R.string.contact_detail_waiting_message)
        )
        SpacerHeight(size = 30)

    }

    @Composable
    fun AnsState(endAt: String, ansContent: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                textAlign = TextAlign.Start,
                modifier = Modifier,
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                text = stringResource(R.string.contact_detail_answer_content),
            )
            Text(
                textAlign = TextAlign.End,
                modifier = Modifier,
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontSize = 16.sp
                ),
                text = endAt,
            )
        }

        SpacerHeight(size = 20)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                textAlign = TextAlign.Start,
                text = ansContent,
                style = TextStyle(
                    color = colorResource(id = R.color.color_FFFFFF),
                    fontSize = 21.sp
                )
            )
        }
    }
    @Composable
    fun SpacerHeight(size: Int) {
        Spacer(modifier = Modifier.height(size.dp))
    }
}