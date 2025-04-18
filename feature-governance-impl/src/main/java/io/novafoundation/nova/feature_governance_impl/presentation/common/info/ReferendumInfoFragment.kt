package io.novafoundation.nova.feature_governance_impl.presentation.common.info

import android.os.Bundle
import android.view.Gravity
import androidx.core.view.isGone
import androidx.core.view.isVisible

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setAddressOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendumInfoBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.share.setupReferendumSharing
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel

class ReferendumInfoFragment : BaseFragment<ReferendumInfoViewModel, FragmentReferendumInfoBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(descriptionPayload: ReferendumInfoPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, descriptionPayload)
            }
        }
    }

    override fun createBinding() = FragmentReferendumInfoBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.referendumInfoToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.referendumInfoToolbar.setRightActionClickListener { viewModel.shareButtonClicked() }

        binder.referendumInfoProposer.setOnClickListener {
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

        viewModel.titleFlow.observe { binder.referendumInfoTitle.text = it }
        viewModel.subtitleFlow.observe { binder.referendumInfoDescription.text = it }
        viewModel.idFlow.observe { binder.referendumInfoNumber.setText(it) }
        viewModel.trackFlow.observe { binder.referendumInfoTrack.setReferendumTrackModel(it) }
        viewModel.timeEstimation.observe { binder.referendumInfoTime.setReferendumTimeEstimation(it, Gravity.END) }
        viewModel.proposerAddressModel.observeWhenVisible(binder.referendumInfoProposer::setAddressOrHide)
        viewModel.isLoadingState.observe {
            binder.referendumInfoContainer.isGone = it
            binder.referendumInfoProgress.isVisible = it
        }
    }
}
