package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.ScoreData
import kr.co.sbsolutions.newsoomirang.data.entity.ScoreResultData
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
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

    private val _scoreInfoMessage: MutableSharedFlow<ScoreResultData> = MutableSharedFlow()
    val scoreInfoMessage: SharedFlow<ScoreResultData> = _scoreInfoMessage.asSharedFlow()

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

    fun getInfoMessage(score: String, type: Int, language: String) {
        val tempType: String = if (type == 0) "a" else "s"
        viewModelScope.launch {
//            _scoreInfoMessage.emit(ScoreResultData(
//                data = listOf(ScoreData(title = "이미지 베너" ,
//                    link = "https://www.naver.com/" ,
//                    image = "https://wimg.mk.co.kr/news/cms/202311/03/news-p.v1.20231103.d4ee94d7f4ab4ea887536033551298ae.png"),
//                    ScoreData(title = "이미지 베너" ,
//                        link = "https://www.naver.com/" ,
//                        image = "https://wimg.mk.co.kr/news/cms/202311/03/news-p.v1.20231103.d4ee94d7f4ab4ea887536033551298ae.png"))
//            , msg = "상태가 좋아요 !!@3123"))

            request { authDataSource.getScoreMsg(score, tempType, language) }
                .collectLatest {
                    it.result?.let { data ->
                        _scoreInfoMessage.emit(data.copy(score = score.toInt()))
                    }
                }
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

//                val bytes = ByteArrayOutputStream()
//                image?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
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