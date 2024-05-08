package kr.co.sbsolutions.newsoomirang.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.presenter.main.ImageViewPagerAdapter
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data_store")
val Context.tokenStore: DataStore<Preferences> by preferencesDataStore(name = "token_store")
val Context.moveStore: DataStore<Preferences> by preferencesDataStore(name = "moveStore")
fun ContextWrapper.getPermissionResult(): ArrayList<String> {
    val deniedPermissions = arrayListOf<String>()

    Cons.PERMISSIONS_ALL.forEach { permission ->
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            deniedPermissions.add(permission)
        }
    }

    Cons.PERMISSIONS_30.forEach { permission ->
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            deniedPermissions.add(permission)
        }
    }

    Cons.PERMISSIONS_31.forEach { permission ->
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            deniedPermissions.add(permission)
        }
    }

    Cons.PERMISSIONS_33.forEach { permission ->
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            deniedPermissions.add(permission)
        }
    }

    return deniedPermissions
}

fun Context.showYearDialog(currentYear : Int,  cancelAction: (() -> Unit)? = null, confirmAction: ((Int) -> Unit)){
    val maxYear : Int = LocalDate.now().year
    val minYear = 2020
    val dialogView = LayoutInflater.from(this).inflate(R.layout.show_alert_year_dialog, null)
    val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        .setView(dialogView)
        .setCancelable(true)
        .create()
    with(dialogView) {
        val piker = findViewById<NumberPicker>(R.id.picker_year)
        piker.minValue = minYear
        piker.maxValue = maxYear
        piker.value = currentYear
        findViewById<Button>(R.id.btnConfirm).apply {
            setOnSingleClickListener {
                confirmAction.invoke(piker.value)
                dialog.dismiss()
            }
        }
        findViewById<Button>(R.id.btnCancel).apply {
            setOnSingleClickListener {
                cancelAction?.invoke()
                dialog.dismiss()
            }
        }
    }

    dialog.show()
}

fun Context.showAlertDialog(title: Int? = null, message: String, buttonText: Int = R.string.app_confirm, cancelable: Boolean = true, confirmAction: (() -> Unit)? = null) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.show_alert_dialog, null)
    val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        .setView(dialogView)
        .setCancelable(cancelable)
        .create()

    with(dialogView) {
        findViewById<TextView>(R.id.tvDialogContent).text = message
        title?.let {
            findViewById<TextView>(R.id.tvDialogTitle).setText(it)
        }
        findViewById<Button>(R.id.btnConfirm).apply {
            setText(buttonText)
            setOnSingleClickListener {
                confirmAction?.invoke()
                dialog.dismiss()
            }
        }
    }

    dialog.show()
}

fun Context.showAlertDialogWithCancel(
    title: Int? = null,
    message: String,
    cancelButtonText: Int = R.string.app_cancel,
    cancelAction: (() -> Unit)? = null,
    confirmButtonText: Int = R.string.app_confirm,
    confirmAction: (() -> Unit),
    cancelable: Boolean = true
) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.show_alert_dialog_with_cancel, null)
    val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        .setView(dialogView)
        .setCancelable(cancelable)
        .create()

    with(dialogView) {
        findViewById<TextView>(R.id.tvDialogContent).text = message
        title?.let {
            findViewById<TextView>(R.id.tvDialogTitle).setText(it)
        }
        findViewById<Button>(R.id.btnConfirm).apply {
            setText(confirmButtonText)
            setOnSingleClickListener {
                confirmAction.invoke()
                dialog.dismiss()
            }
        }
        findViewById<Button>(R.id.btnCancel).apply {
            setText(cancelButtonText)
            setOnSingleClickListener {
                cancelAction?.invoke()
                dialog.dismiss()
            }
        }
    }
    dialog.show()
}

@SuppressLint("CutPasteId", "MissingInflatedId")
fun Context.guideAlertDialog(confirmAction: ((isChecked: Boolean) -> Unit)? = null, ) : AlertDialog {
    val imageViewPagerAdapter = ImageViewPagerAdapter(listOf(R.drawable.guide1,R.drawable.guide2))
    val dialogView = LayoutInflater.from(this).inflate(R.layout.row_app_guide, null)
    val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        .setView(dialogView)
        .setCancelable(true)
        .create()

    var currentPageIndex = 0

    val checkBox = dialogView.findViewById<AppCompatCheckBox>(R.id.cb_1)
    dialogView.findViewById<ViewPager2>(R.id.vp_2).adapter = imageViewPagerAdapter


    //set the orientation of the viewpager using ViewPager2.orientation
    dialogView.findViewById<ViewPager2>(R.id.vp_2).orientation = ViewPager2.ORIENTATION_HORIZONTAL

    //select any page you want as your starting page
    val guideTitle = dialogView.findViewById<AppCompatTextView>(R.id.tv_guide_title)

   val job = CoroutineScope(Dispatchers.Default).launch{
        while (true){
            delay(1000)

            withContext(Dispatchers.Main){
                if (currentPageIndex == imageViewPagerAdapter.itemCount ){
                    currentPageIndex = 0
                    guideTitle.text = getString(R.string.guide_device_info_message1)
                } else {
                    currentPageIndex++
                    guideTitle.text = getString(R.string.guide_device_info_message2)
                }
                dialogView.findViewById<ViewPager2>(R.id.vp_2).currentItem = currentPageIndex
            }
        }
    }
    // registering for page change callback
    dialogView.findViewById<ViewPager2>(R.id.vp_2).registerOnPageChangeCallback(
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

            }
        }
    )

    with(dialogView) {
        findViewById<Button>(R.id.btn_3).apply {
            setOnSingleClickListener {
                job.cancel()
                confirmAction?.invoke(checkBox.isChecked)
                dialog.dismiss()
            }
        }
    }
    return  dialog
}


