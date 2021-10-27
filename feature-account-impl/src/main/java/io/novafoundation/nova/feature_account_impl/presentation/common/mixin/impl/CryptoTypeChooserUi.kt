package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import android.content.Context
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.AdvancedBlockView
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.EncryptionTypeChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel

fun <V> BaseFragment<V>.setupCryptoTypeChooserUi(
    viewModel: V,
    ui: AdvancedBlockView,
    ignoreSelectionFrozen: Boolean = false
) where V : BaseViewModel, V : WithCryptoTypeChooserMixin {
    viewModel.cryptoTypeChooserMixin.encryptionTypeChooserEvent.observeEvent {
        showEncryptionChooser(requireContext(), it, viewModel)
    }

    viewModel.cryptoTypeChooserMixin.selectedEncryptionTypeFlow.observe {
        ui.setEncryption(it.name)
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

private fun showEncryptionChooser(
    context: Context,
    payload: DynamicListBottomSheet.Payload<CryptoTypeModel>,
    viewModel: WithCryptoTypeChooserMixin
) {
    EncryptionTypeChooserBottomSheetDialog(
        context = context,
        payload = payload,
        onClicked = viewModel.cryptoTypeChooserMixin::selectedEncryptionChanged
    ).show()
}
