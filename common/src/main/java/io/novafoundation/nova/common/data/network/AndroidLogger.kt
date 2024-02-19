
package io.novafoundation.nova.common.data.network

import android.util.Log
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

const val TAG = "AndroidLogger"

class AndroidLogger(
    private val debug: Boolean
) : Logger {
    override fun log(message: String?) {
        if (debug) {
            Log.d(TAG, message.toString())
        }
    }

    override fun log(throwable: Throwable?) {
        if (debug) {
            throwable?.printStackTrace()
        }
    }
}
