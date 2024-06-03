package kr.co.sbsolutions.sleepcheck.domain

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.data.server.ApiResponse
import kr.co.sbsolutions.sleepcheck.data.server.ErrorResponse
import kr.co.sbsolutions.sleepcheck.data.server.ResultError
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

fun <T : BaseEntity> apiRequestFlow(call: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
    emit(ApiResponse.Loading)

    withTimeoutOrNull(20000L) {
        val response = call()

        try {
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    // FIXME: 메세지 회원정보 에러 캐칭
                    if (data.message.contains("에러")) {
                        emit(ApiResponse.ReAuthorize)
                    } else {
                        emit(ApiResponse.Success(data))
                    }

                }
            } else {
                if (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) {
                    emit(ApiResponse.ReAuthorize)
                } else {
                    response.errorBody()?.let { error ->
                        val parsedError = if (error.contentType()?.subtype == "html") {
                            ErrorResponse(message = "서버  통신  에러\n 조금 뒤에 시도해 주세요", success = false, code = response.code().toString())
                        } else {
                            Gson().fromJson(error.charStream(), ErrorResponse::class.java)
                        }
//
                        emit(ApiResponse.Failure(ResultError.ErrorCustom(parsedError.message)))
                        error.close()
                    }
                }
            }
        } catch (e: Exception) {
//            emit(ApiResponse.Failure(e.message ?: e.toString(), "400"))
            emit(ApiResponse.Failure(ResultError.ErrorCustom(e.message ?: e.toString())))
        }
    } ?: emit(ApiResponse.Failure(ResultError.ErrorCustom("네트워크 연결이 원할하지 않습니다. 확인후  다시 시도해주세요.")))
//    emit(ApiResponse.Failure("Timeout! Please try again.", "408"))
}.flowOn(Dispatchers.IO)


fun <T > apiDownloadRequestFlow(call: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
    emit(ApiResponse.Loading)

    withTimeoutOrNull(20000L) {
        val response = call()

        try {
            if (response.isSuccessful) {
                response.body()?.let { data ->

                        emit(ApiResponse.Success(data))
                }
            } else {
                if (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) {
                    emit(ApiResponse.ReAuthorize)
                } else {
                    response.errorBody()?.let { error ->
                            ErrorResponse(message = "서버  통신  에러\n 조금 뒤에 시도해 주세요", success = false, code = response.code().toString())
                        emit(ApiResponse.Failure(ResultError.ErrorCustom(error.toString())))
                        error.close()
                    }
                }
            }
        } catch (e: Exception) {
//            emit(ApiResponse.Failure(e.message ?: e.toString(), "400"))
            emit(ApiResponse.Failure(ResultError.ErrorCustom(e.message ?: e.toString())))
        }
    } ?: emit(ApiResponse.Failure(ResultError.ErrorCustom("네트워크 연결이 원할하지 않습니다. 확인후  다시 시도해주세요.")))
//    emit(ApiResponse.Failure("Timeout! Please try again.", "408"))
}.flowOn(Dispatchers.IO)