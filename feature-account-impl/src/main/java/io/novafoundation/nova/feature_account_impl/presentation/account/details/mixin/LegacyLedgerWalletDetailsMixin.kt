package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.mapToSet
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
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LegacyLedgerWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val host: WalletDetailsMixinHost,
    private val appLinksProvider: AppLinksProvider,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {

    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { setOf(AccountAction.CHANGE) }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        if (ledgerMigrationTracker.anyChainSupportsMigrationApp()) {
            AlertModel(
                style = AlertView.Style.fromPreset(AlertView.StylePreset.WARNING),
                message = resourceManager.getString(R.string.account_ledger_legacy_warning_title),
                subMessage = resourceManager.getString(R.string.account_ledger_legacy_warning_message),
                action = AlertModel.ActionModel(
                    text = resourceManager.getString(R.string.common_find_out_more),
                    listener = { host.browserableDelegate.showBrowser(appLinksProvider.ledgerMigrationArticle) }
                )
            )
        } else {
            AlertModel(
                style = AlertView.Style(
                    backgroundColorRes = R.color.block_background,
                    iconRes = R.drawable.ic_ledger
                ),
                message = resourceManager.getString(R.string.ledger_wallet_details_description)
            )
        }
    }

    override fun accountProjectionsFlow(): Flow<GroupedList<AccountInChain.From, AccountInChain>> = flowOfAll {
        val ledgerSupportedChainIds = SubstrateApplicationConfig.all().mapToSet { it.chainId }
        val chains = interactor.getAllChains()
            .filter { it.id in ledgerSupportedChainIds }
        interactor.chainProjectionsFlow(
            metaAccount.id,
            chains,
            hasAccountComparator().withChainComparator()
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
