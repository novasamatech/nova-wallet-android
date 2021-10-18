package jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl

import android.content.Context
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.AdvancedBlockView
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.encryption.EncryptionTypeChooserBottomSheetDialog
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel

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
