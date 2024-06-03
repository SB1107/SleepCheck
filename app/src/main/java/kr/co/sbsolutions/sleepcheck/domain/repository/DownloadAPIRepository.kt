package kr.co.sbsolutions.sleepcheck.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kr.co.sbsolutions.sleepcheck.data.api.DownloadServiceAPI
import okhttp3.ResponseBody
import javax.inject.Inject

class DownloadAPIRepository @Inject constructor(private val api: DownloadServiceAPI) : RemoteDownload {

    override fun getDownloadZipFile(path: String , fileName : String): Flow<ResponseBody> = flow{
        val response = api.getDownloadFile(path,fileName)
        if (response.isSuccessful) {
            response.body()?.let { emit(it) }
        }
    }
}