package io.novafoundation.nova.feature_multisig_operations.presentation.details.general

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountModel
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.DerivativeFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter.SignatoryRvItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class SignatoryListFormatter @Inject constructor(
    private val accountInteractor: AccountInteractor,
    private val proxyFormatter: ProxyFormatter,
    private val multisigFormatter: MultisigFormatter,
    private val derivativeFormatter: DerivativeFormatter,
) {

    private class FormattingContext(
        val currentSignatoryAccountId: AccountIdKey,
        val currentSignatory: MetaAccount,
        val metaAccountByAccountIds: GroupedList<AccountIdKey, MetaAccount>,
        val metaAccountsByMetaId: Map<Long, MetaAccount>,
        val approvals: Set<AccountIdKey>
    ) {

        /**
         * We try to show the most relevant account to user.
         * For signatory account that associated with current multisig account we like to show the same account
         */
        fun account(accountId: AccountIdKey): MetaAccount? {
            if (currentSignatoryAccountId == accountId) return currentSignatory

            return metaAccountByAccountIds[accountId]
                ?.findRelevantAccountToShow()
        }

        fun account(metaId: Long) = metaAccountsByMetaId[metaId]

        fun isApprovedBy(accountId: AccountIdKey): Boolean = accountId in approvals
    }

    suspend fun formatSignatories(
        chain: Chain,
        currentSignatory: MetaAccount,
        signatories: Map<AccountIdKey, AccountModel>,
        approvals: Set<AccountIdKey>
    ): List<SignatoryRvItem> {
        val metaAccounts = accountInteractor.getActiveMetaAccounts()
        val formattingContext = formattingContext(chain, currentSignatory, metaAccounts, approvals)

        return signatories.map { (signatoryAccountId, signatoryAccountModel) ->
            val maybeMetaAccount = formattingContext.account(signatoryAccountId)
            SignatoryRvItem(
                accountModel = signatoryAccountModel,
                subtitle = maybeMetaAccount?.formatSubtitle(formattingContext),
                isApproved = formattingContext.isApprovedBy(signatoryAccountId)
            )
        }
    }

    private fun formattingContext(
        chain: Chain,
        currentSignatory: MetaAccount,
        metaAccounts: List<MetaAccount>,
        approvals: Set<AccountIdKey>
    ): FormattingContext {
        val metaAccountsByAccountIds = metaAccounts
            .filter { it.hasAccountIn(chain) }
            .groupBy { it.requireAccountIdKeyIn(chain) }

        val metaAccountsByMetaIds = metaAccounts.associateBy { it.id }

        return FormattingContext(
            currentSignatory.requireAccountIdKeyIn(chain),
            currentSignatory,
            metaAccountsByAccountIds,
            metaAccountsByMetaIds,
            approvals
        )
    }

    private suspend fun MetaAccount.formatSubtitle(context: FormattingContext): CharSequence? = when (this) {
        is ProxiedMetaAccount -> formatSubtitle(context)
        is MultisigMetaAccount -> formatSubtitle(context)
        is DerivativeMetaAccount -> formatSubtitle(context)
        else -> null
    }

    private suspend fun ProxiedMetaAccount.formatSubtitle(context: FormattingContext): CharSequence? {
        val proxyMetaAccount = context.account(this.proxy.proxyMetaId) ?: return null

        return proxyFormatter.formatProxiedMetaAccountSubtitle(proxyMetaAccount,  proxy)
    }

    private suspend fun MultisigMetaAccount.formatSubtitle(context: FormattingContext): CharSequence? {
        val signatory = context.account(this.signatoryMetaId) ?: return null

        return multisigFormatter.formatSignatorySubtitle(signatory)
    }

    private suspend fun DerivativeMetaAccount.formatSubtitle(context: FormattingContext): CharSequence? {
        val parent = context.account(this.parentMetaId) ?: return null

        return derivativeFormatter.formatDeriveAccountSubtitle(this, parent)
    }
}

/**
 * Since we may have multiple accounts by one account id it would be better to show the most relevant to user.
 * For example we show secrets-account instead of the proxied-account
 */
private fun Collection<MetaAccount>.findRelevantAccountToShow(): MetaAccount? {
    return minByOrNull { it.priorityToShowAsSignatory() }
}

private fun MetaAccount.priorityToShowAsSignatory() = when (type) {
    LightMetaAccount.Type.SECRETS -> 0

    LightMetaAccount.Type.PARITY_SIGNER,
    LightMetaAccount.Type.LEDGER_LEGACY,
    LightMetaAccount.Type.LEDGER,
    LightMetaAccount.Type.POLKADOT_VAULT,
    LightMetaAccount.Type.WATCH_ONLY -> 1

    LightMetaAccount.Type.PROXIED,
    LightMetaAccount.Type.MULTISIG,
    LightMetaAccount.Type.DERIVATIVE -> 2
}
