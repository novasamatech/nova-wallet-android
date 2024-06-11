package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GenericLedgerWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
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

    override suspend fun getChainProjections(): GroupedList<AccountInChain.From, AccountInChain> {
        val chains = ledgerMigrationTracker.supportedChainsByGenericApp()

        return interactor.getChainProjections(
            metaAccount = metaAccount,
            chains = chains,
            sorting = Chain.defaultComparatorFrom(AccountInChain::chain)
        )
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
