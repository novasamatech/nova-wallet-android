package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setAddressOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendumDetailsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.share.setupReferendumSharing
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumCallModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDetailsModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.applyTo

class ReferendumDetailsFragment : BaseFragment<ReferendumDetailsViewModel, FragmentReferendumDetailsBinding>(), WithContextExtensions {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentReferendumDetailsBinding.inflate(layoutInflater)

    override val providedContext: Context
        get() = requireContext()

    override fun initViews() {
        binder.referendumDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        binder.referendumDetailsToolbar.setRightActionClickListener { viewModel.shareButtonClicked() }

        binder.referendumDetailsRequestedAmountContainer.background = getRoundedCornerDrawable(R.color.block_background)
        binder.referendumDetailsTrack.background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        binder.referendumDetailsNumber.background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        binder.referendumFullDetails.background = getRoundedCornerDrawable(R.color.block_background)
            .withRippleMask()
        binder.referendumTimelineContainer.background = getRoundedCornerDrawable(R.color.block_background)

        binder.referendumDetailsReadMore.setOnClickListener {
            viewModel.readMoreClicked()
        }

        binder.referendumDetailsVotingStatus.setPositiveVotersClickListener {
            viewModel.positiveVotesClicked()
        }

        binder.referendumDetailsVotingStatus.setNegativeVotersClickListener {
            viewModel.negativeVotesClicked()
        }

        binder.referendumDetailsVotingStatus.setAbstainVotersClickListener {
            viewModel.abstainVotesClicked()
        }

        binder.referendumDetailsDappList.onDAppClicked(viewModel::dAppClicked)

        binder.referendumFullDetails.setOnClickListener {
            viewModel.fullDetailsClicked()
        }

        binder.referendumDetailsVotingStatus.setStartVoteOnClickListener {
            viewModel.voteClicked()
        }

        binder.referendumDetailsProposer.setOnClickListener {
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
        setupReferendumSharing(viewModel.shareReferendumMixin)
        observeValidations(viewModel)
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.referendumNotAwaitableAction)

        viewModel.referendumDetailsModelFlow.observeWhenVisible { loadingState ->
            setContentVisible(loadingState.isLoaded())
            binder.referendumDetailsProgress.isVisible = loadingState.isLoading()
            loadingState.dataOrNull?.let { setReferendumState(it) }
        }

        viewModel.proposerAddressModel.observeWhenVisible(binder.referendumDetailsProposer::setAddressOrHide)

        viewModel.referendumCallModelFlow.observeWhenVisible(::setReferendumCall)

        viewModel.referendumDApps.observeWhenVisible(binder.referendumDetailsDappList::setDAppsOrHide)

        viewModel.voteButtonState.observeWhenVisible(binder.referendumDetailsVotingStatus::setVoteButtonState)

        viewModel.showFullDetails.observeWhenVisible(binder.referendumFullDetails::setVisible)
    }

    private fun setReferendumState(model: ReferendumDetailsModel) {
        binder.referendumDetailsTrack.setReferendumTrackModel(model.track)
        binder.referendumDetailsNumber.setText(model.number)

        binder.referendumDetailsTitle.text = model.title
        setDescription(model.description)

        binder.referendumDetailsYourVote.setModel(model.yourVote)

        binder.referendumDetailsVotingStatus.letOrHide(model.statusModel) {
            binder.referendumDetailsVotingStatus.setStatus(it)
        }
        binder.referendumDetailsVotingStatus.setTimeEstimation(model.timeEstimation)
        binder.referendumDetailsVotingStatus.setVotingModel(model.voting)
        binder.referendumDetailsVotingStatus.setPositiveVoters(model.ayeVoters)
        binder.referendumDetailsVotingStatus.setNegativeVoters(model.nayVoters)
        binder.referendumDetailsVotingStatus.setAbstainVoters(model.abstainVoters)

        binder.referendumTimelineContainer.letOrHide(model.timeline) {
            binder.referendumDetailsTimeline.setTimeline(it)
        }
    }

    // TODO we need a better way of managing views for specific calls when multiple calls will be supported
    private fun setReferendumCall(model: ReferendumCallModel?) {
        when (model) {
            is ReferendumCallModel.GovernanceRequest -> {
                binder.referendumDetailsRequestedAmountContainer.makeVisible()
                binder.referendumDetailsRequestedAmount.text = model.amount.token
                binder.referendumDetailsRequestedAmountFiat.text = model.amount.fiat
            }

            null -> {
                binder.referendumDetailsRequestedAmountContainer.makeGone()
            }
        }
    }

    private fun setContentVisible(visible: Boolean) {
        binder.referendumDetailsToolbarChips.setVisible(visible)
        binder.referendumDetailsScrollView.setVisible(visible)
    }

    private fun setDescription(model: ShortenedTextModel?) {
        model.applyTo(binder.referendumDetailsDescription, binder.referendumDetailsReadMore, viewModel.markwon)
    }
}
