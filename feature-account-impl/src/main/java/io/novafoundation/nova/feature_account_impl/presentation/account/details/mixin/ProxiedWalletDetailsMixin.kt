package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProxiedWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val proxyFormatter: ProxyFormatter,
    metaAccount: MetaAccount
) : WalletDetailsMixin(metaAccount) {
    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        val proxyAccount = metaAccount.proxy ?: return@flowOf null
        val proxyMetaAccount = interactor.getMetaAccount(proxyAccount.metaId)

        val proxyAccountWithIcon = proxyFormatter.mapProxyMetaAccount(proxyMetaAccount.name, proxyFormatter.makeAccountDrawable(proxyMetaAccount))
        AlertModel(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_proxy
            ),
            message = resourceManager.getString(R.string.proxied_wallet_details_info_warning),
            subMessage = SpannableStringBuilder(proxyAccountWithIcon)
                .appendSpace()
                .append(proxyFormatter.mapProxyTypeToString(proxyAccount.proxyType))
        )
    }

    override fun accountProjectionsFlow(): Flow<List<Any>> = flowOfAll {
        val proxiedChainIds = metaAccount.chainAccounts.keys
        val chains = interactor.getAllChains()
            .filter { it.id in proxiedChainIds }

        interactor.chainProjectionsFlow(metaAccount.id, chains, hasAccountComparator().withChainComparator())
            .map { accounts ->
                val availableActions = availableAccountActions.first()

                accounts.toListWithHeaders(
                    keyMapper = { _, _ -> null },
                    valueMapper = { chainAccount -> accountFormatter.formatChainAccountProjection(chainAccount, availableActions) }
                )
            }
    }
}
