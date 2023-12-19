package io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation.ProxiedExtrinsicValidationFailure.ProxyMetaAccountNotFound
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation.ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class ProxyHaveEnoughFeeValidationFactory(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val extrinsicService: ExtrinsicService
) {
    fun create(): ProxyHaveEnoughFeeValidation {
        return ProxyHaveEnoughFeeValidation(
            assetSourceRegistry,
            accountRepository,
            walletRepository,
            extrinsicService
        )
    }
}

class ProxyHaveEnoughFeeValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val extrinsicService: ExtrinsicService,
) : Validation<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure> {

    override suspend fun validate(value: ProxiedExtrinsicValidationPayload): ValidationStatus<ProxiedExtrinsicValidationFailure> {
        val lastProxyMetaAccount = accountRepository.getLastProxyAccountFor(value.rootProxiedMetaAccount.id)
        if (lastProxyMetaAccount == null) {
            return validationError(ProxyMetaAccountNotFound)
        }
        val commissionAsset = walletRepository.getAsset(lastProxyMetaAccount.id, value.chain.commissionAsset)!!
        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(value.chain, commissionAsset.token.configuration)
        val fee = getFee(value)
        val availableBalanceToPayFee = commissionAsset.balanceCountedTowardsEDInPlanks - existentialDeposit

        return validOrError(availableBalanceToPayFee >= fee.amount) {
            ProxyNotEnoughFee(lastProxyMetaAccount, availableBalanceToPayFee.atLeastZero(), fee)
        }
    }

    private suspend fun getFee(value: ProxiedExtrinsicValidationPayload): Fee {
        return extrinsicService.estimateFeeV2(value.chain) {
            call(value.call)
        }
    }
}

fun ProxiedExtrinsicValidationSystemBuilder.proxyHaveEnoughFeeValidation(
    proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory
) = validate(proxyHaveEnoughFeeValidationFactory.create())
