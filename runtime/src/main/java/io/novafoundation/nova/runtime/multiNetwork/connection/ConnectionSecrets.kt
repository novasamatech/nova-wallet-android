package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.runtime.BuildConfig

class ConnectionSecrets(private val secretsByName: Map<String, String>): Map<String, String> by secretsByName {

    companion object {

        fun default(): ConnectionSecrets {
            return ConnectionSecrets(mapOf(
                "INFURA_API_KEY" to BuildConfig.INFURA_API_KEY
            ))
        }
    }
}
