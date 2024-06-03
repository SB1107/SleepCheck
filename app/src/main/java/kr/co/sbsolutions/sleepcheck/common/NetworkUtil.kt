package kr.co.sbsolutions.sleepcheck.common

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtil {
    // Return value: CELLULAR | WIFI | UNKNOWN
    fun getTransportName(connectivityManager: ConnectivityManager): String {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return "UNKNOWN"
        var activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return "UNKNOWN"

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "UNKNOWN"
        }
//        } else {
//            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo ?: return false
//            @Suppress("DEPRECATION") return networkInfo.isConnected
//        }
    }

    fun registerNetworkCallback(connectivityManager: ConnectivityManager, callback: ConnectivityManager.NetworkCallback) {
//        val networkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
//            .build()
//        connectivityManager.registerDefaultNetworkCallback(networkRequest, callback)
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    fun unregisterNetworkCallback(connectivityManager: ConnectivityManager, callback: ConnectivityManager.NetworkCallback) {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}