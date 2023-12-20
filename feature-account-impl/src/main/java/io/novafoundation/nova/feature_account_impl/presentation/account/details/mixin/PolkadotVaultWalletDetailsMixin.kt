package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.polkadotVaultAccountTypeAlert
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.polkadotVaultTitle
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.model.AccountTypeAlert
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PolkadotVaultWalletDetailsMixin(
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    metaAccount: MetaAccount
) : WalletDetailsMixin(
    interactor,
    metaAccount
) {
    private val accountFormatter = accountFormatterFactory.create(
        accountTitleFormatter = { it.polkadotVaultTitle(resourceManager, metaAccount) }
    )

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AccountTypeAlert?> = flowOf {
        val vaultVariant = metaAccount.type.asPolkadotVaultVariantOrThrow()
        val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(vaultVariant)
        polkadotVaultAccountTypeAlert(vaultVariant, variantConfig, resourceManager)
    }

    override suspend fun getChainProjections(): GroupedList<AccountInChain.From, AccountInChain> {
        return interactor.getChainProjections(metaAccount, interactor.getAllChains(), hasAccountComparator().withChainComparator())
    }

    override suspend fun mapAccountHeader(from: AccountInChain.From): TextHeader? {
        return null
    }

    override suspend fun mapAccount(accountInChain: AccountInChain): AccountInChainUi {
        return accountFormatter.formatChainAccountProjection(
            accountInChain,
            availableAccountActions.first()
        )
    }
}
