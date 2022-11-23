package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.CopierBottomSheet
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsApproveThreshold
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsBeneficiary
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsBeneficiaryContainer
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsCallHash
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsDeposit
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsElectorate
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsPlaceholder
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsPreImage
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsPreimageTitle
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsProposal
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsProposalContainer
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsRequestedAmount
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsSupportThreshold
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsToolbar
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsTurnout
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsVoteThreshold
import javax.inject.Inject

class ReferendumFullDetailsFragment : BaseFragment<ReferendumFullDetailsViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumFullDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_referendum_full_details, container, false)
    }

    override fun initViews() {
        referendumFullDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        referendumFullDetailsPreImage.background = getRoundedCornerDrawable(R.color.block_background)

        referendumFullDetailsApproveThreshold.showValueOrHide(primary = viewModel.approveThreshold)
        referendumFullDetailsSupportThreshold.showValueOrHide(primary = viewModel.supportThreshold)
        referendumFullDetailsVoteThreshold.showValueOrHide(primary = viewModel.voteThreshold)

        referendumFullDetailsCallHash.showValueOrHide(primary = viewModel.callHash)
        viewModel.callHash?.let { hash ->
            referendumFullDetailsCallHash.setOnClickListener { showCopyingBottomSheet(hash) }
        }

        referendumFullDetailsPreimageTitle.isVisible = viewModel.hasPreimage
        referendumFullDetailsPlaceholder.isVisible = viewModel.isPreimageTooLong
        referendumFullDetailsPreImage.isVisible = viewModel.isPreviewAvailable
        referendumFullDetailsPreImage.text = viewModel.preImage

        referendumFullDetailsProposal.setOnClickListener { viewModel.openProposal() }
        referendumFullDetailsBeneficiary.setOnClickListener { viewModel.openBeneficiary() }
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

        viewModel.proposerModel.observe { addressAndAmount ->
            if (addressAndAmount == null) {
                referendumFullDetailsProposalContainer.makeGone()
            } else {
                referendumFullDetailsProposal.makeVisible()
                referendumFullDetailsProposal.showAddress(addressAndAmount.addressModel)
                addressAndAmount.amountModel?.let { referendumFullDetailsDeposit.showAmount(it) }
            }
        }

        viewModel.beneficiaryModel.observe { addressAndAmount ->
            if (addressAndAmount == null) {
                referendumFullDetailsBeneficiaryContainer.makeGone()
            } else {
                referendumFullDetailsBeneficiary.makeVisible()
                referendumFullDetailsBeneficiary.showAddress(addressAndAmount.addressModel)
                addressAndAmount.amountModel?.let { referendumFullDetailsRequestedAmount.showAmount(it) }
            }
        }

        viewModel.turnoutAmount.observe(referendumFullDetailsTurnout::showAmountOrHide)

        viewModel.electorateAmount.observe(referendumFullDetailsElectorate::showAmountOrHide)
    }

    private fun showCopyingBottomSheet(value: String) {
        CopierBottomSheet(
            requireContext(),
            value = value,
            buttonNameRes = R.string.referendum_full_details_copy_hash
        ).show()
    }
}
