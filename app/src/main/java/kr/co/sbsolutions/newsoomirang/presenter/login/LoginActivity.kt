package kr.co.sbsolutions.newsoomirang.presenter.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.LogWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.addFlag
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.databinding.ActivityLoginBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.newsoomirang.presenter.policy.PolicyActivity
import kr.co.sbsolutions.newsoomirang.presenter.splash.WHERE
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    companion object {
        private const val RC_SIGN_IN = 1234
    }

    private val viewModel: LoginViewModel by viewModels()
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private var mAuth = FirebaseAuth.getInstance()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private lateinit var googleLoginActivityResultLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var logWorkerHelper: LogWorkerHelper


    override fun onStart() {
        super.onStart()
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setGoogle()
        bindView()
        onGoogleLogin()
    }

    private fun bindView() {
        binding.apply {
            btGoogle.setOnClickListener {
                logWorkerHelper.insertLog("login 클릭 사용자가 직접")
                requestGoogleLoginActivation()
            }
            btKakao.setOnClickListener {
                viewModel.socialLogin(SocialType.KAKAO)
            }
        }
        lifecycleScope.launch{
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.whereActivity.collectLatest {
                        when (it) {
                            WHERE.None -> {}
                            WHERE.Login -> {}
                            WHERE.Main -> {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java).addFlag())
                                finish()
                            }

                            WHERE.Policy -> {
                                startActivity(Intent(this@LoginActivity, PolicyActivity::class.java).putExtra("accessToken", viewModel.accessToken).addFlag())
                            }
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(message = it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
                        binding.actionProgress.clProgress.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }

            }
        }
    }

    private fun setGoogle() {
        if (mAuth.currentUser != null) {
            // 로그인된 사용자입니다.
            val uid = mAuth.currentUser?.uid
            Log.d(TAG, "[GOOGLE_ UID]: $uid")
            if (uid == null) {
                Log.d(TAG, "[GOOGLE_ UID_NULL]: $uid")
            }
        } else {
            // 로그인되지 않은 사용자입니다.
        }
        /*gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()*/
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun requestGoogleLoginActivation() {
        if (::googleSignInClient.isInitialized){
            val signInIntent = Intent(googleSignInClient.signInIntent)
            googleLoginActivityResultLauncher.launch(signInIntent)
        }
    }

    private fun onGoogleLogin() {
        googleLoginActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.socialLogin(SocialType.GOOGLE , result.data)
            }
        }
    }
}