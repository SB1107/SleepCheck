package kr.co.sbsolutions.newsoomirang.presenter.leave

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginActivity

@AndroidEntryPoint
class LeaveActivity : AppCompatActivity() {

    private val viewModel: LeaveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultPreview()
        }

        lifecycleScope.launch {
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.logoutResult.collect {
                        startActivity(Intent(this@LeaveActivity, LoginActivity::class.java))
                    }
                }
            }
            launch {
                viewModel.errorMessage.collectLatest {
                    showAlertDialog(R.string.common_title, it)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun DefaultPreview() {
        var etcText by remember { mutableStateOf("") }
        val maxChars = 100
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentChars = s?.length ?: 0
                // 글자 수 표시
                // ...

                // 225글자 초과 이벤트
                if (currentChars > maxChars) {
                    // 이벤트 보내기
                    // ...
                }
            }
        }
        var checkBoxText by remember { mutableStateOf("") }
        var isButtonEnable by remember { mutableStateOf(false) }
        val radioButtons = remember {
            mutableStateListOf(
                CheckBoxData(
                    isChecked = false,
                    text = "예상했던 서비스가 아니에요.",
                ),
                CheckBoxData(
                    isChecked = false,
                    text = "소리가 불편해요.",
                ),
                CheckBoxData(
                    isChecked = false,
                    text = "효과를 보지 못했어요.",
                ),
                CheckBoxData(
                    isChecked = false,
                    text = "앱의 기능이 부족해요.",
                ),
                CheckBoxData(
                    isChecked = false,
                    text = "자주 사용하지 않아요.",
                ),
                CheckBoxData(
                    isChecked = false,
                    text = "기타",
                ),
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = Color(LocalContext.current.getColor(R.color.color_061629)),
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.setting_general_withdraw),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
            bottomBar = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorResource(id = R.color.color_061629))
                        .padding(bottom = 30.dp)
                        .height(50.dp)
                        .padding(horizontal = 30.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.color_0F63C8),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.Black
                    ),
                    enabled = isButtonEnable,
                    onClick = {
                        if (checkBoxText.isEmpty()) {
                            showAlertDialog(message = "동의사항을 체크해주세요.")
                        }else{
                            if (checkBoxText == "기타") {
                                checkBoxText  = etcText
                            }
                            viewModel.leaveButtonClick(checkBoxText)
                        }
                    }

                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.leave_button),
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(color = colorResource(id = R.color.color_061629))
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                TextHeader("회원탈퇴 전 아래 내용을 확인해주세요.")
                Spacer(modifier = Modifier.height(10.dp))

                LeaveDesText(text = "고객님의 계정에 저장된 정보가 삭제될 예정입니다. 삭제된 정보는 추후에 복원할 수 없습니다.")
                Spacer(modifier = Modifier.height(10.dp))
                LeaveDesText(text = "같은 아이디로 재가입이 불가합니다.")

                Spacer(modifier = Modifier.height(20.dp))
                TextHeader("탈퇴 사유를 선택해주세요.")
                LeaveRadioButtons(radioButtons, click = { data ->
                    checkBoxText = data.text
                    isButtonEnable = checkBoxText.isNotEmpty()
                    radioButtons.replaceAll { it.copy(isChecked = (it.text == data.text)) }
                })
                Spacer(modifier = Modifier.height(10.dp))

                if (checkBoxText == "기타") {
                    Text(
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(horizontal = 30.dp)
                            .fillMaxWidth(),
                        style = TextStyle(color = colorResource(id = R.color.color_FFFFFF)),
                        text = "${etcText.length} / $maxChars 글자 이내로 입력해주세요.",
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 30.dp),
                        value = etcText,
                        onValueChange = { etcText = it },
                        placeholder = {
                            Text(
                                text = "기타 탈퇴 사유를 입력해 주세요. 고객님의 소중한 의견을 반영하여, 더 좋은 서비스로 찾아뵙겠습니다.",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.color_78899F)
                                )
                            )
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(id = R.color.color_0F63C8),
                            unfocusedBorderColor = colorResource(id = R.color.color_0F63C8)
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
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
                        text = data.text, style = TextStyle(color = Color.White, fontSize = 14.sp),
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
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
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