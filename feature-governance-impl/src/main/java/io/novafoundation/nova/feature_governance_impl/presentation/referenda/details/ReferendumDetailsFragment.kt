package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline.TimelineLayout
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotersView
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsAddress
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDappList
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDescription
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsNumber
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsReadMore
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmount
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmountContainer
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsRequestedAmountFiat
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsTimeline
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsTitle
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsToolbar
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
        referendumDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        referendumDetailsRequestedAmountContainer.background = getRoundedCornerDrawable(R.color.white_8)
        referendumDetailsTrack.background = getRoundedCornerDrawable(R.color.white_16, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        referendumDetailsNumber.background = getRoundedCornerDrawable(R.color.white_16, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
        referendumFullDetails.background = getRoundedCornerDrawable(R.color.white_8)
            .withRippleMask(getRippleMask())
        referendumTimelineContainer.background = getRoundedCornerDrawable(R.color.white_8)

        // Placeholders
        referendumDetailsTrack.text = "Main track"
        referendumDetailsTrack.setDrawableStart(R.drawable.ic_info_16, widthInDp = 16, tint = R.color.white, paddingInDp = 4)
        referendumDetailsNumber.text = "#256"
        referendumDetailsTrack.setOnClickListener {}
        referendumDetailsNumber.setOnClickListener {}
        referendumDetailsAddress.setAddress(context!!.getDrawable(R.drawable.ic_fire)!!, "Address")
        referendumDetailsAddress.setOnClickListener { }
        referendumDetailsTitle.text = "Oh mine!"
        referendumDetailsDescription.text = "Sovereign Nature Initiative (SNI) is a non-profit foundation that has brought together multiple partners and " +
            "engineers from the Kusama ecosystem including Kodadot, Unique Network, Kilt Protocol, Momentum, and Ocean Protocol, to support the building of " +
            "Web3 capacities for wildlife"
        referendumDetailsReadMore.setOnClickListener { }
        referendumDetailsRequestedAmount.text = "1,158.47 KSM"
        referendumDetailsRequestedAmountFiat.text = "\$51,158.3"
        referendumDetailsYourVote.setVoteType(R.string.referendum_vote_positive_type, R.color.green)
        referendumDetailsYourVote.setVoteValue("60 votes", "10 KSM x 6x")
        referendumDetailsVotingStatus.setStatus("Executed", R.color.multicolor_green_100)
        referendumDetailsVotingStatus.setThreshold(
            ReferendumVotingModel(
                0.8f,
                0.9f,
                R.drawable.ic_close,
                R.color.red,
                "Threshold: 16,492 of 15,392.5 KSM ",
                "Aye: 17.5%",
                "Nay: 82.5%",
                "To pass: 20%"
            )
        )
        referendumDetailsVotingStatus.setPositiveVoters(
            VotersView.VotersModel(
                R.string.referendum_vote_positive_type,
                R.color.green,
                "638 voters",
                "1,398 votes"
            )
        )
        referendumDetailsVotingStatus.setPositiveVotersClickListener {}

        referendumDetailsVotingStatus.setNegativeVoters(
            VotersView.VotersModel(
                R.string.referendum_vote_negative_type,
                R.color.red,
                "28 voter",
                "1,398 votes"
            )
        )
        referendumDetailsVotingStatus.setNegativeVotersClickListener {}
        referendumDetailsVotingStatus.showDetails(false)
        referendumDetailsDappList.addDApp("Title", "Subtitle", null) {}
        referendumDetailsDappList.addDApp("Title", "Subtitle", null) {}
        referendumDetailsTimeline.setTimeline(
            TimelineLayout.Timeline(
                listOf(
                    TimelineLayout.TimelineState("Start", "Sept 1, 2022 04:44:31", R.drawable.ic_info_16, R.color.yellow),
                    TimelineLayout.TimelineState("Executed", "Reject in 18 days", null, R.color.white_64)
                ),
                false
            )
        )
        referendumFullDetails.setOnClickListener { }
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
    }
}
