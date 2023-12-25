package io.novafoundation.nova.feature_account_api.data.proxy.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

typealias ProxiedExtrinsicValidationSystem = ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure>
typealias ProxiedExtrinsicValidationSystemBuilder = ValidationSystemBuilder<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure>

class ProxiedExtrinsicValidationPayload(
    val proxyMetaAccount: MetaAccount,
    val proxyAccountId: AccountId,
    val chainWithAsset: ChainWithAsset,
    val call: GenericCall.Instance
)

sealed interface ProxiedExtrinsicValidationFailure {

    class ProxyNotEnoughFee(
        val metaAccount: MetaAccount,
        val asset: Chain.Asset,
        val availableBalance: BigInteger,
        val fee: Fee
    ) : ProxiedExtrinsicValidationFailure
}
