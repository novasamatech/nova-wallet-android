package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerAccountPayload
import kotlinx.android.synthetic.main.fragment_import_parity_signer_preview.previewImportParitySignerAccounts
import kotlinx.android.synthetic.main.fragment_import_parity_signer_preview.previewImportParitySignerContinue
import kotlinx.android.synthetic.main.fragment_import_parity_signer_preview.previewImportParitySignerDescription
import kotlinx.android.synthetic.main.fragment_import_parity_signer_preview.previewImportParitySignerToolbar
import javax.inject.Inject

class PreviewImportParitySignerFragment : BaseFragment<PreviewImportParitySignerViewModel>(), ChainAccountsAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ChainAccountsAdapter(this, imageLoader)
    }

    companion object {

        private const val PAYLOAD_KEY = "PreviewImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerAccountPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_import_parity_signer_preview, container, false)

    override fun initViews() {
        previewImportParitySignerToolbar.applyStatusBarInsets()
        previewImportParitySignerToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        previewImportParitySignerAccounts.setHasFixedSize(true)
        previewImportParitySignerAccounts.adapter = adapter

        previewImportParitySignerContinue.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .previewImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: PreviewImportParitySignerViewModel) {
        setupExternalActions(viewModel)

        viewModel.chainAccountProjections.observe(adapter::submitList)

        previewImportParitySignerDescription.text = viewModel.title
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
