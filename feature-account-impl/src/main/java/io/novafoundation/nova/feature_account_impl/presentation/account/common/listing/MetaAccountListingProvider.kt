package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountListingMixinFactory(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val accountInteractor: AccountInteractor,
) {

    fun create(
        coroutineScope: CoroutineScope,
        itemSelectRules: MetaAccountSelectRules = CurrentMetaAccountSelectRules()
    ): MetaAccountListingMixin {
        return MetaAccountListingProvider(
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            accountInteractor = accountInteractor,
            itemSelectRules = itemSelectRules,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountListingProvider(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val accountInteractor: AccountInteractor,
    private val itemSelectRules: MetaAccountSelectRules,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = accountInteractor.metaAccountsFlow().map { list ->
        list.toListWithHeaders(
            keyMapper = ::mapMetaAccountTypeToUi,
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
            name = name,
            isSelected = itemSelectRules.select(this),
            picture = icon,
            totalBalance = totalBalance.formatAsCurrency()
        )
    }

    private fun mapMetaAccountTypeToUi(type: LightMetaAccount.Type): ChipLabelModel? = when (type) {
        LightMetaAccount.Type.SECRETS -> null
        LightMetaAccount.Type.WATCH_ONLY -> ChipLabelModel(
            iconRes = R.drawable.ic_watch,
            title = resourceManager.getString(R.string.account_watch_only)
        )
        LightMetaAccount.Type.PARITY_SIGNER -> ChipLabelModel(
            iconRes = R.drawable.ic_parity_signer,
            title = resourceManager.getString(R.string.account_parity_signer)
        )
    }
}

class CurrentMetaAccountSelectRules : MetaAccountSelectRules {
    override suspend fun select(metaAccountWithBalance: MetaAccountWithTotalBalance): Boolean {
        return metaAccountWithBalance.isSelected
    }
}
