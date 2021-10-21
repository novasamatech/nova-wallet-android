package io.novafoundation.nova.feature_account_impl.presentation.importing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.common.accountSource.SourceTypeChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.setupCryptoTypeChooserUi
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.setupForcedChainUi
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.FileRequester
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.JsonImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.MnemonicImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.RawSeedImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.RequestCode
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.ImportSourceView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.JsonImportView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.MnemonicImportView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.SeedImportView
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.AdvancedBlockView.FieldState
import kotlinx.android.synthetic.main.fragment_import_account.advancedBlockView
import kotlinx.android.synthetic.main.fragment_import_account.importForcedChain
import kotlinx.android.synthetic.main.fragment_import_account.nextBtn
import kotlinx.android.synthetic.main.fragment_import_account.sourceTypeContainer
import kotlinx.android.synthetic.main.fragment_import_account.sourceTypeInput
import kotlinx.android.synthetic.main.fragment_import_account.toolbar
import javax.inject.Inject

class ImportAccountFragment : BaseFragment<ImportAccountViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        private const val PAYLOAD = "network_type"

        fun getBundle(payload: AddAccountPayload): Bundle {

            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    private var sourceViews: List<View>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_account, container, false)
    }

    override fun initViews() {
        toolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        sourceTypeInput.setWholeClickListener { viewModel.openSourceChooserClicked() }

        nextBtn.setOnClickListener { viewModel.nextClicked() }

        nextBtn.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .importAccountComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ImportAccountViewModel) {
        sourceViews = viewModel.sourceTypes.map {
            val view = createSourceView(it)

            view.observeSource(it, viewLifecycleOwner)
            view.observeCommon(viewModel, viewLifecycleOwner)

            observeFeatures(it)

            view
        }

        setupForcedChainUi(viewModel, importForcedChain, imageLoader)

        viewModel.showSourceSelectorChooserLiveData.observeEvent(::showTypeChooser)

        viewModel.selectedSourceTypeLiveData.observe {
            val index = viewModel.sourceTypes.indexOf(it)

            sourceTypeContainer.removeAllViews()
            sourceTypeContainer.addView(sourceViews!![index])

            sourceTypeInput.setMessage(it.nameRes)
        }

        viewModel.nextButtonState.observe(nextBtn::setState)

        viewModel.changeableAdvancedFields.observe {
            val derivationPathState = getFieldState(it, disabledState = FieldState.HIDDEN)

            with(advancedBlockView) {
                configure(derivationPathField, derivationPathState)
            }
        }

        setupCryptoTypeChooserUi(viewModel, advancedBlockView, ignoreSelectionFrozen = true)
        viewModel.cryptoTypeChooserEnabled.observe {
            advancedBlockView.setEnabled(advancedBlockView.encryptionTypeField, it)
        }

        advancedBlockView.derivationPathEditText.bindTo(viewModel.derivationPathLiveData, viewLifecycleOwner)
    }

    private fun observeFeatures(source: ImportSource) {
        if (source is FileRequester) {
            source.chooseJsonFileEvent.observeEvent {
                openFilePicker(it)
            }
        }
    }

    private fun getFieldState(isEnabled: Boolean, disabledState: FieldState = FieldState.DISABLED): FieldState {
        return if (isEnabled) FieldState.NORMAL else disabledState
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.let { viewModel.systemCallResultReceived(requestCode, it) }
    }

    private fun showTypeChooser(it: Payload<ImportSource>) {
        SourceTypeChooserBottomSheetDialog(requireActivity(), it, viewModel::sourceTypeChanged)
            .show()
    }

    private fun openFilePicker(it: RequestCode) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        startActivityForResult(intent, it)
    }

    private fun createSourceView(source: ImportSource): ImportSourceView {
        val context = requireContext()

        return when (source) {
            is JsonImportSource -> JsonImportView(context)
            is MnemonicImportSource -> MnemonicImportView(context)
            is RawSeedImportSource -> SeedImportView(context)
        }
    }
}
