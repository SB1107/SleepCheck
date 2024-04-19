package kr.co.sbsolutions.newsoomirang.presenter.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.common.WebType
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    private val binding: ActivityWebViewBinding by lazy {
        ActivityWebViewBinding.inflate(layoutInflater)
    }
    lateinit var mWebTypeUrl: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        intent?.let {
            mWebTypeUrl = it.getStringExtra("webTypeUrl").toString()
            binding.actionBar.toolbarTitle.text = it.getStringExtra("webTypeTitle").toString()
        }
        initWebView()
        binding.actionBar.backButton.setOnSingleClickListener {
            finish()
        }
    }

    //--------------------------------------------------------------------------------------------
    // MARK : Local functions
    //--------------------------------------------------------------------------------------------
    private fun initWebView() {
        setWebView()
        when (mWebTypeUrl) {
            WebType.TERMS0.url,
            WebType.TERMS1.url -> {
                binding.webView.loadUrl(BuildConfig.SERVER_URL + mWebTypeUrl)
            }

            WebType.TERMS2.url -> {
                binding.webView.loadUrl(mWebTypeUrl)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebView() {
        // 웹뷰 설정
        binding.webView.settings.apply {
            supportZoom() // 확대/축소 기능 활성화
            defaultTextEncodingName = "utf-8"
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            javaScriptEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            builtInZoomControls = true // 확대/축소 버튼 표시
            displayZoomControls = false // 확대/축소 아이콘 표시 안 함
        }

        // 웹뷰 초기화
        binding.webView.apply {
            addJavascriptInterface(AndroidBridge(), "sbsolutions")
            setInitialScale(1) // 화면 크기에 맞게 웹뷰 크기 조절
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
        }

        // 쿠키 설정
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }
    }

    private class AndroidBridge {
        @JavascriptInterface
        fun resultAddress(zonecode: String?, address: String?, lat: String?, lng: String?) {
//      Timber.i("zonecode : " + zonecode + ",address:" + address + ",lat:" + lat + ",lng:" + lng);
        }

        @JavascriptInterface
        fun resultVerification(name: String?, phone: String?, birth: String?, gender: String?) {
//      Timber.i("name : " + name + ",phone:" + phone + ",birth:" + birth + ",gender:" + gender);
//            WebViewActivity.mListener.onAuthResult(name, phone, birth, gender)
//            AppHelper.getInstance().finishActivity()
        }
    }

}

interface WebListener {
    fun onAuthResult(name: String?, phone: String?, birth: String?, gender: String?) { // 본인인증 결과
    }
}
