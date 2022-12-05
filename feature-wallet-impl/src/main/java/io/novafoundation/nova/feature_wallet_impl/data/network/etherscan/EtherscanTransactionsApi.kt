package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan

import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanResponse
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface EtherscanTransactionsApi {

    suspend fun getOperationsHistory(
        chainId: ChainId,
        baseUrl: String,
        contractAddress: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanAccountTransfer>>
}

class RealEtherscanTransactionsApi(
    private val retrofitApi: RetrofitEtherscanTransactionsApi,
    private val apiKeys: EtherscanApiKeys
) : EtherscanTransactionsApi {

    override suspend fun getOperationsHistory(
        chainId: ChainId,
        baseUrl: String,
        contractAddress: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanAccountTransfer>> {
        val apiKey = apiKeys.keyFor(chainId)

        return retrofitApi.getOperationsHistory(
            baseUrl = baseUrl,
            contractAddress = contractAddress,
            accountAddress = accountAddress,
            pageNumber = pageNumber,
            pageSize = pageSize,
            apiKey = apiKey
        )
    }
}
