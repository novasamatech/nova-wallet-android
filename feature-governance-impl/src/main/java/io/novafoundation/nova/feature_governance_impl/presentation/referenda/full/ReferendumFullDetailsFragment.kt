package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import android.os.Bundle
import androidx.core.view.isVisible

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.copy.setupCopyText
import io.novafoundation.nova.common.presentation.CopierBottomSheet
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendumFullDetailsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.model.AddressAndAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide

import javax.inject.Inject

class ReferendumFullDetailsFragment : BaseFragment<ReferendumFullDetailsViewModel, FragmentReferendumFullDetailsBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumFullDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentReferendumFullDetailsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.referendumFullDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        binder.referendumFullDetailsPreImage.background = getRoundedCornerDrawable(R.color.block_background)

        binder.referendumFullDetailsApproveThreshold.showValueOrHide(primary = viewModel.approveThreshold)
        binder.referendumFullDetailsSupportThreshold.showValueOrHide(primary = viewModel.supportThreshold)
        binder.referendumFullDetailsVoteThreshold.showValueOrHide(primary = viewModel.voteThreshold)

        binder.referendumFullDetailsCallHash.showValueOrHide(primary = viewModel.callHash)
        viewModel.callHash?.let { hash ->
            binder.referendumFullDetailsCallHash.setOnClickListener { viewModel.copyCallHash() }
        }

        binder.referendumFullDetailsPreimageTitle.isVisible = viewModel.hasPreimage
        binder.referendumFullDetailsPlaceholder.isVisible = viewModel.isPreimageTooLong
        binder.referendumFullDetailsPreImage.isVisible = viewModel.isPreviewAvailable
        binder.referendumFullDetailsPreImage.text = viewModel.preImage

        binder.referendumFullDetailsProposal.setOnClickListener { viewModel.openProposal() }
        binder.referendumFullDetailsBeneficiary.setOnClickListener { viewModel.openBeneficiary() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendumFullDetailsFactory()
            .create(this, requireArguments().getParcelable(KEY_PAYLOAD)!!)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumFullDetailsViewModel) {
        setupExternalActions(viewModel)
        setupCopyText(viewModel)

        viewModel.proposerModel.observe { state ->
            updateProposerState(state)
        }

        viewModel.beneficiaryModel.observe { state ->
            updateBeneficiaryState(state)
        }

        viewModel.turnoutAmount.observe(binder.referendumFullDetailsTurnout::showAmountOrHide)

        viewModel.electorateAmount.observe(binder.referendumFullDetailsElectorate::showAmountOrHide)
    }

    private fun updateProposerState(state: LoadingState<AddressAndAmountModel?>) {
        if (state is LoadingState.Loaded) {
            val addressAndAmount = state.data
            if (addressAndAmount == null) {
                binder.referendumFullDetailsProposalContainer.makeGone()
            } else {
                binder.referendumFullDetailsProposal.makeVisible()
                binder.referendumFullDetailsProposal.showAddress(addressAndAmount.addressModel)

                binder.referendumFullDetailsDeposit.setVisible(addressAndAmount.amountModel != null)
                addressAndAmount.amountModel?.let { binder.referendumFullDetailsDeposit.showAmount(it) }
            }
        } else {
            binder.referendumFullDetailsProposal.showProgress()
            binder.referendumFullDetailsDeposit.showProgress()
        }
    }

    private fun updateBeneficiaryState(state: LoadingState<AddressAndAmountModel?>) {
        if (state is LoadingState.Loaded) {
            val addressAndAmount = state.data

            if (addressAndAmount == null) {
                binder.referendumFullDetailsBeneficiaryContainer.makeGone()
            } else {
                binder.referendumFullDetailsBeneficiary.makeVisible()
                binder.referendumFullDetailsBeneficiary.showAddress(addressAndAmount.addressModel)
                addressAndAmount.amountModel?.let { binder.referendumFullDetailsRequestedAmount.showAmount(it) }
            }
        } else {
            binder.referendumFullDetailsBeneficiary.showProgress()
            binder.referendumFullDetailsRequestedAmount.showProgress()
        }
    }
}
