package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.copyAddressClicked
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsApproveThreshold
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsBeneficiary
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsBeneficiaryContainer
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsCallHash
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsDeposit
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsElectorate
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsPlaceholder
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsPreImage
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsProposal
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsProposalContainer
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsRequestedAmount
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsSupportThreshold
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsToolbar
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsTurnout
import kotlinx.android.synthetic.main.fragment_referendum_full_details.referendumFullDetailsVoteThreshold
import kotlinx.coroutines.flow.first

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
        referendumFullDetailsPreImage.background = getRoundedCornerDrawable(R.color.white_8)

        referendumFullDetailsProposal.setPrimaryValueIcon(R.drawable.ic_info_16, R.color.white_64)
        referendumFullDetailsBeneficiary.setPrimaryValueIcon(R.drawable.ic_info_16, R.color.white_64)
        referendumFullDetailsCallHash.setPrimaryValueIcon(R.drawable.ic_info_16, R.color.white_64)

        viewModel.approveThreshold?.let {
            referendumFullDetailsApproveThreshold.showValue(it)
            referendumFullDetailsApproveThreshold.makeVisible()
        }
        viewModel.supportThreshold?.let {
            referendumFullDetailsSupportThreshold.showValue(it)
            referendumFullDetailsSupportThreshold.makeVisible()
        }
        viewModel.voteThreshold?.let {
            referendumFullDetailsVoteThreshold.showValue(it)
            referendumFullDetailsVoteThreshold.makeVisible()
        }

        viewModel.callHash?.let {
            referendumFullDetailsCallHash.showValue(it)
            referendumFullDetailsCallHash.makeVisible()
        }

        referendumFullDetailsPlaceholder.isGone = viewModel.hasPreImage
        referendumFullDetailsPreImage.isVisible = viewModel.hasPreImage
        referendumFullDetailsPreImage.text = viewModel.preImage

        referendumFullDetailsProposal.setOnClickListener { viewModel.openProposal() }
        referendumFullDetailsBeneficiary.setOnClickListener { viewModel.openBeneficiary() }
        referendumFullDetailsCallHash.setOnClickListener { viewModel.openCallHash() }
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
        setupExternalActions(viewModel) { context, payload ->
            ChainAccountActionsSheet(
                context,
                payload,
                onCopy = viewModel::copyAddressClicked,
                onViewExternal = viewModel::viewExternalClicked,
                onChange = viewModel::changeChainAccountClicked,
                onExport = viewModel::exportClicked,
                availableAccountActions = viewModel.availableAccountActions.first()
            )
        }
        viewModel.proposerAddressModelFlow.observe { addressAndAmount ->
            if (addressAndAmount == null) {
                referendumFullDetailsProposalContainer.makeGone()
            } else {
                referendumFullDetailsProposal.makeVisible()
                referendumFullDetailsProposal.showAddress(addressAndAmount.addressModel)
                addressAndAmount.amountModel?.let { referendumFullDetailsDeposit.showAmount(it) }
            }
        }

        viewModel.beneficiaryAddressModelFlow.observe { addressAndAmount ->
            if (addressAndAmount == null) {
                referendumFullDetailsBeneficiaryContainer.makeGone()
            } else {
                referendumFullDetailsBeneficiary.makeVisible()
                referendumFullDetailsBeneficiary.showAddress(addressAndAmount.addressModel)
                addressAndAmount.amountModel?.let { referendumFullDetailsRequestedAmount.showAmount(it) }
            }
        }

        viewModel.turnoutAmount.observe {
            referendumFullDetailsTurnout?.showAmount(it)
            referendumFullDetailsTurnout.makeVisible()
        }

        viewModel.electorateAmount.observe {
            referendumFullDetailsElectorate?.showAmount(it)
            referendumFullDetailsElectorate.makeVisible()
        }
    }
}
