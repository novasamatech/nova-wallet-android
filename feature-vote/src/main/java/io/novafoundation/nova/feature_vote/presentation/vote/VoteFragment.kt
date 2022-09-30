package io.novafoundation.nova.feature_vote.presentation.vote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.tabs.addTab
import io.novafoundation.nova.common.view.tabs.setupWithRouter
import io.novafoundation.nova.feature_vote.R
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureComponent
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import kotlinx.android.synthetic.main.fragment_vote.voteAvatar
import kotlinx.android.synthetic.main.fragment_vote.voteContainer
import kotlinx.android.synthetic.main.fragment_vote.voteTabs
import javax.inject.Inject

class VoteFragment : BaseFragment<VoteViewModel>() {

    @Inject
    lateinit var router: VoteRouter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_vote, container, false)
    }

    override fun initViews() {
        voteContainer.applyStatusBarInsets()

        voteTabs.addTab(R.string.common_governance)
        voteTabs.addTab(R.string.crowdloan_crowdloan)

        voteTabs.setupWithRouter(router, viewLifecycleOwner.lifecycle)

        voteAvatar.setOnClickListener { viewModel.avatarClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<VoteFeatureComponent>(this, VoteFeatureApi::class.java)
            .voteComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: VoteViewModel) {
        viewModel.selectedWalletModel.observe(voteAvatar::setModel)
    }
}
