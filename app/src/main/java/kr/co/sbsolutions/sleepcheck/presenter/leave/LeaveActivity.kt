package kr.co.sbsolutions.sleepcheck.presenter.leave

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.sleepcheck.presenter.login.LoginActivity

@AndroidEntryPoint
class LeaveActivity : AppCompatActivity() {

//    private val viewModel: LeaveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultPreview()
        }

//        lifecycleScope.launch {
//            launch {
//                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                    viewModel.logoutResult.collect {
//                        startActivity(Intent(this@LeaveActivity, LoginActivity::class.java))
//                    }
//                }
//            }
//            launch {
//                viewModel.errorMessage.collectLatest {
//                    showAlertDialog(R.string.common_title, it)
//                }
//            }
//        }
    }

    @Preview
    @Composable
    fun DefaultPreview(vm: LeaveViewModel = hiltViewModel()) {
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle(initialValue = null)
        val logoutResult by vm.logoutResult.collectAsStateWithLifecycle(initialValue = false)
        errorMessage?.let {
            showAlertDialog(R.string.common_title, it)
        }
        if (logoutResult) {
            startActivity(Intent(LocalContext.current, LoginActivity::class.java))
        }

        var etcText by remember { mutableStateOf("") }
        val maxChars = 100
        var checkBoxText by remember { mutableStateOf("") }
        var isButtonEnable by remember { mutableStateOf(false) }
        var isAgreeEnable by remember { mutableStateOf(false) }

        val radioButtons = remember {
            mutableStateListOf(
                CheckBoxData(
                    isChecked = false,
                    text = getString(R.string.leave_reasons_unsubscribing1),
                ),
                CheckBoxData(
                    isChecked = false,
                    text = getString(R.string.leave_reasons_unsubscribing2),
                ),
                CheckBoxData(
                    isChecked = false,
                    text = getString(R.string.leave_reasons_unsubscribing3),
                ),
                CheckBoxData(
                    isChecked = false,
                    text = getString(R.string.leave_reasons_unsubscribing4),
                ),
                CheckBoxData(
                    isChecked = false,
                    text = getString(R.string.leave_reasons_unsubscribing5),
                ),
            )
        }

        SoomScaffold(bgImage = R.drawable.back1,
            topText = stringResource(R.string.leave_title_unsubscribing), topAction = { finish() },
            childView =
            {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(8f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        TextHeader(stringResource(R.string.leave_unsubscribing_info))
                        Spacer(modifier = Modifier.height(10.dp))

                        LeaveDesText(text = stringResource(R.string.leave_unsubscribing_info_delete_data))
                        Spacer(modifier = Modifier.height(10.dp))
//                        LeaveDesText(text = "같은 아이디로 재가입이 불가합니다.")

                        Spacer(modifier = Modifier.height(20.dp))
                        TextHeader(stringResource(R.string.leave_unsubscribing_choice_reason))
                        LeaveRadioButtons(radioButtons, click = { data ->
                            checkBoxText = data.text
                            isButtonEnable = checkBoxText.isNotEmpty()
                            radioButtons.replaceAll { it.copy(isChecked = (it.text == data.text)) }
                        })
                        Spacer(modifier = Modifier.height(10.dp))

                        if (checkBoxText == stringResource(R.string.leave_etc)) {
                            Text(
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .padding(horizontal = 30.dp)
                                    .fillMaxWidth(),
                                style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                                text = stringResource(R.string.leave_character_limits, etcText.length, maxChars),
                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(189.dp)
                                    .padding(horizontal = 30.dp)
                                    .background(color = colorResource(id = R.color.white)),
                                shape = RoundedCornerShape(10.dp),
                                value = etcText,
                                onValueChange = {
                                    if (it.length <= maxChars) {
                                        etcText = it
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.leave_farewell_messages),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            color = colorResource(id = R.color.color_282828)
                                        )
                                    )
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorResource(id = R.color.color_0F63C8),
                                    unfocusedBorderColor = colorResource(id = R.color.color_0F63C8)
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 30.dp)
                            .fillMaxWidth()
                            .clickable {
                                isAgreeEnable = !isAgreeEnable
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            modifier = Modifier.height(50.dp),
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = Color.White,
                                checkedColor = colorResource(id = R.color.color_0F63C8),
                                uncheckedColor = Color.White
                            ),
                            checked = isAgreeEnable,
                            onCheckedChange = { isAgree ->
                                isAgreeEnable = isAgree
                            },
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            text = stringResource(R.string.leave_unsubscribing_select_agree), style = TextStyle(color = Color.White, fontSize = 19.sp),
                            textAlign = TextAlign.Start
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp)
                            .height(50.dp)
                            .padding(horizontal = 30.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.color_yellow),
                            contentColor = Color.Black,
                            disabledContainerColor = colorResource(id = R.color.color_777777),
                            disabledContentColor = Color.Black
                        ),
                        enabled = isButtonEnable and isAgreeEnable,
                        onClick = {
                            if (checkBoxText.isEmpty()) {
                                showAlertDialog(message = getString(R.string.leave_unsubscribing_check_confrim))
                            } else {
                                if (checkBoxText == getString(R.string.leave_etc)) {
                                    checkBoxText = etcText
                                }
                                vm.leaveButtonClick(checkBoxText)
                            }
                        }

                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = LocalContext.current.getString(R.string.leave_button),
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            })
    }


    @Composable
    private fun LeaveRadioButtons(radioButtons: List<CheckBoxData>, click: (CheckBoxData) -> Unit) {

        Column {
            Spacer(modifier = Modifier.height(10.dp))
            radioButtons.forEachIndexed { index, data ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth()
                        .clickable {
                            click(data)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        modifier = Modifier.height(50.dp),
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color.White,
                            checkedColor = colorResource(id = R.color.color_0F63C8),
                            uncheckedColor = Color.White
                        ),
                        checked = data.isChecked,
                        onCheckedChange = {
                            click(data)
                        },
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .padding(top = 5.dp),
                        text = data.text, style = TextStyle(color = Color.White, fontSize = 19.sp),
                        textAlign = TextAlign.Start
                    )
                }

            }
        }
    }

    @Composable
    private fun TextHeader(text: String) {
        Text(
            modifier = Modifier.padding(horizontal = 30.dp),
            text = text,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Color.White)
        )
    }

    @Composable
    fun LeaveDesText(text: String) {
        Row(modifier = Modifier.padding(horizontal = 30.dp)) {
            Text(text = "·", style = TextStyle(color = colorResource(id = R.color.color_78899F)))
            Text(text = text, style = TextStyle(color = colorResource(id = R.color.color_78899F)))
        }
    }

    data class CheckBoxData(
        val isChecked: Boolean,
        val text: String
    )
}