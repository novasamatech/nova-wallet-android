package io.novafoundation.nova.feature_vote.presentation.vote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_vote.R
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureComponent
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import kotlinx.android.synthetic.main.fragment_vote.voteAvatar
import kotlinx.android.synthetic.main.fragment_vote.voteContainer
import kotlinx.android.synthetic.main.fragment_vote.voteTabs
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_vote.voteViewPager

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
        val adapter = VotePagerAdapter(this, router)
        voteViewPager.adapter = adapter
        voteTabs.setupWithViewPager2(voteViewPager, adapter::getPageTitle)

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
