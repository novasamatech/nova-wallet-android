package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

@FeatureScope
class ProxyHaveEnoughFeeValidationFactory @Inject constructor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService
) {
    fun <P, E> create(
        proxyAccountId: (P) -> AccountId,
        proxiedMetaAccount: (P) -> ProxiedMetaAccount,
        proxiedCall: (P) -> GenericCall.Instance,
        chainWithAsset: (P) -> ChainWithAsset,
        proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
    ): ProxyHaveEnoughFeeValidation<P, E> {
        return ProxyHaveEnoughFeeValidation(
            assetSourceRegistry,
            extrinsicService,
            proxiedMetaAccount,
            proxyAccountId,
            proxiedCall,
            chainWithAsset,
            proxyNotEnoughFee
        )
    }
}

class ProxyHaveEnoughFeeValidation<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val proxiedMetaAccount: (P) -> ProxiedMetaAccount,
    private val proxyAccountId: (P) -> AccountId,
    private val proxiedCall: (P) -> GenericCall.Instance,
    private val chainWithAsset: (P) -> ChainWithAsset,
    private val proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainWithAsset(value).chain
        val chainAsset = chainWithAsset(value).asset
        val fee = calculateFee(proxiedMetaAccount(value), chain, proxiedCall(value))

        val assetSource = assetSourceRegistry.sourceFor(chainAsset)
        val assetBalanceSource = assetSource.balance

        val balance = assetBalanceSource.queryAccountBalance(chain, chainAsset, proxyAccountId(value))

        val existentialDeposit = assetBalanceSource.existentialDeposit(chainAsset)
        val balanceWithoutEd = (balance.countedTowardsEd - existentialDeposit).atLeastZero()

        return validOrError(balance.transferable >= fee.amount && balanceWithoutEd >= fee.amount) {
            proxyNotEnoughFee(value, balance.transferable, fee)
        }
    }

    private suspend fun calculateFee(
        proxiedMetaAccount: ProxiedMetaAccount,
        chain: Chain,
        proxiedCall: GenericCall.Instance
    ): Fee {
        return extrinsicService.estimateFee(chain, proxiedMetaAccount.intoOrigin()) {
            call(proxiedCall)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.proxyHasEnoughFeeValidation(
    factory: ProxyHaveEnoughFeeValidationFactory,
    proxiedMetaAccount: (P) -> ProxiedMetaAccount,
    proxyAccountId: (P) -> AccountId,
    proxiedCall: (P) -> GenericCall.Instance,
    chainWithAsset: (P) -> ChainWithAsset,
    proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
) = validate(
    factory.create(
        proxyAccountId = proxyAccountId,
        proxiedMetaAccount = proxiedMetaAccount,
        proxiedCall = proxiedCall,
        chainWithAsset = chainWithAsset,
        proxyNotEnoughFee = proxyNotEnoughFee
    )
)
