package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.polkadotVaultAccountTypeAlert
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.polkadotVaultTitle
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PolkadotVaultWalletDetailsMixin(
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {
    private val accountFormatter = accountFormatterFactory.create(
        accountTitleFormatter = { it.polkadotVaultTitle(resourceManager, metaAccount) }
    )

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        val vaultVariant = metaAccount.type.asPolkadotVaultVariantOrThrow()
        val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(vaultVariant)
        polkadotVaultAccountTypeAlert(vaultVariant, variantConfig, resourceManager)
    }

    override fun accountProjectionsFlow(): Flow<List<Any>> = flowOfAll {
        interactor.chainProjectionsBySourceFlow(metaAccount.id, interactor.getAllChains(), hasAccountComparator().withChainComparator())
    }.map { accounts ->
        val availableActions = availableAccountActions.first()

        accounts.toListWithHeaders(
            keyMapper = { _, _ -> null },
            valueMapper = { chainAccount -> accountFormatter.formatChainAccountProjection(chainAccount, availableActions) }
        )
    }
}
