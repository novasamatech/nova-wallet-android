package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.model.HardwareWalletModel

class SelectHardwareWalletBottomSheet(
    context: Context,
    private val onSuccess: (HardwareWalletModel) -> Unit
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.account_select_hardware_wallet)

        item(
            icon = R.drawable.ic_parity_signer,
            titleRes = R.string.account_parity_signer,
            showArrow = true,
            onClick = { onSuccess(HardwareWalletModel.PARITY_SIGNER) }
        )

        item(
            icon = R.drawable.ic_ledger,
            titleRes = R.string.account_ledger_nano_x,
            showArrow = true,
            onClick = { onSuccess(HardwareWalletModel.LEDGER_NANO_X) }
        )
    }
}
