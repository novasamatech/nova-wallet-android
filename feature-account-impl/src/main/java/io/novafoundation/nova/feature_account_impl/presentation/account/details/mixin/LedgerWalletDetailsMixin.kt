package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.notHasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.model.AccountTypeAlert
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LedgerWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    metaAccount: MetaAccount
) : WalletDetailsMixin(
    interactor,
    metaAccount
) {

    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { setOf(AccountAction.CHANGE) }

    override val typeAlert: Flow<AccountTypeAlert?> = flowOf {
        AccountTypeAlert(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_ledger
            ),
            message = resourceManager.getString(R.string.ledger_wallet_details_description)
        )
    }

    override suspend fun getChainProjections(): GroupedList<AccountInChain.From, AccountInChain> {
        val ledgerSupportedChainIds = SubstrateApplicationConfig.all().mapToSet { it.chainId }
        val chains = interactor.getAllChains()
            .filter { it.id in ledgerSupportedChainIds }
        return interactor.getChainProjections(
            metaAccount,
            chains,
            notHasAccountComparator().withChainComparator()
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
