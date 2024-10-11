package io.novafoundation.nova.feature_governance_impl.presentation.common.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

class DescriptionFragment : BaseFragment<DescriptionViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(descriptionPayload: DescriptionPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, descriptionPayload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_description, container, false)
    }

    override fun initViews() {
        descriptionToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(this, GovernanceFeatureApi::class.java)
            .descriptionFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DescriptionViewModel) {
        viewModel.markdownDescription.observe {
            viewModel.markwon.setParsedMarkdown(descriptionFullDescription, it)
        }

        descriptionTitle.setTextOrHide(viewModel.title)
        descriptionToolbar.setTitle(viewModel.toolbarTitle)
    }
}
