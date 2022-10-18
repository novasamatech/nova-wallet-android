package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description

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
import kotlinx.android.synthetic.main.fragment_referendum_description.referendumDescriptionTitle
import kotlinx.android.synthetic.main.fragment_referendum_description.referendumDescriptionToolbar
import kotlinx.android.synthetic.main.fragment_referendum_description.referendumFullDescription

class ReferendumDescriptionFragment : BaseFragment<ReferendumDescriptionViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(referendumDescriptionPayload: ReferendumDescriptionPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, referendumDescriptionPayload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_description, container, false)
    }

    override fun initViews() {
        referendumDescriptionToolbar.setHomeButtonListener { viewModel.backClicked() }
        val payload = arguments?.getParcelable<ReferendumDescriptionPayload>(KEY_PAYLOAD)
        payload?.let { setPayload(it) }

        referendumDescriptionTitle.setTextOrHide(viewModel.referendumTitle)
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(this, GovernanceFeatureApi::class.java)
            .referendumDescriptionFactory()
            .create(this, arguments?.getParcelable(KEY_PAYLOAD)!!)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumDescriptionViewModel) {
        viewModel.markdownDescription.observe {
            viewModel.markwon.setParsedMarkdown(referendumFullDescription, it)
        }
    }

    private fun setPayload(payload: ReferendumDescriptionPayload) {
    }
}
