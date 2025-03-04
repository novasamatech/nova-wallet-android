package io.novafoundation.nova.feature_governance_impl.presentation.common.info

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setAddressOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.share.setupReferendumSharing
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoContainer
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoDescription
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoNumber
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoProgress
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoProposer
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoTime
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoTitle
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoToolbar
import kotlinx.android.synthetic.main.fragment_referendum_info.referendumInfoTrack

class ReferendumInfoFragment : BaseFragment<ReferendumInfoViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(descriptionPayload: ReferendumInfoPayload): Bundle {
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
        return inflater.inflate(R.layout.fragment_referendum_info, container, false)
    }

    override fun initViews() {
        referendumInfoToolbar.setHomeButtonListener { viewModel.backClicked() }

        referendumInfoToolbar.setRightActionClickListener { viewModel.shareButtonClicked() }

        referendumInfoProposer.setOnClickListener {
            viewModel.proposerClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(this, GovernanceFeatureApi::class.java)
            .referendumInfoFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumInfoViewModel) {
        setupExternalActions(viewModel)
        setupReferendumSharing(viewModel.shareReferendumMixin)

        viewModel.titleFlow.observe { referendumInfoTitle.text = it }
        viewModel.subtitleFlow.observe { referendumInfoDescription.text = it }
        viewModel.idFlow.observe { referendumInfoNumber.setText(it) }
        viewModel.trackFlow.observe { referendumInfoTrack.setReferendumTrackModel(it) }
        viewModel.timeEstimation.observe { referendumInfoTime.setReferendumTimeEstimation(it, Gravity.END) }
        viewModel.proposerAddressModel.observeWhenVisible(referendumInfoProposer::setAddressOrHide)
        viewModel.isLoadingState.observe {
            referendumInfoContainer.isGone = it
            referendumInfoProgress.isVisible = it
        }
    }
}
