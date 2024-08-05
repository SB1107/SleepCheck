package kr.co.sbsolutions.sleepcheck.presenter.question.contactUs

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.databinding.ActivityQuestionBinding
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceActivity
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SoomScaffold
import okhttp3.internal.wait

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
        SoomScaffold(topText = stringResource(R.string.setting_qna),
            topAction = { finish() },
            bgColor = Color.Transparent,
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

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            SpacerHeight(size = 10)
            TitleText(text = stringResource(R.string.contact_subject), textSize = 21)
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = etcTitleText,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                onValueChange = titleValueChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.contact_subject_enter), style = TextStyle(
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
                TitleText(text = stringResource(R.string.contact_contents), textSize = 21)
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                    text = stringResource(R.string.contact_character_limits, etcText.length),
                )
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                value = etcText,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                onValueChange = textValueChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.contact_contents_enter), style = TextStyle(
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
        }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.color_main),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),

            onClick = {
                if (etcTitleText.isEmpty()) {
                    showAlertDialog(message = getString(R.string.contact_subject_enter))
                } else if (etcText.isEmpty()) {
                    showAlertDialog(message = getString(R.string.contact_contents_enter))
                } else if (etcText.length > 200) {
                    showAlertDialog(message = getString(R.string.contact_character_limits_message))
                }
                if (etcText.isNotEmpty() && etcTitleText.isNotEmpty() && etcText.length <= 200) {
                    viewModel.sendDetail(etcTitleText, etcText)
                }
            },
        ) {
            DetailText(text = stringResource(R.string.contact_register), textSize = 16, color = Color.White)
        }
        SpacerHeight(size = 15)

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