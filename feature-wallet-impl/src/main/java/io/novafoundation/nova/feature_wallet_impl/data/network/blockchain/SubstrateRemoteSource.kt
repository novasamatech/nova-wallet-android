package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.ExtrinsicStatusEvent
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.TransferExtrinsic
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class TransferExtrinsicWithStatus(
    val extrinsic: TransferExtrinsic,
    val statusEvent: ExtrinsicStatusEvent?,
)

interface SubstrateRemoteSource {

    suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId
    ): AccountInfo

    suspend fun getTransferFee(
        chain: Chain,
        transfer: Transfer
    ): BigInteger

    suspend fun performTransfer(
        accountId: ByteArray,
        chain: Chain,
        transfer: Transfer,
    ): String

    suspend fun fetchAccountTransfersInBlock(
        chainId: ChainId,
        blockHash: String,
        accountId: ByteArray
    ): Result<List<TransferExtrinsicWithStatus>>
}
