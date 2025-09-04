package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.domain.balance.calculateBalanceCountedTowardsEd
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class ProxyHaveEnoughFeeValidationFactory(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val walletRepository: WalletRepository,
    private val extrinsicService: ExtrinsicService
) {
    fun <P, E> create(
        proxyAccountId: (P) -> AccountId,
        metaAccount: (P) -> MetaAccount,
        call: (P) -> GenericCall.Instance,
        chainWithAsset: (P) -> ChainWithAsset,
        proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
    ): ProxyHaveEnoughFeeValidation<P, E> {
        return ProxyHaveEnoughFeeValidation(
            assetSourceRegistry,
            walletRepository,
            extrinsicService,
            metaAccount,
            proxyAccountId,
            call,
            chainWithAsset,
            proxyNotEnoughFee
        )
    }
}

class ProxyHaveEnoughFeeValidation<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val walletRepository: WalletRepository,
    private val extrinsicService: ExtrinsicService,
    private val metaAccount: (P) -> MetaAccount,
    private val proxyAccountId: (P) -> AccountId,
    private val call: (P) -> GenericCall.Instance,
    private val chainWithAsset: (P) -> ChainWithAsset,
    private val proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainWithAsset(value).chain
        val chainAsset = chainWithAsset(value).asset
        val fee = calculateFee(metaAccount(value), chain, call(value))
        val asset = walletRepository.getAsset(proxyAccountId(value), chainAsset)!!

        val assetSource = assetSourceRegistry.sourceFor(chainAsset)
        val assetBalanceSource = assetSource.balance

        val accountData = assetBalanceSource.queryAccountBalance(chain, chainAsset, proxyAccountId(value))

        val existentialDeposit = assetBalanceSource.existentialDeposit(chainAsset)
        val transferable = asset.transferableMode.calculateTransferable(accountData.free, accountData.frozen, accountData.reserved)
        val balanceCountedTowardsEd = asset.edCountingMode.calculateBalanceCountedTowardsEd(accountData.free, accountData.reserved)
        val balanceWithoutEd = (balanceCountedTowardsEd - existentialDeposit).atLeastZero()

        return validOrError(transferable >= fee.amount && balanceWithoutEd >= fee.amount) {
            proxyNotEnoughFee(value, transferable, fee)
        }
    }

    private suspend fun calculateFee(metaAccount: MetaAccount, chain: Chain, callInstance: GenericCall.Instance): Fee {
        return extrinsicService.estimateFee(chain, metaAccount.intoOrigin()) {
            call(callInstance)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.proxyHasEnoughFeeValidation(
    factory: ProxyHaveEnoughFeeValidationFactory,
    metaAccount: (P) -> MetaAccount,
    proxyAccountId: (P) -> AccountId,
    call: (P) -> GenericCall.Instance,
    chainWithAsset: (P) -> ChainWithAsset,
    proxyNotEnoughFee: (payload: P, availableBalance: Balance, fee: Fee) -> E,
) = validate(
    factory.create(
        proxyAccountId,
        metaAccount,
        call,
        chainWithAsset,
        proxyNotEnoughFee
    )
)
