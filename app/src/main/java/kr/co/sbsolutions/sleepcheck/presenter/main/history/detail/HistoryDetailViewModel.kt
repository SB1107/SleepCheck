package kr.co.sbsolutions.sleepcheck.presenter.main.history.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.entity.ScoreResultData
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDetailResult
import kr.co.sbsolutions.sleepcheck.data.model.SleepDetailDTO
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    dataManager: DataManager,
    tokenManager: TokenManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseViewModel(dataManager, tokenManager) {
    private val _sleepDataDetailData: MutableSharedFlow<SleepDetailDTO> = MutableSharedFlow()
    val sleepDataDetailData: SharedFlow<SleepDetailDTO> = _sleepDataDetailData.asSharedFlow()

    private val _infoMessage: MutableSharedFlow<Pair<String, String>> = MutableSharedFlow()
    val infoMessage: SharedFlow<Pair<String, String>> = _infoMessage.asSharedFlow()

    private val _scoreInfoMessage: MutableSharedFlow<ScoreResultData> = MutableSharedFlow()
    val scoreInfoMessage: SharedFlow<ScoreResultData> = _scoreInfoMessage.asSharedFlow()

    private val _connectLink: MutableSharedFlow<String> = MutableSharedFlow()
    val connectLink: SharedFlow<String> = _connectLink.asSharedFlow()

    private var isSharing = false
    private  var dataId : String = ""
    fun getSleepData(id: String, language: String) {
        this.dataId = id
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getSleepDataDetail(id, language) }
                .collectLatest {
                    it.result?.let { result ->
                        val newData = SleepDetailDTO(id  = result.id, userId = result.userId, number = result.number , dirName = result.dirName,
                            fileName = result.fileName, asleepTime = result.asleepTime, type = result.type, snoreTime = result.snoreTime,
                            apneaState = result.apneaState, apneaCount = result.apneaCount, apnea10 = result.apnea10, apnea30 = result.apnea30,
                            apnea60 = result.apnea60, straightPositionTime = result.straightPositionTime, leftPositionTime = result.leftPositionTime,
                            rightPositionTime = result.rightPositionTime, downPositionTime = result.downPositionTime, wakeTime = result.wakeTime,
                            straightPer = result.straightPer, leftPer = result.leftPer, rightPer = result.rightPer, downPer = result.downPer,
                            wakePer = result.wakePer, sleepPattern = result.sleepPattern, startedAt = result.startedAt,
                            endedAt = result.endedAt, sleepTime = result.sleepTime, state = result.state, deepSleepTime = result.deepSleepTime,
                            moveCount = result.moveCount, remSleepTime = result.remSleepTime, lightSleepTime = result.lightSleepTime,
                            fastBreath = result.fastBreath, slowBreath = result.slowBreath, unstableBreath = result.unstableBreath,
                            avgNormalBreath = result.avgNormalBreath, normalBreathTime = result.normalBreathTime, description = result.description,
                            avgFastBreath = result.avgFastBreath, avgSlowBreath = result.avgSlowBreath, snoreCount = result.snoreCount,
                            coughCount = result.coughCount, breathScore = result.breathScore, snoreScore = result.snoreScore,
                            ment = result.ment, unstableIdx = result.unstableIdx?.split(",") ?: emptyList(),
                            nobreath_idx = result.nobreath_idx?.split(",") ?: emptyList(),
                            snoring_idx = result.snoring_idx?.split(",") ?: emptyList(),
                            supineIdx = result.supineIdx?.split(",") ?: emptyList(),
                            leftIdx = result.leftIdx?.split(",") ?: emptyList(),
                            rightIdx = result.rightIdx?.split(",") ?: emptyList(),
                            proneIdx = result.proneIdx?.split(",") ?: emptyList(),
                            remIdx = result.remIdx?.split(",") ?: emptyList(),
                            lightIdx = result.lightIdx?.split(",") ?: emptyList(),
                            deepIdx = result.deepIdx?.split(",") ?: emptyList(),
                            movement = result.movement?.split(",") ?: emptyList(),
                        )
                        _sleepDataDetailData.emit(newData)
                    }
                }
        }
    }

     fun getLink(){
        viewModelScope.launch(Dispatchers.IO) {

            request { authDataSource.getConnectLink(dataId = dataId.toInt()) }
                .collectLatest { data ->
                    data.result?.let {
                        _connectLink.emit(it.url)
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

    fun sharingImage(context: Context, image: ImageBitmap) {
//        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
        if (!isSharing) {
            viewModelScope.launch(Dispatchers.IO) {
                isSharing = true
//                val file = File(context.cacheDir, "temp.png")
//                val stream = FileOutputStream(file)
//                image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
//                stream.close()

//                val bytes = ByteArrayOutputStream()
//                image?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                val path: String = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    image.asAndroidBitmap(),
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