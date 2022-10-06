package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVoting
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.VoteType
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVote
import kotlinx.android.synthetic.main.fragment_referenda_list.*
import javax.inject.Inject

class ReferendaListFragment : BaseFragment<ReferendaListViewModel>(), ReferendaListAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val referendaHeaderAdapter by lazy { ReferendaListHeaderAdapter(imageLoader) }

    private val referendaListAdapter by lazy { ReferendaListAdapter(this) }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(referendaHeaderAdapter, referendaListAdapter) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referenda_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun initViews() {
        referendaList.adapter = adapter
        referendaListAdapter.submitList(
            listOf(
                ReferendaStatusModel("Active", "2"),
                ReferendumModel(
                    ReferendumStatus.PASSING,
                    "Implement cool thing",
                    ReferendumTimeEstimation(TimerValue.fromCurrentTime(0), R.drawable.ic_time_16, R.color.yellow),
                    listOf(),
                    ReferendumVoting(
                        true,
                        "Threshold: 1.551 KSM of 1.43 KSM",
                        "Aye: 80%",
                        "Nay: 20%",
                        "To pass: 20%"
                    ),
                    YourVote(VoteType.POSITIVE, "Your vote is so cool")
                ),
                ReferendumModel(
                    ReferendumStatus.PASSING,
                    "Implement cool thing",
                    ReferendumTimeEstimation(TimerValue.fromCurrentTime(0), R.drawable.ic_time_16, R.color.yellow),
                    listOf(),
                    ReferendumVoting(
                        true,
                        "Threshold: 1.551 KSM of 1.43 KSM",
                        "Aye: 80%",
                        "Nay: 20%",
                        "To pass: 20%"
                    ),
                    null
                ),
                ReferendaStatusModel("Completed", "2"),
                ReferendumModel(
                    ReferendumStatus.NOT_PASSING,
                    "Implement cool thing",
                    ReferendumTimeEstimation(TimerValue.fromCurrentTime(0), R.drawable.ic_time_16, R.color.yellow),
                    listOf(),
                    null,
                    null
                )
            )
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendaListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendaListViewModel) {
    }

    override fun onReferendaClick() {
    }
}
