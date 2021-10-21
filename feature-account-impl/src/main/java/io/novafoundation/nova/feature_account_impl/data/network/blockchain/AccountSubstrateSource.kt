package io.novafoundation.nova.feature_account_impl.data.network.blockchain

interface AccountSubstrateSource {

    /**
     * @throws FearlessException
     */
    suspend fun getNodeNetworkType(nodeHost: String): String
}