fun Context.showAlertDialogWithToolTip() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.tooltip_dialog, null)
    val imageView = dialogView.findViewById<ImageView>(R.id.imgToolTip)
    val dialog = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCancelable(true)
        .create()

    Glide.with(this)
        .load(R.mipmap.ic_launcher)
        .into(imageView)

    dialogView.setOnSingleClickListener {
        dialog.dismiss()
    }
    dialog.show()
}

fun Boolean.booleanToInt(): Int {
    return if (this) 1 else 0
}

fun Int.toBoolean(): Boolean {
    return this == 1
}

fun String.toDate(format: String): Date? {
    val simpleDateFormat = SimpleDateFormat(format, Locale.KOREA)
    return simpleDateFormat.parse(this)
}

fun Date.toDayString(format: String): String? {
    if (format.isEmpty()) {
        return null
    }
    val simpleDateFormat = SimpleDateFormat(format, Locale.KOREA)
    return simpleDateFormat.format(this)
}

fun Int.InpuMintoHourMinute() : String{
    val time = Duration.ofMinutes(this.toLong())
    val hours = time.toHours() // 전체 시간을 시간 단위로 추출

    val minutes = (time.toMinutes() % 60) // 전체 시간에서 시간 단위를 제외한 나머지 분 단위 추출
    return if (hours > 0) {
        String.format("%d시간 %d분", hours, minutes)
    } else {
        String.format("%d 분",minutes)
    }
}
fun Int.toHourMinute(): String {
    val time = Duration.ofSeconds(this.toLong())
    val hours = time.toHours() // 전체 시간을 시간 단위로 추출

    val minutes = (time.toMinutes() % 60) // 전체 시간에서 시간 단위를 제외한 나머지 분 단위 추출
    return if (hours > 0) {
        String.format("%d시간 %d분", hours, minutes)
    } else {
        String.format("%d 분",minutes)
    }
}
fun Int.toHourOrMinute(): String {
    val time = Duration.ofMinutes(this.toLong())
    val hours = time.toHours() // 전체 시간을 시간 단위로 추출

    val minutes = (time.toMinutes() % 60) // 전체 시간에서 시간 단위를 제외한 나머지 분 단위 추출
    return if (hours > 0) {
        String.format("약\n%d시간", hours)
    } else {
        String.format("약\n%d 분",minutes)
    }
}

fun Context.toDp2Px(dp: Float): Float {
    return dp * (this.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.toPx2Dp(px: Float): Float {
    return px / (this.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Intent.addFlag() = this.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
fun ContextWrapper.isIgnoringBatteryOptimizations() = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
// 배터리 최적화 이슈로 인해 현재 처리하지 않음
//fun ContextWrapper.isIgnoringBatteryOptimizations() = true

fun String.hexToBytes(): ByteArray {
    val len = this.length / 2
    val data = ByteArray(len)
    for (i in 0 until len) {
        data[i] = ((Character.digit(this[i * 2], 16) shl 4) or
                Character.digit(this[i * 2 + 1], 16)).toByte()
    }
    return data
}

fun ByteArray.hexToString(): String {
    val strBuilder = StringBuffer()
    strBuilder.append("[ ")
    for (v in this) {
        strBuilder.append(String.format("%02X ", v))
    }
    strBuilder.append("]\n")
    return strBuilder.toString()
}

fun String.prefixToHex(): String {
    return this.replace("[", "").replace("]", "").replace(" ", "").substring(0, 8)
}

fun String.getChangeDeviceName(): String {
    val nameCheck = this.contains("H")
    val bleNumber = this.split("-").last()
    val resultName = if (!nameCheck)"Soomirang - $bleNumber" else "HSMD - $bleNumber"
    return resultName
}

fun View.setOnSingleClickListener(onClickListener: (view: View) -> Unit) {
    setOnClickListener(OnSingleClickListenerHelper(onClickListener))
}

fun Any.timeStamp(dateFormat: String = "yyyy년 MM월 dd일 HH시 mm분 SS.sss초"): String {
    val timeStamp = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(System.currentTimeMillis()))
    Log.d(TAG, "시간: $timeStamp ")
    return timeStamp
}

fun hasUpdate(currentVer: String, compareVer: String): Boolean {
    val curVer = currentVer.split(".").map { it.toInt() }
    val comVer = compareVer.split(".").map { it.toInt() }
    
    // 메이저 버전 비교
    if (comVer[0] > curVer[0]) {
        return true
    }
    // 마이너 버전 비교
    else if (comVer[1] > curVer[1]) {
        return true
    }
    // 패치 버전 비교
    else if (comVer[2] > curVer[2]) {
        return true
    }
    // 버전이 같음
    return false
}


