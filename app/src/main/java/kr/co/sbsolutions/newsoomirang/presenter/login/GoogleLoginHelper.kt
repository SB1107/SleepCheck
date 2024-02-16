package kr.co.sbsolutions.newsoomirang.presenter.login

import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.data.model.SocialTypeModel

class GoogleLoginHelper : SocialLogin {
    override fun login(data: Intent?): Flow<SocialTypeModel> = callbackFlow {
        // Google Sign In 결과를 처리합니다.
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            // Google Sign In에 성공했습니다.
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d(Cons.TAG, "[LOGIN] GoogleToken1: $idToken")
//                Log.d(TAG, "[LOGIN] FCM_TOKEN: $fcmToken")
//                Log.d(TAG, "[LOGIN] FIREBASE_UID: ${firebaseAuth.uid}")

            // Firebase Auth에 사용자를 등록합니다.
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                            Log.d(TAG, "onActivityResult: 성공")
                        // 로그인에 성공했습니다.
                        val user = task.result?.user

                        //로그인 API
//                            viewModel.snsAuthenticationLogin(user?.uid.toString(), fcmToken, user?.displayName.toString())
                        Log.d(Cons.TAG, "user: ${user?.uid}")
                        trySend(SocialTypeModel(socialToken = user?.uid.toString(), socialType = SocialType.GOOGLE.typeName, name = user?.displayName.toString()))
                        close()
                    } else {
                        // 로그인에 실패했습니다.
                        task.exception?.let {
//                                Log.e(TAG, "로그인 실패: ${it}")
                            cancel(it.message.toString())
                            close()
                        }
                    }
                }
        } catch (e: ApiException) {
            cancel(e.message.toString())
            close()
            // Google Sign In에 실패했습니다.
//                Log.e(TAG, "Google Sign In 실패: ${e.message}")
        }
        awaitClose()
    }
}