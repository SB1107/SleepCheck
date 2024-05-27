package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.domain.model.ScoreModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import org.intellij.lang.annotations.Language
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    dataManager: DataManager,
    tokenManager: TokenManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseViewModel(dataManager, tokenManager) {
    private val _sleepDataDetailData: MutableSharedFlow<SleepDetailResult> = MutableSharedFlow()
    val sleepDataDetailData: SharedFlow<SleepDetailResult> = _sleepDataDetailData.asSharedFlow()

    private val _infoMessage: MutableSharedFlow<Pair<String, String>> = MutableSharedFlow()
    val infoMessage: SharedFlow<Pair<String, String>> = _infoMessage.asSharedFlow()

    private val _scoreInfoMessage: MutableSharedFlow<List<ScoreModel>> = MutableSharedFlow()
    val scoreInfoMessage: SharedFlow<List<ScoreModel>> = _scoreInfoMessage.asSharedFlow()

    private var isSharing = false

    fun getSleepData(id: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getSleepDataDetail(id, language) }
                .collectLatest {
                    it.result?.let { result ->
                        _sleepDataDetailData.emit(result)
                    }
                }
        }
    }

    fun sendInfoMessage(title: String, message: String) {
        viewModelScope.launch {
            _infoMessage.emit(Pair(title, message))
        }
    }

    fun getInfoMessage() {
        viewModelScope.launch {
            _scoreInfoMessage.emit(
                listOf(
                ScoreModel(
                    score = "40", contents = "수면 상태(점수)가 낮은 편입니다.\n" +
                            "전문 병원을 내원하여 상담을 권장합니다. (이비인후과, 호흡기내과,수면 센터 등등)",
                    image = "https://www.sleep.or.kr/custom/196/images/favicon_400x210.png",
                    imageDes = "해당 이미지 선택 해주세요"
                )
            )
            )
        }
    }

    fun sharingImage(context: Context, image: Bitmap?) {
//        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
        if (!isSharing) {
            viewModelScope.launch(Dispatchers.IO) {
                isSharing = true
                val file = File(context.cacheDir, "temp.png")
                val stream = FileOutputStream(file)
                image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val bytes = ByteArrayOutputStream()
                image?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                val path: String = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    image,
                    "수면 기록 공유",
                    "수면 기록을 공유합니다."
                )

                withContext(Dispatchers.Main) {
                    Intent(Intent.ACTION_SEND).apply {
                        setType("image/*")
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
                        val chooser = Intent.createChooser(this, "수면 기록 공유")
                        context.startActivity(chooser)
                        isSharing = false
                    }
                }
            }
        }
    }
}
//

//            ShareClient.instance.uploadImage(file) { imageUploadResult, error ->
//                if (error != null) {
////                    Log.e(TAG, "이미지 업로드 실패", error)
//                    sendErrorMessage("이미지 업로드 실패\n${error.message}")
//                } else if (imageUploadResult != null) {
//                    Log.d(TAG, "sharingKakao: ${imageUploadResult.infos.original.url}")
////                    Log.i(TAG, "이미지 업로드 성공 \n${imageUploadResult.infos.original}")
//                    val makeFeed = FeedTemplate(
//                        content = Content(
//                            title = "수면 기록 공유",
//                            description = "",
//                            imageUrl = imageUploadResult.infos.original.url,
//                            link = Link(
//                                webUrl = imageUploadResult.infos.original.url,
//                                mobileWebUrl = imageUploadResult.infos.original.url
//                            )
//                        ),
//                    )
//                    ShareClient.instance.shareDefault(context, makeFeed) { linkResult, error ->
//                        error?.let {
//                            sendErrorMessage("카카오톡 공유 실패\n${it.message}")
//
//                        }
//                        linkResult?.let {
//                            context.startActivity(it.intent)
//                        }
//                    }
//                }
//            }
//        } else {
//            sendErrorMessage("카카오톡이 설치되어 있지 않습니다. 카카오톡을 설치해주세요.")
//        }
//    }
//}