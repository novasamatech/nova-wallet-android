package io.novafoundation.nova.feature_account_impl.data.network.blockchain

interface AccountSubstrateSource {

    /**
     * @throws NovaException
     */
    suspend fun getNodeNetworkType(nodeHost: String): String
}
