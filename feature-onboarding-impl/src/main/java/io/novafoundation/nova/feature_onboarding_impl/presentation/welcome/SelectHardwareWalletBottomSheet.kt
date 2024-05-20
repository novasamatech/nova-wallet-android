package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.model.HardwareWalletModel

class SelectHardwareWalletBottomSheet(
    context: Context,
    private val onSuccess: (HardwareWalletModel) -> Unit
) : FixedListBottomSheet(context) {

    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider

    init {
        polkadotVaultVariantConfigProvider = FeatureUtils
            .getFeature<AccountFeatureApi>(context, AccountFeatureApi::class.java)
            .polkadotVaultVariantConfigProvider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.account_select_hardware_wallet)

        polkadotVaultItem(PolkadotVaultVariant.POLKADOT_VAULT)

        textItem(
            iconRes = R.drawable.ic_ledger,
            titleRes = R.string.account_ledger_nano_x_generic,
            showArrow = true,
            onClick = { onSuccess(HardwareWalletModel.LedgerGeneric) }
        )

        textItem(
            iconRes = R.drawable.ic_ledger_legacy,
            titleRes = R.string.account_ledger_nano_x_legacy,
            showArrow = true,
            applyIconTint = false,
            onClick = { onSuccess(HardwareWalletModel.LedgerLegacy) }
        )

        polkadotVaultItem(PolkadotVaultVariant.PARITY_SIGNER)
    }

    private fun polkadotVaultItem(variant: PolkadotVaultVariant) {
        val config = polkadotVaultVariantConfigProvider.variantConfigFor(variant)

        textItem(
            iconRes = config.common.iconRes,
            titleRes = config.common.nameRes,
            showArrow = true,
            onClick = { onSuccess(HardwareWalletModel.PolkadotVault(variant)) }
        )
    }
}
