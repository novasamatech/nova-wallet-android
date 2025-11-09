package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.SelectedAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountValidForTransactionListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val chainRegistry: ChainRegistry,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor
) {

    fun create(
        coroutineScope: CoroutineScope,
        chainId: ChainId,
        selectedAccount: SelectedAccountPayload?,
        metaAccountFilter: Filter<MetaAccount>
    ): MetaAccountListingMixin {
        return MetaAccountValidForTransactionListingMixin(
            walletUiUseCase = walletUiUseCase,
            resourceManager = resourceManager,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            chainRegistry = chainRegistry,
            chainId = chainId,
            selectedAccount = selectedAccount,
            accountTypePresentationMapper = accountTypePresentationMapper,
            metaAccountFilter = metaAccountFilter,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountValidForTransactionListingMixin(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId,
    private val selectedAccount: SelectedAccountPayload?,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val metaAccountFilter: Filter<MetaAccount>,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val chainFlow by coroutineScope.lazyAsync { chainRegistry.getChain(chainId) }

    override val metaAccountsFlow = metaAccountGroupingInteractor.getMetaAccountsWithFilter(metaAccountFilter)
        .map { list ->
            list.toListWithHeaders(
                keyMapper = { type, _ -> accountTypePresentationMapper.mapMetaAccountTypeToUi(type) },
                valueMapper = { mapMetaAccountToUi(it) }
            )
        }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccount: MetaAccount): AccountUi {
        val icon = walletUiUseCase.walletIcon(metaAccount)

        val chain = chainFlow.await()
        val chainAddress = metaAccount.addressIn(chain)

        return AccountUi(
            id = metaAccount.id,
            title = metaAccount.name,
            subtitle = mapSubtitle(chainAddress, chain),
            isSelected = isSelected(metaAccount, chainAddress),
            isClickable = chainAddress != null,
            picture = icon,
            chainIcon = null,
            updateIndicator = false,
            subtitleIconRes = if (chainAddress == null) R.drawable.ic_warning_filled else null,
            isEditable = false
        )
    }

    private fun isSelected(metaAccount: MetaAccount, chainAddress: String?): Boolean {
        return when (selectedAccount) {
            is SelectedAccountPayload.MetaAccount -> selectedAccount.metaId == metaAccount.id
            is SelectedAccountPayload.Address -> selectedAccount.address == chainAddress
            null -> false
        }
    }

    private fun mapSubtitle(address: String?, chain: Chain): String {
        return address ?: resourceManager.getString(R.string.account_chain_not_found, chain.name)
    }
}
