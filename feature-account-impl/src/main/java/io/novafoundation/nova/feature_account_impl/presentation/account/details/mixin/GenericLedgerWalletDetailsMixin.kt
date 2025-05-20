package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.ChainAccountGroupUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasChainAccount
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GenericLedgerWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val router: AccountRouter,
    private val addressSchemeFormatter: AddressSchemeFormatter,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {

    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        AlertModel(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_ledger
            ),
            message = resourceManager.getString(R.string.ledger_wallet_details_description)
        )
    }

    override fun accountProjectionsFlow(): Flow<List<Any>> {
        return flowOfAll {
            val chains = ledgerMigrationTracker.supportedChainsByGenericApp()

            interactor.chainProjectionsByAddressSchemeFlow(
                metaId = metaAccount.id,
                chains = chains,
                sorting = Chain.defaultComparatorFrom(AccountInChain::chain)
            ).map { accounts ->
                val availableActions = availableAccountActions.first()

                accounts.toListWithHeaders(
                    keyMapper = ::createGroupUi,
                    valueMapper = { chainAccount -> accountFormatter.formatChainAccountProjection(chainAccount, availableActions) }
                )
            }
        }
    }

    override suspend fun groupActionClicked(groupId: String) {
        router.openAddGenericEvmAddressSelectLedger(metaAccount.id)
    }

    private fun createGroupUi(addressScheme: AddressScheme, accounts: List<AccountInChain>): ChainAccountGroupUi {
        val canAdd = accounts.none { it.hasChainAccount }
        val action = if (canAdd) {
            ChainAccountGroupUi.Action(
                name = resourceManager.getString(R.string.account_add_address),
                icon = R.drawable.ic_add_circle
            )
        } else {
            null
        }

        return ChainAccountGroupUi(
            id = addressScheme.name,
            title = addressSchemeFormatter.accountsLabel(addressScheme),
            action = action
        )
    }
}
