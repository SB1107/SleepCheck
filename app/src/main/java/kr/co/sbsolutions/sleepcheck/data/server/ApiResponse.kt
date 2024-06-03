package kr.co.sbsolutions.sleepcheck.data.server

sealed class ApiResponse<out T> {
    object Loading: ApiResponse<Nothing>()

    data class Success<out T>(
        val data: T
    ): ApiResponse<T>()

    data class Failure(
        val errorCode: ResultError,
    ): ApiResponse<Nothing>()

    object ReAuthorize: ApiResponse<Nothing>()
}
