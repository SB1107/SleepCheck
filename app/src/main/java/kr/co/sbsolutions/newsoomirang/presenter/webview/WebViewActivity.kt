package kr.co.sbsolutions.newsoomirang.presenter.webview

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
               binding.actionBar.toolbarTitle.text =  it.getStringExtra("webTypeTitle").toString()
            }
        initWebView()
        binding.actionBar.backButton.setOnClickListener {
            finish()
        }
    }

    //--------------------------------------------------------------------------------------------
    // MARK : Local functions
    //--------------------------------------------------------------------------------------------
    private fun initWebView() {
        binding.webView.addJavascriptInterface(AndroidBridge(), "sbsolutions")
        val webSettings = binding.webView.settings
        webSettings.defaultTextEncodingName = "utf-8"
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.useWideViewPort = true
        webSettings.javaScriptEnabled = true
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = WebViewClient()
        binding.webView.setVerticalScrollbarOverlay(true) //스크롤설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //혼합콘텐츠,타사쿠키사용
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(binding.webView, true)
        }
        binding.webView.loadUrl(BuildConfig.SERVER_URL + mWebTypeUrl)
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
