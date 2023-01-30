package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setAddressOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumCallModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDetailsModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.applyTo
import io.novafoundation.nova.feature_governance_impl.presentation.view.setVoteModelOrHide
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsContainer
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDappList
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDescription
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsNumber
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsProgress
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsProposer
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsReadMore
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmount
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmountContainer
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmountFiat
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsScrollView
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsTimeline
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsTitle
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsToolbar
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsToolbarChips
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsTrack
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsVotingStatus
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsYourVote
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumFullDetails
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumTimelineContainer

class ReferendumDetailsFragment : BaseFragment<ReferendumDetailsViewModel>(), WithContextExtensions {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val providedContext: Context
        get() = requireContext()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_details, container, false)
    }

    override fun initViews() {
        referendumDetailsContainer.applyStatusBarInsets()

        referendumDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        referendumDetailsRequestedAmountContainer.background = getRoundedCornerDrawable(R.color.block_background)
        referendumDetailsTrack.background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        referendumDetailsNumber.background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        referendumFullDetails.background = getRoundedCornerDrawable(R.color.block_background)
            .withRippleMask()
        referendumTimelineContainer.background = getRoundedCornerDrawable(R.color.block_background)

        referendumDetailsReadMore.setOnClickListener {
            viewModel.readMoreClicked()
        }

        referendumDetailsVotingStatus.setPositiveVotersClickListener {
            viewModel.positiveVotesClicked()
        }

        referendumDetailsVotingStatus.setNegativeVotersClickListener {
            viewModel.negativeVotesClicked()
        }

        referendumDetailsDappList.onDAppClicked(viewModel::dAppClicked)

        referendumFullDetails.setOnClickListener {
            viewModel.fullDetailsClicked()
        }

        referendumDetailsVotingStatus.setStartVoteOnClickListener {
            viewModel.voteClicked()
        }

        referendumDetailsProposer.setOnClickListener {
            viewModel.proposerClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendumDetailsFactory()
            .create(this, requireArguments().getParcelable(KEY_PAYLOAD)!!)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumDetailsViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)

        viewModel.referendumDetailsModelFlow.observeWhenVisible {
            when (it) {
                is LoadingState.Loading -> {
                    setContentVisible(false)
                }
                is LoadingState.Loaded -> {
                    setContentVisible(true)

                    setReferendumState(it.data)
                }
            }
        }

        viewModel.proposerAddressModel.observeWhenVisible(referendumDetailsProposer::setAddressOrHide)

        viewModel.referendumCallModelFlow.observeWhenVisible(::setReferendumCall)

        viewModel.referendumDApps.observeWhenVisible(referendumDetailsDappList::setDAppsOrHide)

        viewModel.voteButtonState.observeWhenVisible(referendumDetailsVotingStatus::setVoteButtonState)

        viewModel.showFullDetails.observeWhenVisible(referendumFullDetails::setVisible)
    }

    private fun setReferendumState(model: ReferendumDetailsModel) {
        referendumDetailsTrack.setReferendumTrackModel(model.track)
        referendumDetailsNumber.setText(model.number)

        referendumDetailsTitle.text = model.title
        setDescription(model.description)

        referendumDetailsYourVote.setVoteModelOrHide(model.yourVote)

        referendumDetailsVotingStatus.setStatus(model.statusModel)
        referendumDetailsVotingStatus.setTimeEstimation(model.timeEstimation)
        referendumDetailsVotingStatus.setVotingModel(model.voting)
        referendumDetailsVotingStatus.setPositiveVoters(model.ayeVoters)
        referendumDetailsVotingStatus.setNegativeVoters(model.nayVoters)

        referendumDetailsTimeline.setTimeline(model.timeline)
    }

    // TODO we need a better way of managing views for specific calls when multiple calls will be supported
    private fun setReferendumCall(model: ReferendumCallModel?) {
        when (model) {
            is ReferendumCallModel.GovernanceRequest -> {
                referendumDetailsRequestedAmountContainer.makeVisible()
                referendumDetailsRequestedAmount.text = model.amount.token
                referendumDetailsRequestedAmountFiat.text = model.amount.fiat
            }
            null -> {
                referendumDetailsRequestedAmountContainer.makeGone()
            }
        }
    }

    private fun setContentVisible(visible: Boolean) {
        referendumDetailsToolbarChips.setVisible(visible)
        referendumDetailsScrollView.setVisible(visible)

        referendumDetailsProgress.setVisible(!visible)
    }

    private fun setDescription(maybeModel: ShortenedTextModel?) {
        maybeModel.applyTo(referendumDetailsDescription, referendumDetailsReadMore, viewModel.markwon)
    }
}
