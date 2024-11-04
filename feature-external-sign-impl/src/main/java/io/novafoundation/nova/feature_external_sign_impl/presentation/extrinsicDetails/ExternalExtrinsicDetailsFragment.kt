package io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails

import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_impl.databinding.FragmentDappExtrinsicDetailsBinding
import io.novafoundation.nova.feature_external_sign_impl.di.ExternalSignFeatureComponent

class ExternalExtrinsicDetailsFragment : BaseBottomSheetFragment<ExternalExtrinsicDetailsViewModel, FragmentDappExtrinsicDetailsBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(extrinsicContent: String) = bundleOf(PAYLOAD_KEY to extrinsicContent)
    }

    override fun createBinding() = FragmentDappExtrinsicDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.signExtrinsicToolbar.setHomeButtonListener { viewModel.closeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<ExternalSignFeatureComponent>(this, ExternalSignFeatureApi::class.java)
            .extrinsicDetailsComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExternalExtrinsicDetailsViewModel) {
        binder.extrinsicDetailsContent.text = viewModel.extrinsicContent
    }
}
