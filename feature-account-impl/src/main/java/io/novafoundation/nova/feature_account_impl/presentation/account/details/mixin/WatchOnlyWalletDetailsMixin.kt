package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.mapToAccountGroupUi
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WatchOnlyWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {
    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { setOf(AccountAction.CHANGE) }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        AlertModel(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_watch_only_filled
            ),
            message = resourceManager.getString(R.string.account_details_watch_only_alert)
        )
    }

    override fun accountProjectionsFlow(): Flow<List<Any>> = flowOfAll {
        interactor.chainProjectionsBySourceFlow(metaAccount.id, interactor.getAllChains(), hasAccountComparator().withChainComparator())
            .map { accounts ->
                val availableActions = availableAccountActions.first()

                accounts.toListWithHeaders(
                    keyMapper = { from, _ -> from.mapToAccountGroupUi(resourceManager) },
                    valueMapper = { chainAccount -> accountFormatter.formatChainAccountProjection(chainAccount, availableActions) }
                )
            }
    }
}
