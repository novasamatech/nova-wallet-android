package io.novafoundation.nova.runtime.multiNetwork.connection

import android.util.Log
import io.novafoundation.nova.common.utils.formatNamedOrThrow
import io.novafoundation.nova.runtime.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ConnectionSecrets(private val secretsByName: Map<String, String>) : Map<String, String> by secretsByName {

    companion object {

        fun default(): ConnectionSecrets {
            return ConnectionSecrets(
                mapOf(
                    "INFURA_API_KEY" to BuildConfig.INFURA_API_KEY,
                    "DWELLIR_API_KEY" to BuildConfig.DWELLIR_API_KEY
                )
            )
        }
    }
}

fun ConnectionSecrets.saturateUrl(url: String): String? {
    return runCatching { url.formatNamedOrThrow(this) }.getOrNull()
}

fun List<Chain.Node>.saturateNodeUrls(connectionSecrets: ConnectionSecrets): List<NodeWithSaturatedUrl> {
    return mapNotNull { node ->
        val saturatedUrl = connectionSecrets.saturateUrl(node.unformattedUrl) ?: run {
            Log.w("ConnectionSecrets", "Failed to saturate url ${node.unformattedUrl} due to unknown secrets in the url")
            return@mapNotNull null
        }

        NodeWithSaturatedUrl(node, saturatedUrl)
    }
}
