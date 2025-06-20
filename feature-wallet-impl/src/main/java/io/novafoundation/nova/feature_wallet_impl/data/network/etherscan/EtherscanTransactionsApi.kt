package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan

import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanNormalTxResponse
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.etherscan.EtherscanApiKeys
import io.novafoundation.nova.runtime.network.etherscan.model.EtherscanResponse

interface EtherscanTransactionsApi {

    suspend fun getErc20Transfers(
        chainId: ChainId,
        baseUrl: String,
        contractAddress: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanAccountTransfer>>

    suspend fun getNormalTxsHistory(
        chainId: ChainId,
        baseUrl: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanNormalTxResponse>>
}

class RealEtherscanTransactionsApi(
    private val retrofitApi: RetrofitEtherscanTransactionsApi,
    private val apiKeys: EtherscanApiKeys
) : EtherscanTransactionsApi {

    override suspend fun getErc20Transfers(
        chainId: ChainId,
        baseUrl: String,
        contractAddress: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanAccountTransfer>> {
        val apiKey = apiKeys.keyFor(chainId)

        return retrofitApi.getErc20Transfers(
            baseUrl = baseUrl,
            contractAddress = contractAddress,
            accountAddress = accountAddress,
            pageNumber = pageNumber,
            pageSize = pageSize,
            apiKey = apiKey
        )
    }

    override suspend fun getNormalTxsHistory(
        chainId: ChainId,
        baseUrl: String,
        accountAddress: String,
        pageNumber: Int,
        pageSize: Int
    ): EtherscanResponse<List<EtherscanNormalTxResponse>> {
        val apiKey = apiKeys.keyFor(chainId)

        return retrofitApi.getNormalTxsHistory(
            baseUrl = baseUrl,
            accountAddress = accountAddress,
            pageNumber = pageNumber,
            pageSize = pageSize,
            apiKey = apiKey
        )
    }
}
