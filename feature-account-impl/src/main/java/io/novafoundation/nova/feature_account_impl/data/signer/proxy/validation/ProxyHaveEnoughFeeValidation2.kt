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
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class ProxyHaveEnoughFeeValidation2<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val extrinsicService: ExtrinsicService,
    private val proxiedMetaAccount: (P) -> MetaAccount,
    private val chain: (P) -> Chain,
    private val callInstance: (P) -> GenericCall.Instance,
    private val proxyMetaAccountNotFound: () -> E,
    private val proxyNotEnoughFee: (proxy: MetaAccount, availableBalanceToPayFee: Balance, fee: Fee) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val lastProxyMetaAccount = accountRepository.getLastProxyAccountFor(proxiedMetaAccount(value).id)
        if (lastProxyMetaAccount == null) {
            return validationError(proxyMetaAccountNotFound())
        }
        val commissionAsset = walletRepository.getAsset(lastProxyMetaAccount.id, chain(value).commissionAsset)!!
        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain(value), commissionAsset.token.configuration)
        val fee = getFee(value)
        val availableBalanceToPayFee = commissionAsset.balanceCountedTowardsEDInPlanks - existentialDeposit

        return validOrError(availableBalanceToPayFee >= fee.amount) {
            proxyNotEnoughFee(lastProxyMetaAccount, availableBalanceToPayFee.atLeastZero(), fee)
        }
    }

    private suspend fun getFee(value: P): Fee {
        return extrinsicService.estimateFeeV2(chain(value)) {
            call(callInstance(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.proxyHaveEnoughFeeValidation2(
    assetSourceRegistry: AssetSourceRegistry,
    accountRepository: AccountRepository,
    walletRepository: WalletRepository,
    extrinsicService: ExtrinsicService,
    proxiedMetaAccount: (P) -> MetaAccount,
    chain: (P) -> Chain,
    callInstance: (P) -> GenericCall.Instance,
    proxyMetaAccountNotFound: () -> E,
    proxyNotEnoughFee: (proxy: MetaAccount, availableBalanceToPayFee: Balance, fee: Fee) -> E,
) = validate(
    ProxyHaveEnoughFeeValidation2(
        assetSourceRegistry,
        accountRepository,
        walletRepository,
        extrinsicService,
        proxiedMetaAccount,
        chain,
        callInstance,
        proxyMetaAccountNotFound,
        proxyNotEnoughFee
    )
)
