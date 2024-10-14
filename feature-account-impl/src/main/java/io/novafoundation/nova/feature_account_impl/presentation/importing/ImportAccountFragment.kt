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
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.FileRequester
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.RequestCode

import javax.inject.Inject

class ImportAccountFragment : BaseFragment<ImportAccountViewModel, FragmentImportAccountBinding>() {

    override val binder by viewBinding(FragmentImportAccountBinding::bind)

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

    override fun initViews() {
        binder.importAccountToolbar.setRightActionClickListener {
            viewModel.optionsClicked()
        }
        binder.importAccountToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        binder.importAccountContinue.setOnClickListener { viewModel.nextClicked() }
        binder.importAccountContinue.prepareForProgress(viewLifecycleOwner)
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
        binder.importAccountTitle.setText(viewModel.importSource.nameRes)

        val sourceView = viewModel.importSource.initializeView(viewModel, fragment = this)
        binder.importAccountSourceContainer.addView(sourceView)

        observeFeatures(viewModel.importSource)

        binder.importAccountToolbar.setRightIconVisible(viewModel.importSource.encryptionOptionsAvailable)

        viewModel.nextButtonState.observe(binder.importAccountContinue::setState)
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
