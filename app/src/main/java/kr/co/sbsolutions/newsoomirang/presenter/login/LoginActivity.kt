package kr.co.sbsolutions.newsoomirang.presenter.login

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.BeginSignInRequest.PasswordRequestOptions
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.databinding.ActivityLoginBinding

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private val mAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var idToken: String
    private val oneTapClientResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    // 인텐트로 부터 로그인 자격 정보를 가져옴
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    // 가져온 자격 증명에서 Google ID 토큰을 추출
                    val googleIdToken = credential.googleIdToken
                    if (googleIdToken != null) {
                        // Google ID 토큰을 사용해 Firebase 인증 자격 증명을 생성
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        // 생성된 Firebase 인증 자격 증명을 사용하여 Firebase 에 로그인을 시도
                        Firebase.auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Firebase.auth.currentUser?.getIdToken(true)?.addOnCompleteListener { idTokenTask ->
                                    if (idTokenTask.isSuccessful) {
                                        idTokenTask.result?.token?.let { token ->
                                            idToken = token
//                                            viewModel.login(idToken)
                                            Log.e(TAG , "idToken")
                                        } ?: Log.e(TAG, "FirebaseIdToken is null.")
                                    }
                                }
                            } else {
//                                Timber.e(task.exception)
                            }
                        }
                    } else {
                        Log.e(TAG, "GoogleIdToken is null.")
                    }
                } catch (exception: ApiException) {
                    Log.e(TAG, exception.localizedMessage)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initGoogleLogin()
        binding.btGoogle.setOnClickListener {
            oneTapClient
                .beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        oneTapClientResult.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (exception: IntentSender.SendIntentException) {
                        Log.e(TAG,"Couldn't start One Tap UI: ${exception.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { exception ->
                    Log.e(TAG,exception.localizedMessage)
                }
        }
    }

    private fun initGoogleLogin() {
        oneTapClient = Identity.getSignInClient(this)
        val passwordRequestOptions = PasswordRequestOptions.builder()
            .setSupported(true)
            .build()

        val googleIdTokenRequestOptions = GoogleIdTokenRequestOptions.builder()
            .setSupported(true)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    // Google Id Token 기반 로그인을 지원하도록 설정
                    .setSupported(true)
                    // 서버의 클라이언트 ID 를 설정
//                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    // 기존에 인증된 계정만을 필터링하지 않도록 설정
                    .setFilterByAuthorizedAccounts(false)
                    .build(),
            )
            // 이전에 선택 했던 계정을 기억
            .setAutoSelectEnabled(true)
            .build()
    }
}