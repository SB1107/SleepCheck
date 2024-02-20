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
            error?.let {
                close(error)
            } ?: token?.let {
                getKAKAOInfo()
            }
        }

        when (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            true -> {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    error?.let {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            close(error)
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } ?: token?.let {
                        getKAKAOInfo()
                    }
                }
            }

            false -> { UserApiClient.instance.loginWithKakaoAccount(context, callback = callback) }

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

enum class SocialType(val typeName: String) {
    GOOGLE("G"), KAKAO("K")
}