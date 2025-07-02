package io.novafoundation.nova.feature_multisig_operations.presentation.details.general

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_BIG
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter.SignatoryRvItem
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SignatoryListFormatter(
    private val addressIconGenerator: AddressIconGenerator,
    private val walletUiUseCase: WalletUiUseCase,
    private val accountInteractor: AccountInteractor,
    private val proxyFormatter: ProxyFormatter,
    private val multisigFormatter: MultisigFormatter
) {

    private class FormattingContext(val metaAccounts: Map<Long, MetaAccount>, val approvals: Set<AccountIdKey>) {

        fun account(id: Long): MetaAccount = metaAccounts.getValue(id)

        fun isApprovedBy(accountId: AccountIdKey): Boolean = accountId in approvals
    }

    suspend fun formatSignatories(
        chain: Chain,
        signatories: Set<AccountIdKey>,
        approvals: Set<AccountIdKey>
    ): List<SignatoryRvItem> {
        val metaAccounts = accountInteractor.getActiveMetaAccounts()
        val metaAccountsByAccountId = metaAccounts.filter { it.hasAccountIn(chain) }
            .associateBy { it.requireAccountIdKeyIn(chain) }

        val formattingContext = getFormattingContext(metaAccounts, approvals)

        return signatories.map {
            val metaAccount = metaAccountsByAccountId[it]
            when {
                metaAccount != null -> formatMetaAccount(metaAccount, chain, it, formattingContext)
                else -> formatUnknownAccount(it, chain, formattingContext)
            }
        }
    }

    private fun getFormattingContext(metaAccounts: List<MetaAccount>, approvals: Set<AccountIdKey>): FormattingContext {
        val metaAccountsById = metaAccounts.associateBy { it.id }
        return FormattingContext(metaAccountsById, approvals)
    }

    private suspend fun formatUnknownAccount(accountId: AccountIdKey, chain: Chain, context: FormattingContext): SignatoryRvItem {
        val address = chain.addressOf(accountId)
        return SignatoryRvItem(
            address = addressIconGenerator.createAddressModel(
                accountAddress = address,
                accountName = address.ellipsizeAddress(),
                sizeInDp = SIZE_BIG,
                background = BACKGROUND_TRANSPARENT
            ),
            subtitle = null,
            isApproved = context.isApprovedBy(accountId)
        )
    }

    private suspend fun formatMetaAccount(
        metaAccount: MetaAccount,
        chain: Chain,
        accountId: AccountIdKey,
        context: FormattingContext
    ): SignatoryRvItem {
        return SignatoryRvItem(
            address = addressIconGenerator.createAddressModel(
                accountAddress = metaAccount.requireAddressIn(chain),
                accountName = metaAccount.name,
                sizeInDp = SIZE_BIG,
                background = BACKGROUND_TRANSPARENT
            ),
            subtitle = metaAccount.formatSubtitle(context),
            isApproved = context.isApprovedBy(accountId)
        )
    }

    private suspend fun MetaAccount.formatSubtitle(context: FormattingContext): CharSequence? = when (this) {
        is ProxiedMetaAccount -> formatSubtitle(context)
        is MultisigMetaAccount -> formatSubtitle(context)
        else -> null
    }

    private suspend fun ProxiedMetaAccount.formatSubtitle(context: FormattingContext): CharSequence {
        val proxyMetaAccount = context.account(this.proxy.proxyMetaId)

        return proxyFormatter.mapProxyMetaAccountSubtitle(
            proxyMetaAccount.name,
            proxyFormatter.makeAccountDrawable(proxyMetaAccount),
            proxy
        )
    }

    private suspend fun MultisigMetaAccount.formatSubtitle(context: FormattingContext): CharSequence {
        val signatory = context.account(this.signatoryMetaId)

        return multisigFormatter.formatSignatorySubtitle(signatory)
    }
}
