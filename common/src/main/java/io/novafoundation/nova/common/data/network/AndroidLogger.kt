
package io.novafoundation.nova.common.data.network

import android.util.Log
import jp.co.soramitsu.fearless_utils.wsrpc.logging.Logger

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
