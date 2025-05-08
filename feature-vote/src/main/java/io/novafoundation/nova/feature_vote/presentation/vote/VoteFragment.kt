package io.novafoundation.nova.feature_vote.presentation.vote

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_vote.databinding.FragmentVoteBinding
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureComponent
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

import javax.inject.Inject

class VoteFragment : BaseFragment<VoteViewModel, FragmentVoteBinding>() {

    override fun createBinding() = FragmentVoteBinding.inflate(layoutInflater)

    @Inject
    lateinit var router: VoteRouter

    override fun initViews() {
        binder.voteContainer.applyStatusBarInsets()
        val adapter = VotePagerAdapter(this, router)
        binder.voteViewPager.adapter = adapter
        binder.voteTabs.setupWithViewPager2(binder.voteViewPager, adapter::getPageTitle)

        binder.voteAppBar.onWalletClick { viewModel.avatarClicked() }
        binder.voteAppBar.onWalletConnectClick { viewModel.walletConnectClicked() }
        binder.voteAppBar.onSettingsClick { }
    }

    override fun inject() {
        FeatureUtils.getFeature<VoteFeatureComponent>(this, VoteFeatureApi::class.java)
            .voteComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: VoteViewModel) {
        viewModel.selectedWalletModel.observe {
            binder.voteAppBar.setSelectedWallet(it.typeIcon?.icon, it.name)
        }

        viewModel.walletConnectAccountSessions.observe {
            binder.voteAppBar.setWalletConnectActive(it.hasConnections)
        }
    }
}
