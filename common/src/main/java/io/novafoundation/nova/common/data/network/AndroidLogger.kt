
package io.novafoundation.nova.common.data.network

import android.util.Log
import io.novafoundation.nova.common.data.storage.DebugSettings
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

const val TAG = "AndroidLogger"

class AndroidLogger(
    private val debug: Boolean,
    private val settings: DebugSettings
) : Logger {
    override fun log(message: String?) {
        if (debug && !(settings.nodeLogsDisabled() && isNodeLog(message))) {
            Log.d(TAG, message.toString())
        }
    }

    override fun log(throwable: Throwable?) {
        if (debug) {
            throwable?.printStackTrace()
        }
    }
}

internal fun isNodeLog(message: String?): Boolean {
    val normalized = message?.trimStart() ?: return false
    return normalized.startsWith("[STATE MACHINE]") || normalized.startsWith("[SOCKET]") || normalized.startsWith("[RPC]")
}
