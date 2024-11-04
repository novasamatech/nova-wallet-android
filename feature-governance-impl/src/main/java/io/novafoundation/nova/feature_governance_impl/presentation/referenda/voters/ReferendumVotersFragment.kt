package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.os.Bundle
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendumVotersBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.VoterItemDecoration
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.VotersAdapter

import javax.inject.Inject

class ReferendumVotersFragment : BaseFragment<ReferendumVotersViewModel, FragmentReferendumVotersBinding>(), VotersAdapter.Handler {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumVotersPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentReferendumVotersBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val votersAdapter by lazy(LazyThreadSafetyMode.NONE) { VotersAdapter(this, imageLoader) }

    override fun initViews() {
        binder.referendumVotersToolbar.setTitle(viewModel.title)
        binder.referendumVotersList.setHasFixedSize(true)
        binder.referendumVotersList.adapter = votersAdapter
        binder.referendumVotersList.addItemDecoration(VoterItemDecoration(requireContext(), votersAdapter))
        binder.referendumVotersToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendumVotersFactory()
            .create(this, arguments!!.getParcelable(KEY_PAYLOAD)!!)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumVotersViewModel) {
        setupExternalActions(viewModel)

        viewModel.voterModels.observe {
            if (it is LoadingState.Loaded) {
                val voters = it.data
                votersAdapter.submitList(voters)
                binder.referendumVotersPlaceholder.isVisible = voters.isEmpty()
                binder.referendumVotersList.isVisible = voters.isNotEmpty()
                binder.referendumVotersCount.makeVisible()
                binder.referendumVotersProgress.makeGone()
            } else {
                binder.referendumVotersPlaceholder.makeGone()
                binder.referendumVotersProgress.makeVisible()
            }
        }

        viewModel.votersCount.observe(binder.referendumVotersCount::setText)
    }

    override fun onVoterClick(position: Int) {
        viewModel.voterClicked(position)
    }

    override fun onExpandItemClick(position: Int) {
        viewModel.expandVoterClicked(position)
    }
}
