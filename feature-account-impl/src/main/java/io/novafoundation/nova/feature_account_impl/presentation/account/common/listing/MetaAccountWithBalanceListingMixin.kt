package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountWithBalanceListingMixinFactory(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor
) {

    fun create(
        coroutineScope: CoroutineScope
    ): MetaAccountListingMixin {
        return MetaAccountWithBalanceListingMixin(
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = metaAccountGroupingInteractor.metaAccountsWithTotalBalanceFlow().map { list ->
        list.toListWithHeaders(
            keyMapper = { mapMetaAccountTypeToUi(it, resourceManager) },
            valueMapper = { mapMetaAccountToUi(it) }
        )
    }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccount: MetaAccountWithTotalBalance) = with(metaAccount) {
        val icon = addressIconGenerator.createAddressIcon(
            accountId = metaAccount.substrateAccountId,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )

        MetaAccountUi(
            id = metaId,
            title = name,
            subtitle = totalBalance.formatAsCurrency(),
            isSelected = metaAccount.isSelected,
            isClickable = true,
            picture = icon,
            subtitleIconRes = null,
        )
    }
}
