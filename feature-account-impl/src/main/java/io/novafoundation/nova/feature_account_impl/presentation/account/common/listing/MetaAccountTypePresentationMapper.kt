package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.common.view.TintedIcon
import io.novafoundation.nova.common.view.TintedIcon.Companion.asTintedIcon
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountChipGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker

class MetaAccountTypePresentationMapper(
    private val resourceManager: ResourceManager,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
) {

    suspend fun mapMetaAccountTypeToUi(type: LightMetaAccount.Type): AccountChipGroupRvItem? {
        var ledgerGenericAvailable: Boolean? = null

        // Cache result of `ledgerMigrationTracker.anyChainSupportsMigrationApp()` in the method scope
        val genericLedgerAvailabilityChecker: GenericLedgerAvailabilityChecker = {
            if (ledgerGenericAvailable == null) {
                ledgerGenericAvailable = ledgerMigrationTracker.anyChainSupportsMigrationApp()
            }

            ledgerGenericAvailable!!
        }

        val icon = iconFor(type, genericLedgerAvailabilityChecker)

        val label = when (type) {
            LightMetaAccount.Type.SECRETS -> null
            LightMetaAccount.Type.WATCH_ONLY -> resourceManager.getString(R.string.account_watch_only)

            LightMetaAccount.Type.PARITY_SIGNER, LightMetaAccount.Type.POLKADOT_VAULT -> {
                val config = polkadotVaultVariantConfigProvider.variantConfigFor(type.asPolkadotVaultVariantOrThrow())
                resourceManager.getString(config.common.nameRes)
            }

            LightMetaAccount.Type.LEDGER_LEGACY -> if (genericLedgerAvailabilityChecker()) {
                resourceManager.getString(R.string.accounts_ledger_legacy)
            } else {
                resourceManager.getString(R.string.common_ledger)
            }

            LightMetaAccount.Type.LEDGER -> resourceManager.getString(R.string.common_ledger)

            LightMetaAccount.Type.PROXIED -> resourceManager.getString(R.string.account_proxieds)
        }

        return if (icon != null && label != null) {
            AccountChipGroupRvItem(ChipLabelModel(icon, label))
        } else {
            null
        }
    }

    suspend fun iconFor(type: LightMetaAccount.Type): TintedIcon? {
        return iconFor(type) { ledgerMigrationTracker.anyChainSupportsMigrationApp() }
    }

    private suspend fun iconFor(
        type: LightMetaAccount.Type,
        genericLedgerAvailable: GenericLedgerAvailabilityChecker
    ): TintedIcon? {
        return when (type) {
            LightMetaAccount.Type.SECRETS -> null
            LightMetaAccount.Type.WATCH_ONLY -> R.drawable.ic_watch_only_filled.asTintedIcon(canApplyOwnTint = true)
            LightMetaAccount.Type.PARITY_SIGNER, LightMetaAccount.Type.POLKADOT_VAULT -> {
                val config = polkadotVaultVariantConfigProvider.variantConfigFor(type.asPolkadotVaultVariantOrThrow())

                config.common.iconRes.asTintedIcon(canApplyOwnTint = true)
            }

            LightMetaAccount.Type.LEDGER_LEGACY -> if (genericLedgerAvailable()) {
                R.drawable.ic_ledger_legacy.asTintedIcon(canApplyOwnTint = false)
            } else {
                R.drawable.ic_ledger.asTintedIcon(canApplyOwnTint = true)
            }

            LightMetaAccount.Type.LEDGER -> R.drawable.ic_ledger.asTintedIcon(canApplyOwnTint = true)
            LightMetaAccount.Type.PROXIED -> R.drawable.ic_proxy.asTintedIcon(canApplyOwnTint = true)
        }
    }
}

private typealias GenericLedgerAvailabilityChecker = suspend () -> Boolean
