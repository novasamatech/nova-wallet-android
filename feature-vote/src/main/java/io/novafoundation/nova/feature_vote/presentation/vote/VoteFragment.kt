package io.novafoundation.nova.feature_vote.presentation.vote

import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.feature_vote.databinding.FragmentVoteBinding
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureComponent
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

import javax.inject.Inject

class VoteFragment : BaseFragment<VoteViewModel, FragmentVoteBinding>() {

    override fun createBinding() = FragmentVoteBinding.inflate(layoutInflater)

    @Inject
    lateinit var router: VoteRouter

    override fun applyInsets(rootView: View) {
        binder.voteContainer.applyStatusBarInsets()
    }

    override fun initViews() {
        binder.voteViewPager.adapter = VotePagerAdapter(this, router)
        binder.voteViewPager.isUserInputEnabled = false

        binder.voteAvatar.setOnClickListener { viewModel.avatarClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<VoteFeatureComponent>(this, VoteFeatureApi::class.java)
            .voteComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: VoteViewModel) {
        viewModel.selectedWalletModel.observe(binder.voteAvatar::setModel)
    }
}
