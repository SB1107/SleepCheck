package kr.co.sbsolutions.newsoomirang.presenter.question.contactUs

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.databinding.ActivityQuestionBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.components.Components.SoomScaffold

@AndroidEntryPoint
class ContactUsActivity : BaseServiceActivity() {
    private val viewModel: ContactUsViewModel by viewModels()
    private val binding: ActivityQuestionBinding by lazy {
        ActivityQuestionBinding.inflate(layoutInflater)
    }

    override fun newBackPressed() {
        finish()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root).apply {
            setContent {
                val contactResult by viewModel.contactResultData.collectAsState(initial = BaseEntity())
                DefaultPreview(contactResult)
            }
            setObservers()
        }

        /*lifecycleScope.launch {
            viewModel.contactResultData.collectLatest { message ->
                showAlertDialog(message = message)
            }
        }*/
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun DefaultPreview(baseEntity: BaseEntity = BaseEntity()) {
        var etcText by remember { mutableStateOf("") }
        var etcTitleText by remember { mutableStateOf("") }

        if (baseEntity.success) {
            showAlertDialog(message = baseEntity.message, confirmAction = { newBackPressed() })
            etcText = ""
            etcTitleText = ""
        }
        SoomScaffold(topText = "문의하기",
            topAction = { finish() },
            childView = {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                ) {
                    Log.d(TAG, "DefaultPreview: $etcText")
                    Contact(
                        etcTitleText = etcTitleText,
                        etcText = etcText,
                        titleValueChange = { value -> etcTitleText = value },
                        textValueChange = { value -> etcText = value }
                    )
                }
            }
        )
    }

    //문의하기 화면
    @Composable
    fun Contact(
        etcTitleText: String,
        etcText: String,
        titleValueChange: (String) -> Unit,
        textValueChange: (String) -> Unit
    ) {
        /*Log.d(TAG, "Contact: $etcTitleText $etcText")
        Log.d(TAG, "Contact: $titleValueChange $textValueChange")*/

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            SpacerHeight(size = 10)
            TitleText(text = "문의 제목", textSize = 21)
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = etcTitleText,
                onValueChange = titleValueChange,
                placeholder = {
                    Text(
                        text = "제목을 입력해주세요.", style = TextStyle(
                            color = colorResource(
                                id = R.color.color_dedede
                            )
                        )
                    )
                },
                textStyle = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                    unfocusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                )
            )

            SpacerHeight(size = 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TitleText(text = "문의 내용", textSize = 21)
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                    text = "${etcText.length} / 200 글자 이내로 입력해주세요.",
                )
            }


            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                value = etcText,
                onValueChange = textValueChange,
                placeholder = {
                    Text(
                        text = "문의할 내용을 입력해주세요.", style = TextStyle(
                            color = colorResource(
                                id = R.color.color_dedede
                            )
                        )
                    )
                },
                textStyle = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                    unfocusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                )
            )
            SpacerHeight(size = 60)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    if (etcTitleText.isEmpty()) {
                        showAlertDialog(message = "제목을 입력해주세요.")
                    } else if (etcText.isEmpty()) {
                        showAlertDialog(message = "내용을 입력해주세요.")
                    } else if (etcText.length > 200) {
                        showAlertDialog(message = "200자 이내로 입력해주세요.")
                    }
                    if (etcText.isNotEmpty() && etcTitleText.isNotEmpty() && etcText.length <= 200) {
                        viewModel.sendDetail(etcTitleText, etcText)
                    }
                },
            ) {
                DetailText(text = "문의하기", textSize = 16)
            }
            SpacerHeight(size = 15)
        }
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