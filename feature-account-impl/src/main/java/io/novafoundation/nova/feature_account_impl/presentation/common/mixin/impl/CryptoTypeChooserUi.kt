package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.AdvancedBlockView

fun <V> BaseFragment<V>.setupCryptoTypeChooserUi(
    viewModel: V,
    ui: AdvancedBlockView,
    ignoreSelectionFrozen: Boolean = false
) where V : BaseViewModel, V : WithCryptoTypeChooserMixin {


    viewModel.cryptoTypeChooserMixin.selectedEncryptionTypeFlow.observe {
        ui.setEncryption(it.name)
    }

    ui.setOnEncryptionTypeClickListener {
        viewModel.cryptoTypeChooserMixin.chooseEncryptionClicked()
    }

    if (ignoreSelectionFrozen.not()) {
        viewModel.cryptoTypeChooserMixin.selectionFrozen.observe { frozen ->
            ui.setEnabled(ui.encryptionTypeField, enabled = !frozen)
        }
    }
}

