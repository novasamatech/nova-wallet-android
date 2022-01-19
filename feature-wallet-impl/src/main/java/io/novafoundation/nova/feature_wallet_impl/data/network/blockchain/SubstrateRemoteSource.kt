package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.ExtrinsicStatusEvent
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.TransferExtrinsic
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class TransferExtrinsicWithStatus(
    val extrinsic: TransferExtrinsic,
    val statusEvent: ExtrinsicStatusEvent?,
)

interface SubstrateRemoteSource {

    suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId
    ): AccountInfo

    suspend fun fetchAccountTransfersInBlock(
        chainId: ChainId,
        blockHash: String,
        accountId: ByteArray
    ): Result<List<TransferExtrinsicWithStatus>>
}
