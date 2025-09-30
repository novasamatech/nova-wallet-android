package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators

import android.os.Bundle
import android.view.View

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentDelegateDelegatorsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoterModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.list.DelegatorsAdapter

class DelegateDelegatorsFragment : BaseFragment<DelegateDelegatorsViewModel, FragmentDelegateDelegatorsBinding>(), DelegatorsAdapter.Handler {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: DelegateDelegatorsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentDelegateDelegatorsBinding.inflate(layoutInflater)

    private val delegatorsAdapter = DelegatorsAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun initViews() {
        binder.delegateDelegatorsList.setHasFixedSize(true)
        binder.delegateDelegatorsList.adapter = delegatorsAdapter

        binder.delegateDelegatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .delegateDelegatorsFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DelegateDelegatorsViewModel) {
        setupExternalActions(viewModel)

        viewModel.delegatorModels.observe { state ->
            when (state) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loaded -> {
                    delegatorsAdapter.submitList(state.data)
                    binder.delegateDelegatorsList.makeVisible()
                    binder.delegateDelegatorsProgress.makeGone()
                }
                ExtendedLoadingState.Loading -> {
                    binder.delegateDelegatorsList.makeGone()
                    binder.delegateDelegatorsProgress.makeVisible()
                }
            }
        }

        viewModel.delegatorsCount.observe { state ->
            if (state is ExtendedLoadingState.Loaded) {
                binder.delegateDelegatorsCount.text = state.data
                binder.delegateDelegatorsCount.makeVisible()
            } else {
                binder.delegateDelegatorsCount.makeGone()
            }
        }
    }

    override fun onVoterClick(voter: VoterModel) {
        viewModel.delegatorClicked(voter)
    }
}
