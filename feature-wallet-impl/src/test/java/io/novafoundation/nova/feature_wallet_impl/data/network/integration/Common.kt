package io.novafoundation.nova.feature_wallet_impl.data.network.integration

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

class StdoutLogger : Logger {
    override fun log(message: String?) {
        println(message)
    }

    override fun log(throwable: Throwable?) {
        throwable?.printStackTrace()
    }
}

class NoOpLogger: Logger {
    override fun log(message: String?) {
        // pass
    }

    override fun log(throwable: Throwable?) {
        // pass
    }
}
