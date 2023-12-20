package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.model.AccountTypeAlert
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProxiedWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val proxyFormatter: ProxyFormatter,
    metaAccount: MetaAccount
) : WalletDetailsMixin(
    interactor,
    metaAccount
) {
    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AccountTypeAlert?> = flowOf {
        val proxyAccount = metaAccount.proxy ?: return@flowOf null
        val proxyMetaAccount = interactor.getMetaAccount(proxyAccount.metaId)

        val proxyAccountWithIcon = proxyFormatter.mapProxyMetaAccount(proxyMetaAccount.name, proxyFormatter.makeAccountDrawable(proxyMetaAccount))
        AccountTypeAlert(
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

    override suspend fun getChainProjections(): GroupedList<AccountInChain.From, AccountInChain> {
        val proxiedChainIds = metaAccount.chainAccounts.keys
        val chains = interactor.getAllChains()
            .filter { it.id in proxiedChainIds }
        return interactor.getChainProjections(metaAccount, chains, hasAccountComparator().withChainComparator())
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
