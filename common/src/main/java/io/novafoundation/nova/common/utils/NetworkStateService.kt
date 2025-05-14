package io.novafoundation.nova.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface NetworkStateService {
    val isNetworkAvailable: StateFlow<Boolean>
}

suspend fun <T> NetworkStateService.withNetworkFlatRecover(result: suspend () -> Result<T>) =
    result().flatRecover {
        if (isNetworkAvailable.value) return@flatRecover Result.failure(it)
        isNetworkAvailable.awaitTrue()
        result()
    }

suspend fun <T> NetworkStateService.recoverWithDispatcher(dispatcher: CoroutineDispatcher, result: suspend () -> T) =
    withNetworkFlatRecover {
        runCatching {
            withContext(dispatcher) { result() }
        }
    }

@SuppressLint("MissingPermission")
class RealNetworkStateService(context: Context) : NetworkStateService {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(isNetworkAvailable())
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.value = true
                }

                override fun onLost(network: Network) {
                    _isNetworkAvailable.value = false
                }
            }
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
