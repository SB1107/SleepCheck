package kr.co.sbsolutions.sleepcheck.data.server


sealed class ResultCode

sealed class ResultError(var msg: String) : ResultCode() {
    class ErrorCustom(customMsg: String) : ResultError(customMsg)
    object ErrNetwork : ResultError("네트워크 오류")
}