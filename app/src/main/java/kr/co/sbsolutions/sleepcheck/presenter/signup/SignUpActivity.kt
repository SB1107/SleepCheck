package kr.co.sbsolutions.sleepcheck.presenter.signup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import kr.co.sbsolutions.sleepcheck.common.addFlag
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.data.entity.RentalCompanyItemData
import kr.co.sbsolutions.sleepcheck.databinding.ActivitySignUpBinding
import kr.co.sbsolutions.sleepcheck.presenter.components.Components.SoomScaffold
import kr.co.sbsolutions.sleepcheck.presenter.policy.PolicyActivity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {
    private val binding: ActivitySignUpBinding by lazy { ActivitySignUpBinding.inflate(layoutInflater) }
    private var accessToken: String? = null
    private var where: String? = "login"
    private val viewModel: SignUpViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        intent?.let {
            accessToken = it.getStringExtra("accessToken")
            where = it.getStringExtra("where")
        }
        bindViews()
        setObservers()
        viewModel.getCompanyList()
    }

    @SuppressLint("ResourceAsColor")
    private fun bindViews() {
        binding.apply {
            binding.composeView.apply {
                setContent {
                    RootView()
                }
            }

        }
    }

    @Preview
    @Composable
    fun RootView(items: List<RentalCompanyItemData> = emptyList()) {
        var isButtonEnable by remember { mutableStateOf(false) }
        var companyText by remember { mutableStateOf("") }
        var companyCode by remember { mutableStateOf("") }
        var nameText by remember { mutableStateOf("") }
        var birthdayText by remember { mutableStateOf("") }
        var isBirthdayShow by remember { mutableStateOf(false) }

        SoomScaffold(R.drawable.back1, null,stringResource(R.string.signup), topAction = { finish() }, childView = {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Spacer(modifier = Modifier.height(50.dp))
                    Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                        Text(
                            text = "회사명", style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White),
                            fontSize = 18.sp
                        )
                        CompanySpinner(items = items, text = companyText, selectText = { company ->
                            companyText = company.name
                            companyCode = company.code
                            Log.e(TAG, "code 123= ${company.code} ")
                            isButtonEnable = isEnabled(companyCode, nameText, birthdayText)
                        })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                        Text(
                            text = "성명", style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White),
                            fontSize = 18.sp
                        )
                        InputTextField(R.string.name_des, value = nameText, valueChange = { value ->
                            nameText = value
                            isButtonEnable = isEnabled(companyCode, nameText, birthdayText)
                        })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                        Text(
                            text = "생년월일", style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White),
                            fontSize = 18.sp
                        )
                        InputDate(R.string.birth_des, value = birthdayText, click = {
                            isBirthdayShow = true
                        })
                    }
                    if (isBirthdayShow) {
                        CustomDatePickerDialog(
                            selectedDate = birthdayText,
                            onClickCancel = { isBirthdayShow = false },
                            onClickConfirm = {
                                isBirthdayShow = false
                                birthdayText = it
                                isButtonEnable = isEnabled(companyCode, nameText, birthdayText)
                            })
                    }
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonEnable) colorResource(id = R.color.color_yellow) else colorResource(id = R.color.color_777777),
                        contentColor = Color.Black,
                        disabledContainerColor = colorResource(id = R.color.color_777777),
                        disabledContentColor = Color.Black
                    ),

                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 30.dp)
                        .height(50.dp),

                    onClick = {
                        if (companyText.isEmpty()) {
                            showAlertDialog(message = getString(R.string.company))
                            return@Button
                        } else if (nameText.isEmpty()) {
                            showAlertDialog(message = getString(R.string.name_des))
                            return@Button
                        } else if (birthdayText.isEmpty()) {
                            showAlertDialog(message = getString(R.string.birth_des))
                            return@Button
                        } else if (birthdayText.isEmpty()) {
                            showAlertDialog(message = getString(R.string.birth_des2))
                            return@Button
                        }
                        viewModel.signUp(accessToken, companyCode, nameText, birthdayText)
//                            if (etcText.isNotEmpty() && etcTitleText.isNotEmpty() && etcText.length <= 200) {
//                                viewModel.sendDetail(etcTitleText, etcText)
//                            }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.signup),
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black),
                        fontSize = 16.sp
                    )
                }
            }
        })
    }

    @Composable
    fun CompanySpinner(items: List<RentalCompanyItemData>, text: String, selectText: (RentalCompanyItemData) -> Unit) {
        val expanded = rememberSaveable { mutableStateOf(false) }
        val density = LocalDensity.current
        var width by remember { mutableStateOf(0.dp) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(51.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color = Color.White)
                .clickable {
                    expanded.value = !expanded.value
                }
                .onGloballyPositioned { coordinates ->
                    width = with(density) {
                        coordinates.size.width.toDp()
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text.ifEmpty { stringResource(id = R.string.company) },
                    style = TextStyle(fontWeight = FontWeight.Normal, color = if (text.isEmpty()) colorResource(R.color.color_797979) else Color.Black),
                    fontSize = 16.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = colorResource(id = R.color.color_1A447D))
                        .padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dropdown_arrow),
                        contentDescription = "dropdown arrow",
                        tint = Color.White
                    )
                }
            }
        }
        DropdownMenu(
            modifier = Modifier
                .width(width)
                .background(color = Color.White),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(text = item.name)
                    },
                    onClick = {
                        // 항목을 선택했을 때 수행할 작업
                        selectText.invoke(item)
                        expanded.value = false
                    })
            }
        }
    }

    @Composable
    fun InputTextField(stringId: Int, value: String, valueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Done) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(51.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color = Color.White)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = value,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = imeAction, keyboardType = keyboardType),
                onValueChange = valueChange,
                placeholder = {
                    Text(
                        text = stringResource(stringId), style = TextStyle(
                            color = colorResource(
                                id = R.color.color_797979
                            )
                        )
                    )
                },
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                    unfocusedBorderColor = colorResource(id = R.color.color_FFFFFF),
                )
            )
        }
    }

    @Composable
    fun InputDate(stringId: Int, value: String, click: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(51.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color = Color.White)
                .clickable { click.invoke() }, contentAlignment = Alignment.CenterStart
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = value.ifEmpty { stringResource(id = stringId) },
                style = if (value.isEmpty()) TextStyle(color = colorResource(id = R.color.color_797979)) else TextStyle(color = Color.Black),
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomDatePickerDialog(
        selectedDate: String,
        onClickCancel: () -> Unit,
        onClickConfirm: (yyyyMMdd: String) -> Unit
    ) {
        DatePickerDialog(
            onDismissRequest = { onClickCancel() },
            confirmButton = {},
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            val datePickerState = rememberDatePickerState(
                yearRange = IntRange(1900, Calendar.getInstance().get(Calendar.YEAR)),
                initialDisplayMode = DisplayMode.Picker,
                initialSelectedDateMillis = if (selectedDate.isNotEmpty()) {
                    val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    formatter.parse(selectedDate)?.time
                        ?: System.currentTimeMillis() // 날짜 파싱 실패 시 현재 시간을 기본값으로 사용
                } else {
                    System.currentTimeMillis()
                    // selectedDate가 null인 경우 현재 시간을 기본값으로 사용
                },
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        // 날짜 제한 조건
                        return utcTimeMillis <= System.currentTimeMillis()
//                        return  true
                    }
                })

            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors().copy(
                    selectedDayContainerColor = colorResource(id = R.color.color_1A447D),
                    todayDateBorderColor = colorResource(id = R.color.color_1A447D)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.color_1A447D),
                        contentColor = Color.White,
                        disabledContainerColor = colorResource(id = R.color.color_777777),
                        disabledContentColor = Color.Black
                    ),
                    onClick = {
                        onClickCancel()
                    }) {
                    Text(text = "취소")
                }
                Spacer(modifier = Modifier.width(5.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.color_1A447D),
                        contentColor = Color.White,
                        disabledContainerColor = colorResource(id = R.color.color_777777),
                        disabledContentColor = Color.Black
                    ),
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            val yyyyMMdd = SimpleDateFormat(
                                "yyyyMMdd",
                                Locale.getDefault()
                            ).format(Date(selectedDateMillis))

                            onClickConfirm(yyyyMMdd)
                        }
                    }) {
                    Text(text = "확인")
                }
            }
        }
    }

    private fun isEnabled(company: String, name: String, birthday: String): Boolean {
        return (company.isNotEmpty() && name.isNotEmpty() && birthday.isNotEmpty())
    }

    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
//                        Log.e(TAG, "isProgressBar: ${it}")
                        binding.actionProgress.clProgress.visibility =
                            if (it) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.companyList.collectLatest {
                        binding.composeView.apply {
                            setContent {
                                RootView(it)
                            }
                        }
                    }
                }
                launch {
                    viewModel.signUpResult.collectLatest {
                        startActivity(Intent(this@SignUpActivity, PolicyActivity::class.java).putExtra("accessToken", accessToken).addFlag())
                    }
                }
            }
        }
    }
}

