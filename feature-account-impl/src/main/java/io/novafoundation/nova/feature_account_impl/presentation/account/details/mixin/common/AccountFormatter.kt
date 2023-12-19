package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.*
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi

class AccountFormatterFactory(
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager
) {

    fun create(accountTitleFormatter: suspend (AccountInChain) -> String): AccountFormatter {
        return AccountFormatter(
            iconGenerator,
            resourceManager,
            accountTitleFormatter
        )
    }
}

class AccountFormatter(
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val accountTitleFormatter: suspend (AccountInChain) -> String
) {

    suspend fun formatChainAccountProjection(accountInChain: AccountInChain, availableActions: Set<AccountAction>): AccountInChainUi {
        return with(accountInChain) {
            val accountIcon = projection?.let {
                iconGenerator.createAddressIcon(it.accountId, AddressIconGenerator.SIZE_SMALL, backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT)
            } ?: resourceManager.getDrawable(R.drawable.ic_warning_filled)

            val availableActionsForChain = availableActionsFor(accountInChain, availableActions)
            val canViewAddresses = accountInChain.projection != null
            val canDoAnyActions = availableActionsForChain.isNotEmpty() || canViewAddresses

            AccountInChainUi(
                chainUi = mapChainToUi(chain),
                addressOrHint = accountTitleFormatter(accountInChain),
                address = projection?.address,
                accountIcon = accountIcon,
                actionsAvailable = canDoAnyActions
            )
        }
    }

    private suspend fun availableActionsFor(accountInChain: AccountInChain, availableActions: Set<AccountAction>): Set<AccountAction> {
        return availableActions.filterToSet { action ->
            when (action) {
                AccountAction.CHANGE -> true
                AccountAction.EXPORT -> accountInChain.projection != null
            }
        }
    }
}

fun baseAccountTitleFormatter(resourceManager: ResourceManager): (AccountInChain) -> String {
    return { accountInChain ->
        accountInChain.projection?.address ?: resourceManager.getString(R.string.account_no_chain_projection)
    }
}
