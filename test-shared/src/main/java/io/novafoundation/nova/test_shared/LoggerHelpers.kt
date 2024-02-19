package io.novafoundation.nova.test_shared

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

object StdoutLogger : Logger {
    override fun log(message: String?) {
        println(message)
    }

    override fun log(throwable: Throwable?) {
        throwable?.printStackTrace()
    }
}

object NoOpLogger : Logger {
    override fun log(message: String?) {
        // pass
    }

    override fun log(throwable: Throwable?) {
        // pass
    }
}
