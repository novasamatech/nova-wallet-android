package io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall


typealias ProxiedExtrinsicValidationSystem = ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure>
typealias ProxiedExtrinsicValidationSystemBuilder = ValidationSystemBuilder<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure>

class ProxiedExtrinsicValidationPayload(val rootProxiedMetaAccount: MetaAccount, val chain: Chain, val call: GenericCall.Instance)

sealed interface ProxiedExtrinsicValidationFailure {
    class ProxyNotEnoughFee(val proxyMetaAccount: MetaAccount, val availableBalanceToPayFee: BigInteger, val fee: Fee) : ProxiedExtrinsicValidationFailure

    object ProxyMetaAccountNotFound : ProxiedExtrinsicValidationFailure
}

fun proxiedExtrinsicValidationSystem(
    proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory
) = ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure> {
    proxyHaveEnoughFeeValidation(proxyHaveEnoughFeeValidationFactory)
}
