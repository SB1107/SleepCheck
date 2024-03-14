package kr.co.sbsolutions.newsoomirang.presenter.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.addFlag
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.databinding.ActivityLoginBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.newsoomirang.presenter.policy.PolicyActivity
import kr.co.sbsolutions.newsoomirang.presenter.splash.WHERE

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

    private lateinit var gso: GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setGoogle()
        bindView()
    }

    private fun bindView() {
        binding.apply {
            btGoogle.setOnClickListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
            btKakao.setOnClickListener {
                viewModel.socialLogin(SocialType.KAKAO)
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.whereActivity.collectLatest {
                        when (it) {
                            WHERE.None -> {}
                            WHERE.Login -> {}
                            WHERE.Main -> {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java).addFlag())
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
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            viewModel.socialLogin(SocialType.GOOGLE , data)
        }
    }
}