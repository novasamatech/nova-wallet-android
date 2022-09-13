package io.novafoundation.nova.feature_account_impl.presentation.importing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.FileRequester
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.RequestCode
import kotlinx.android.synthetic.main.fragment_import_account.importAccountContinue
import kotlinx.android.synthetic.main.fragment_import_account.importAccountSourceContainer
import kotlinx.android.synthetic.main.fragment_import_account.importAccountTitle
import kotlinx.android.synthetic.main.fragment_import_account.importAccountToolbar
import javax.inject.Inject

class ImportAccountFragment : BaseFragment<ImportAccountViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        private const val PAYLOAD = "ImportAccountFragment.PAYLOAD"

        fun getBundle(payload: ImportAccountPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_account, container, false)
    }

    override fun initViews() {
        importAccountToolbar.setRightActionClickListener {
            viewModel.optionsClicked()
        }
        importAccountToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        importAccountContinue.setOnClickListener { viewModel.nextClicked() }
        importAccountContinue.prepareForProgress(viewLifecycleOwner)
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
        importAccountTitle.setText(viewModel.importSource.nameRes)

        val sourceView = viewModel.importSource.initializeView(viewModel, fragment = this)
        importAccountSourceContainer.addView(sourceView)

        observeFeatures(viewModel.importSource)

        importAccountToolbar.setRightIconVisible(viewModel.importSource.encryptionOptionsAvailable)

        viewModel.nextButtonState.observe(importAccountContinue::setState)
    }

    private fun observeFeatures(source: ImportSource) {
        if (source is FileRequester) {
            source.chooseJsonFileEvent.observeEvent {
                openFilePicker(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.let { viewModel.systemCallResultReceived(requestCode, it) }
    }

    private fun openFilePicker(it: RequestCode) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        startActivityForResult(intent, it)
    }
}
