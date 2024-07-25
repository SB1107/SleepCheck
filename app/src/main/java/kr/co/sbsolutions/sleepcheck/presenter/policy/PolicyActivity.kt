package kr.co.sbsolutions.sleepcheck.presenter.policy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.WebType
import kr.co.sbsolutions.sleepcheck.common.addFlag
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.toBoolean
import kr.co.sbsolutions.sleepcheck.databinding.ActivityPolicyBinding
import kr.co.sbsolutions.sleepcheck.presenter.main.MainActivity
import kr.co.sbsolutions.sleepcheck.presenter.webview.WebViewActivity
import java.util.Locale

@AndroidEntryPoint
class PolicyActivity : AppCompatActivity() {
    private val binding: ActivityPolicyBinding by lazy {
        ActivityPolicyBinding.inflate(layoutInflater)
    }
    private val viewModel: PolicyViewModel by viewModels()

    private var accessToken: String? = null
    private var where: String? = "login"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        intent?.let {
            accessToken = it.getStringExtra("accessToken")
            where = it.getStringExtra("where")
        }
        bindViews()
    }

    @SuppressLint("ResourceAsColor")
    private fun bindViews() {
        setSystemBarColor(this@PolicyActivity, R.color.color_000000)
        binding.apply {
            actionBar.toolbarTitle.setText(R.string.privacy_title)
            actionBar.appBar.setBackgroundColor(android.R.color.transparent)
            
            
            binding.actionBar.backButton.setOnSingleClickListener { finish() }
            //서비스 이용 약관 보기
            btnServiceTerms.setOnSingleClickListener {
                val locale = resources.configuration.locales[0]
                webViewActivity(WebType.TERMS0, locale)
            }

            //개인 정보 수집 및 이용 정책 보기
            btnPersonalInformationCollection.setOnSingleClickListener {
                val locale = resources.configuration.locales[0]
                webViewActivity(WebType.TERMS1, locale)
            }

            //필수 약관 체크
            cbEssentialTermsService.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.setCheckServerData(isChecked)
//                btnAgree.isEnabled = isChecked
//                Log.d(TAG, "checkServerData : $checkServerData ")
            }

            //일반 약관 체크
            cbConsentUseAppData.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.setCheckAppData(isChecked)
//                Log.d(TAG, "checkAppData: $checkAppData")
            }

            //동의하기 버튼
            btnAgree.setOnSingleClickListener {
                if (!viewModel.checkServerDataFlow.value.toBoolean()) {
                    Toast.makeText(this@PolicyActivity, getString(R.string.policy_r_agree), Toast.LENGTH_SHORT).show()
                    return@setOnSingleClickListener
                }
                viewModel.joinAgree(accessToken)
            }
        }
        lifecycleScope.launch {
            launch {
                if (where != "login" && where != null) {
                    binding.apply {
                        delay(100)
                        cbEssentialTermsService.visibility = View.GONE
                        cbEssentialTermsService.performClick()
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.userName.collect { name ->
                        binding.tvName.text = buildString {
                            append(getString(R.string.policy_message1, name))
                            append(getString(R.string.policy_message2))
                        }
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.checkServerDataFlow.collect {
                        binding.btnAgree.setBackgroundColor(if (it == 0) getColor(R.color.color_999999) else getColor(R.color.color_main))
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.errorMessage.collect {
                        this@PolicyActivity.showAlertDialog(R.string.app_error, it)
                    }
                }
            }
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.policyResult.collect {
                        if (where != "login" && where != null) {
                            finish()
                        } else {
                            startActivity(Intent(this@PolicyActivity, MainActivity::class.java).addFlag())
                            finish()
                        }
                    }
                }
            }
        }
    }



    private fun setSystemBarColor(act: Activity, @ColorRes color: Int) {
        val window = act.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = act.resources.getColor(color)
    }

    private fun webViewActivity(webType: WebType, locale: Locale) {
        startActivity(Intent(this, WebViewActivity::class.java).apply {
            val title = if (locale == Locale.KOREA) webType.titleKo else webType.titleEn
            putExtra("webTypeUrl", webType.url)
            putExtra("webTypeTitle", title)
        })
    }
}
