package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.bumptech.glide.Glide
import kr.co.sbsolutions.newsoomirang.R
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data_store")
val Context.tokenStore: DataStore<Preferences> by preferencesDataStore(name = "token_store")
fun ContextWrapper.getPermissionResult(): ArrayList<String> {
    val deniedPermissions = arrayListOf<String>()

    Cons.PERMISSIONS_ALL.forEach { permission ->
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
            setOnClickListener {
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
            setOnClickListener {
                confirmAction.invoke()
                dialog.dismiss()
            }
        }
        findViewById<Button>(R.id.btnCancel).apply {
            setText(cancelButtonText)
            setOnClickListener {
                cancelAction?.invoke()
                dialog.dismiss()
            }
        }
    }
    dialog.show()
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

    dialogView.setOnClickListener {
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
    val simpleDateFormat = SimpleDateFormat(format, Locale.KOREA)
    return simpleDateFormat.format(this)
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