package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal
import java.math.BigInteger

data class AssetTransfer(
    val sender: MetaAccount,
    val recipient: AccountId,
    val chain: Chain,
    val chainAsset: Chain.Asset,
    val amount: BigDecimal,
)

interface AssetTransfers {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun calculateFee(transfer: AssetTransfer): BigInteger

    suspend fun performTransfer(transfer: AssetTransfer): Result<String>
}
