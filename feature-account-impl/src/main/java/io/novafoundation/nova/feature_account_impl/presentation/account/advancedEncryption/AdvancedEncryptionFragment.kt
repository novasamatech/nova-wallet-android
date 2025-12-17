package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption

import android.content.Context
import android.os.Bundle

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useInputValue
import io.novafoundation.nova.common.view.InputField
import io.novafoundation.nova.common.view.LabeledTextView
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentAdvancedEncryptionBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.EncryptionTypeChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AdvancedEncryptionFragment : BaseFragment<AdvancedEncryptionViewModel, FragmentAdvancedEncryptionBinding>() {

    companion object {

        private const val PAYLOAD = "CreateAccountFragment.payload"

        fun getBundle(payload: AdvancedEncryptionModePayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentAdvancedEncryptionBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.advancedEncryptionToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        binder.advancedEncryptionApply.setOnClickListener {
            viewModel.applyClicked()
        }

        binder.advancedEncryptionSubstrateCryptoType.setOnClickListener {
            viewModel.substrateEncryptionClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .advancedEncryptionComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: AdvancedEncryptionViewModel) {
        observeValidations(viewModel)

        binder.advancedEncryptionApply.setVisible(viewModel.applyVisible)

        viewModel.substrateCryptoTypeInput.bindTo(binder.advancedEncryptionSubstrateCryptoType)
        viewModel.substrateDerivationPathInput.bindTo(
            binder.advancedEncryptionSubstrateDerivationPath,
            viewModel::substrateDerivationPathChanged
        )

        viewModel.ethereumCryptoTypeInput.bindTo(binder.advancedEncryptionEthereumCryptoType)
        viewModel.ethereumDerivationPathInput.bindTo(
            binder.advancedEncryptionEthereumDerivationPath,
            viewModel::ethereumDerivationPathChanged
        )

        viewModel.showSubstrateEncryptionTypeChooserEvent.observeEvent {
            showEncryptionChooser(requireContext(), it)
        }
    }

    private fun Flow<Input<String>>.bindTo(view: InputField, onTextChanged: (String) -> Unit) {
        observe { input ->
            with(view) {
                useInputValue(input) {
                    if (content.text.toString() != it) {
                        content.setText(it)
                    }
                }
            }
        }

        view.content.onTextChanged(onTextChanged)
    }

    private fun Flow<Input<CryptoTypeModel>>.bindTo(view: LabeledTextView) {
        observe { input ->
            with(view) {
                useInputValue(input) { setMessage(it.name) }
            }
        }
    }

    private fun showEncryptionChooser(
        context: Context,
        payload: DynamicListBottomSheet.Payload<CryptoTypeModel>,
    ) {
        EncryptionTypeChooserBottomSheetDialog(
            context = context,
            payload = payload,
            onClicked = { _, item -> viewModel.substrateEncryptionChanged(item) }
        ).show()
    }
}
