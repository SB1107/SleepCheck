package kr.co.sbsolutions.newsoomirang.domain

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.data.server.ErrorResponse
import kr.co.sbsolutions.newsoomirang.data.server.ResultError
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

fun<T : BaseEntity> apiRequestFlow(call: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
    emit(ApiResponse.Loading)

    withTimeoutOrNull(20000L) {
        val response = call()

        try {
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    // FIXME: 메세지 회원정보 에러 캐칭
                    if (data.message.contains("에러")) {
                        emit(ApiResponse.ReAuthorize)
                    }else{
                        emit(ApiResponse.Success(data))
                    }

                }
            } else {
                if(response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) {
                    emit(ApiResponse.ReAuthorize)
                }else if (response.code() == HTTP_INTERNAL_ERROR){
                    emit(ApiResponse.ReAuthorize)
                } else {
                    response.errorBody()?.let { error ->
                        error.close()
//                        val parsedError: ErrorResponse = Gson().fromJson(error.string(), ErrorResponse::class.java)
                        val parsedError: ErrorResponse = Gson().fromJson(error.charStream(), ErrorResponse::class.java)
                        emit(ApiResponse.Failure(ResultError.ErrorCustom(parsedError.message)))
                    }
                }
            }
        } catch (e: Exception) {
//            emit(ApiResponse.Failure(e.message ?: e.toString(), "400"))
            emit(ApiResponse.Failure(ResultError.ErrorCustom(e.message ?: e.toString())))
        }
    } ?: emit(ApiResponse.Failure(ResultError.ErrorCustom("연결시간을 초과하였습니다. 다시 시도해주세요.")))
//    emit(ApiResponse.Failure("Timeout! Please try again.", "408"))
}.flowOn(Dispatchers.IO)