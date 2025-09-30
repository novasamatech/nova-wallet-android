package io.novafoundation.nova.feature_account_api.data.proxy.validation

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import java.math.BigInteger

class ProxiedExtrinsicValidationPayload(
    val proxiedMetaAccount: ProxiedMetaAccount,
    val proxyMetaAccount: MetaAccount,
    val chainWithAsset: ChainWithAsset,
    val proxiedCall: GenericCall.Instance
)

val ProxiedExtrinsicValidationPayload.proxyAccountId: AccountId
    get() = proxyMetaAccount.requireAccountIdIn(chainWithAsset.chain)

sealed interface ProxiedExtrinsicValidationFailure {

    class ProxyNotEnoughFee(
        val proxy: MetaAccount,
        val asset: Chain.Asset,
        val fee: BigInteger,
        val availableBalance: BigInteger
    ) : ProxiedExtrinsicValidationFailure
}
