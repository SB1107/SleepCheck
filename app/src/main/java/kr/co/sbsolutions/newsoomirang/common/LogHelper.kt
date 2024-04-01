package kr.co.sbsolutions.newsoomirang.common

class LogHelper(private  val logWorkerHelper: LogWorkerHelper) {

    fun  insertLog(logMethod :() -> Unit  ){
        val name = getClazzName(logMethod)
        logWorkerHelper.insertLog("Method : $name ")
    }

    fun insertLog(message : String) {
        logWorkerHelper.insertLog(message)
    }

    private fun  getClazzName(request: () -> Unit): String {
        val regex = Regex("[\\d\\p{Punct}$]")
        val clazzName = regex.replace(request.javaClass.name.split(".").last(), "")

        return if (clazzName.contains("ViewModel")) {
            val index = clazzName.indexOf("ViewModel")
            clazzName.substring(index + "ViewModel".length)
        } else {
            clazzName
        }
    }
}