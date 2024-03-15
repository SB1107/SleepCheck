package kr.co.sbsolutions.newsoomirang.presenter.question.contactDetail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components
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
            topText = "문의하기",
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

        /*Log.d(TAG, "createDate: $createDate")
        Log.d(TAG, "title: $title")
        Log.d(TAG, "Contact: $content")
        Log.d(TAG, "ansContent: $ansContent")
        Log.d(TAG, "ansCreatedAt: $ansCreatedAt")*/

        SpacerHeight(size = 10)
        Row {
            Text(
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f),
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                text = "문의 제목",
            )

            Text(
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(2f),
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                text = "작성일 : $createDate",
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF))
            )
        }

        Text(
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth(),
            style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
            text = "문의 내용",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.color_0F63C8),
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = content,
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)) // 텍스트의 색상을 설정합니다.
            )
        }
        SpacerHeight(size = 20)
        Row {
            Text(
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f),
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                text = "답변 내용",
            )

            Text(
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(2f),
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                text = "답변일 : $ansCreatedAt",
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.color_0F63C8),
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = ansContent,
                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF))
            )
        }
        SpacerHeight(size = 10)
    }

    @Composable
    fun TitleText(text: String, textSize: Int, color: Color = Color.White) {
        Text(
            text = text,
            style = TextStyle(fontWeight = FontWeight.Bold, color = color),
            fontSize = textSize.sp
        )
    }

    @Composable
    fun DetailText(text: String, textSize: Int, color: Color = Color.White) {
        Text(
            text = text,
            style = TextStyle(color = color),
            fontSize = textSize.sp
        )
    }

    @Composable
    fun SpacerHeight(size: Int) {
        Spacer(modifier = Modifier.height(size.dp))
    }
}