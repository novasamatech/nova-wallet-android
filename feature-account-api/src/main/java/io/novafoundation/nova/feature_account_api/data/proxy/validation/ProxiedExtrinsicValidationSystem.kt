package io.novafoundation.nova.feature_account_api.data.proxy.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

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
        val fee: String,
        val availableBalance: String
    ) : ProxiedExtrinsicValidationFailure
}
