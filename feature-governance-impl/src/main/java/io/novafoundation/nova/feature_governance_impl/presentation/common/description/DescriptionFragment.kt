package io.novafoundation.nova.feature_governance_impl.presentation.common.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentDescriptionBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

class DescriptionFragment : BaseFragment<DescriptionViewModel, FragmentDescriptionBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(descriptionPayload: DescriptionPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, descriptionPayload)
            }
        }
    }

    override val binder by viewBinding(FragmentDescriptionBinding::bind)

    override fun initViews() {
        binder.descriptionToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(this, GovernanceFeatureApi::class.java)
            .descriptionFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DescriptionViewModel) {
        viewModel.markdownDescription.observe {
            viewModel.markwon.setParsedMarkdown(binder.descriptionFullDescription, it)
        }

        binder.descriptionTitle.setTextOrHide(viewModel.title)
        binder.descriptionToolbar.setTitle(viewModel.toolbarTitle)
    }
}
