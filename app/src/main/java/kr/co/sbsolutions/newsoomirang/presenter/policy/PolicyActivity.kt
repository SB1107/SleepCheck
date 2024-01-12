package kr.co.sbsolutions.newsoomirang.presenter.policy

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.WebType
import kr.co.sbsolutions.newsoomirang.common.toBoolean
import kr.co.sbsolutions.newsoomirang.databinding.ActivityPolicyBinding
import kr.co.sbsolutions.newsoomirang.presenter.webview.WebViewActivity

@AndroidEntryPoint
class PolicyActivity : AppCompatActivity() {
    private val binding: ActivityPolicyBinding by lazy {
        ActivityPolicyBinding.inflate(layoutInflater)
    }
    private val viewModel: PolicyViewModel by viewModels()

    lateinit var accessToken: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        intent?.let {
            accessToken = it.getStringExtra("accessToken").toString()
        }
        bindViews()
    }

    private fun bindViews() {
        setSystemBarColor(this@PolicyActivity, R.color.color_000000)
        binding.apply {
            actionBar.toolbarTitle.setText(R.string.privacy_title)
            actionBar.backButton.visibility = View.GONE
            binding.actionBar.backButton.setOnClickListener { finish() }
            //서비스 이용 약관 보기
            btnServiceTerms.setOnClickListener {
                webViewActivity(WebType.TERMS0)
            }

            //개인 정보 수집 및 이용 정책 보기
            btnPersonalInformationCollection.setOnClickListener {
                webViewActivity(WebType.TERMS1)
            }

            //필수 약관 체크
            cbEssentialTermsService.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.setCheckServerData(isChecked)
                btnAgree.isEnabled = isChecked
//                Log.d(TAG, "checkServerData : $checkServerData ")
            }

            //일반 약관 체크
            cbConsentUseAppData.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.setCheckAppData(isChecked)
//                Log.d(TAG, "checkAppData: $checkAppData")
            }

            //동의하기 버튼
            btnAgree.setOnClickListener {
                if (!viewModel.checkServerDataFlow.value.toBoolean()) {
                    Toast.makeText(this@PolicyActivity, "필수 약관을 동의해주세요.", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
//                sns()
            }
        }
        lifecycleScope.launch {
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.userName.collect { name ->
                        binding.tvName.text = buildString {
                            append("${name} 님의 건강")
                            append("데이터는 중요합니다.")
                        }
                    }
                }
            }
        }
    }

    private fun setSystemBarColor(act: Activity, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = act.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = act.resources.getColor(color)
        }
    }

    private fun webViewActivity(webType: WebType) {
        startActivity(Intent(this, WebViewActivity::class.java).apply {
            putExtra("webTypeUrl", webType.url)
        })
    }
}