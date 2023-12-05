package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
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
        fromChainId: ChainId,
        destinationChainId: ChainId,
        selectedAddress: String?
    ): MetaAccountListingMixin {
        return MetaAccountValidForTransactionListingMixin(
            walletUiUseCase = walletUiUseCase,
            resourceManager = resourceManager,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            chainRegistry = chainRegistry,
            fromChainId = fromChainId,
            destinationChainId = destinationChainId,
            selectedAddress = selectedAddress,
            accountTypePresentationMapper = accountTypePresentationMapper,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountValidForTransactionListingMixin(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val chainRegistry: ChainRegistry,
    private val fromChainId: ChainId,
    private val destinationChainId: ChainId,
    private val selectedAddress: String?,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val destinationChainFlow by coroutineScope.lazyAsync { chainRegistry.getChain(destinationChainId) }

    override val metaAccountsFlow = metaAccountGroupingInteractor.getMetaAccountsForTransaction(fromChainId, destinationChainId)
        .map { list ->
            list.toListWithHeaders(
                keyMapper = { type, _ -> accountTypePresentationMapper.mapMetaAccountTypeToUi(type) },
                valueMapper = { mapMetaAccountToUi(it) }
            )
        }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccount: MetaAccount): AccountUi {
        val icon = walletUiUseCase.walletIcon(metaAccount)

        val chainAddress = metaAccount.addressIn(destinationChainFlow.await())
        val isSelected = chainAddress != null && chainAddress == selectedAddress

        return AccountUi(
            id = metaAccount.id,
            title = metaAccount.name,
            subtitle = mapSubtitle(chainAddress),
            isSelected = isSelected,
            isClickable = chainAddress != null,
            picture = icon,
            chainIconUrl = null,
            subtitleIconRes = if (chainAddress == null) R.drawable.ic_warning_filled else null
        )
    }

    private fun mapSubtitle(address: String?): String {
        return address ?: resourceManager.getString(R.string.account_no_chain_projection)
    }
}
