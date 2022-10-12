package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline.TimelineLayout
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotersView
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotingThresholdView
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsAddress
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDappList
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsDescription
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsNumber
import kotlinx.android.synthetic.main.fragment_referendum_details.referendumDetailsReadMore
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
        referendumDetailsAddress.setAddress(ShapeDrawable(), "Address")
        referendumDetailsAddress.setOnClickListener { }
        referendumDetailsTitle.text = "Oh mine!"
        referendumDetailsDescription.text = "Sovereign Nature Initiative (SNI) is a non-profit foundation that has brought together multiple partners and engineers from the Kusama ecosystem including Kodadot, Unique Network, Kilt Protocol, Momentum, and Ocean Protocol, to support the building of Web3 capacities for wildlife"
        referendumDetailsReadMore.setOnClickListener { }
        referendumDetailsYourVote.setVoteType(R.string.referendum_vote_positive_type, R.color.green)
        referendumDetailsYourVote.setVoteValue("60 votes", "10 KSM x 6x")
        referendumDetailsVotingStatus.setStatus("Executed", R.color.multicolor_green_100)
        referendumDetailsVotingStatus.setTimeEstimation("3:10:22", R.drawable.ic_fire, R.color.yellow)
        referendumDetailsVotingStatus.setThreshold(VotingThresholdView.ThresholdModel(
            "Threshold: 0.8",
            R.drawable.ic_red_cross,
            R.color.red,
            0.8f,
            0.9f,
            "80%",
            "90%",
            "10%"
        ))
        referendumDetailsVotingStatus.setPositiveVoters(VotersView.VotersModel(
            R.string.referendum_vote_positive_type, R.color.green, "60", "12222"
        ))
        referendumDetailsVotingStatus.setPositiveVotersClickListener {}

        referendumDetailsVotingStatus.setNegativeVoters(VotersView.VotersModel(
            R.string.referendum_vote_negative_type, R.color.red, "10", "2000"
        ))
        referendumDetailsVotingStatus.setNegativeVotersClickListener {}

        referendumDetailsDappList.addDApp("Title", "Subtitle", null) {}
        referendumDetailsDappList.addDApp("Title", "Subtitle", null) {}
        referendumDetailsTimeline.setTimeline(
            TimelineLayout.Timeline(
                listOf(
                    TimelineLayout.TimelineState("Start", "at 4:00", R.drawable.ic_info_16, R.color.yellow),
                    TimelineLayout.TimelineState("Start", "at 4:00", R.drawable.ic_info_16, R.color.yellow)
                ),
                true
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
