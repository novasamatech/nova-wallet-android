package io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_impl.R
import io.novafoundation.nova.feature_external_sign_impl.di.ExternalSignFeatureComponent
import kotlinx.android.synthetic.main.fragment_dapp_extrinsic_details.extrinsicDetailsContent
import kotlinx.android.synthetic.main.fragment_dapp_extrinsic_details.signExtrinsicToolbar

class ExternalExtrinsicDetailsFragment : BaseBottomSheetFragment<ExternalExtrinsicDetailsViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(extrinsicContent: String) = bundleOf(PAYLOAD_KEY to extrinsicContent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_extrinsic_details, container, false)
    }

    override fun initViews() {
        signExtrinsicToolbar.setHomeButtonListener { viewModel.closeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<ExternalSignFeatureComponent>(this, ExternalSignFeatureApi::class.java)
            .extrinsicDetailsComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExternalExtrinsicDetailsViewModel) {
        extrinsicDetailsContent.text = viewModel.extrinsicContent
    }
}
