package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.mapToAccountHeader
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SecretsWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {
    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { setOf(AccountAction.EXPORT, AccountAction.CHANGE) }

    override val typeAlert: Flow<AlertModel?> = flowOf { null }

    override fun accountProjectionsFlow(): Flow<GroupedList<AccountInChain.From, AccountInChain>> = flowOfAll {
        interactor.chainProjectionsFlow(metaAccount.id, interactor.getAllChains(), hasAccountComparator().withChainComparator())
    }

    override suspend fun mapAccountHeader(from: AccountInChain.From): TextHeader? {
        return from.mapToAccountHeader(resourceManager)
    }

    override suspend fun mapAccount(accountInChain: AccountInChain): AccountInChainUi {
        return accountFormatter.formatChainAccountProjection(
            accountInChain,
            availableAccountActions.first()
        )
    }
}
