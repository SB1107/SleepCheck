package kr.co.sbsolutions.newsoomirang.presenter.login

import android.content.Context
import android.content.Intent
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kr.co.sbsolutions.newsoomirang.data.model.SocialTypeModel
import javax.inject.Inject


class KaKaoLoginHelper @Inject constructor(
    private val context: Context
) : SocialLogin {
    override fun login(data: Intent?): Flow<SocialTypeModel> = callbackFlow {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
//            Log.e(TAG, "카카오계정으로 로그인 실패", error)
                close(error)
            } else if (token != null) { 
                getKAKAOInfo()
//            Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
            }
        }
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
//                            Log.d(TAG, "bindView: $error")
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        close(error)
                        return@loginWithKakaoTalk
                    }
                    UserApiClient.instance.loginWithKakaoAccount(context,callback = callback)
                } else if (token != null) {
                    getKAKAOInfo()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
        awaitClose()
    }

    private fun ProducerScope<SocialTypeModel>.getKAKAOInfo() {
        UserApiClient.instance.me { user, error ->
            error?.let {
                close(it)
            }
            user?.let {
                trySend(SocialTypeModel(socialToken = it.id.toString(), socialType = SocialType.KAKAO.typeName, name = it.kakaoAccount?.profile?.nickname.toString()))
            }
            close()
        }
    }
}

interface SocialLogin {
    fun login(data: Intent? = null): Flow<SocialTypeModel>
}
enum class SocialType(val typeName : String) {
    GOOGLE("G") , KAKAO("K")
}