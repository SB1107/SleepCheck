package kr.co.sbsolutions.newsoomirang.presenter.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.databinding.ActivityLoginBinding

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

    private fun bindView(){
        binding.apply {
            btGoogle.setOnClickListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
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
            // Google Sign In 결과를 처리합니다.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In에 성공했습니다.
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                Log.d(TAG, "[LOGIN] GoogleToken1: $idToken")
//                Log.d(TAG, "[LOGIN] FCM_TOKEN: $fcmToken")
//                Log.d(TAG, "[LOGIN] FIREBASE_UID: ${firebaseAuth.uid}")

                // Firebase Auth에 사용자를 등록합니다.
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                mAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // 로그인에 성공했습니다.
                            val user = task.result?.user

                            //로그인 API
//                            viewModel.snsAuthenticationLogin(user?.uid.toString(), fcmToken, user?.displayName.toString())
                            viewModel.login("G", user?.uid.toString(),)
                            Log.d(TAG, "user: ${user?.uid}")

                        } else {
                            // 로그인에 실패했습니다.
                            task.exception?.let {
                                Log.e(TAG, "로그인 실패: ${it}")
                            }
                        }
                    }
            } catch (e: ApiException) {
                // Google Sign In에 실패했습니다.
                Log.e(TAG, "Google Sign In 실패: ${e.message}")
            }
        }
    }
}