package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.ChainAccountGroupUi
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun polkadotVaultAccountTypeAlert(
    pokadotVaultVariant: PolkadotVaultVariant,
    variantConfig: PolkadotVaultVariantConfig,
    resourceManager: ResourceManager
): AlertModel {
    return AlertModel(
        style = AlertView.Style(
            backgroundColorRes = R.color.block_background,
            iconRes = variantConfig.common.iconRes
        ),
        message = resourceManager.formatWithPolkadotVaultLabel(R.string.account_details_parity_signer_alert, pokadotVaultVariant)
    )
}

fun AccountInChain.polkadotVaultTitle(resourceManager: ResourceManager, metaAccount: MetaAccount): String {
    val address = projection?.address

    return if (address != null) {
        address
    } else {
        val polkadotVaultVariant = metaAccount.type.asPolkadotVaultVariantOrThrow()
        resourceManager.formatWithPolkadotVaultLabel(R.string.account_details_parity_signer_not_supported, polkadotVaultVariant)
    }
}

fun AccountInChain.From.mapToAccountGroupUi(resourceManager: ResourceManager): ChainAccountGroupUi {
    val resId = when (this) {
        AccountInChain.From.META_ACCOUNT -> R.string.account_shared_secret
        AccountInChain.From.CHAIN_ACCOUNT -> R.string.account_custom_secret
    }

    return ChainAccountGroupUi(
        id = name,
        title = resourceManager.getString(resId),
        action = null
    )
}

fun hasAccountComparator(): Comparator<AccountInChain> {
    return compareBy { it.hasChainAccount }
}

fun Comparator<AccountInChain>.withChainComparator(): Comparator<AccountInChain> {
    return then(Chain.accountInChainComparator())
}

fun Chain.Companion.accountInChainComparator(): Comparator<AccountInChain> {
    return Chain.defaultComparatorFrom(AccountInChain::chain)
}

val AccountInChain.hasChainAccount
    get() = projection != null
