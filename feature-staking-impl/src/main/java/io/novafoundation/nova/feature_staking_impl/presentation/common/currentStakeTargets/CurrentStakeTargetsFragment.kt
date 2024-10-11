package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel

abstract class CurrentStakeTargetsFragment<V : CurrentStakeTargetsViewModel> : BaseFragment<V>(), CurrentStakeTargetAdapter.Handler {

    lateinit var adapter: CurrentStakeTargetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_current_validators, container, false)
    }

    override fun initViews() {
        currentValidatorsContainer.applyStatusBarInsets()

        adapter = CurrentStakeTargetAdapter(this)
        currentValidatorsList.adapter = adapter

        currentValidatorsList.setHasFixedSize(true)

        currentValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }

        currentValidatorsToolbar.setRightActionClickListener { viewModel.changeClicked() }
    }

    override fun subscribe(viewModel: V) {
        viewModel.currentStakeTargetsFlow.observe { loadingState ->
            when (loadingState) {
                is LoadingState.Loading -> {
                    currentValidatorsList.makeGone()
                    currentValidatorsProgress.makeVisible()
                }

                is LoadingState.Loaded -> {
                    currentValidatorsList.makeVisible()
                    currentValidatorsProgress.makeGone()

                    adapter.submitList(loadingState.data)
                }
            }
        }

        viewModel.warningFlow.observe {
            if (it != null) {
                currentValidatorsOversubscribedMessage.makeVisible()
                currentValidatorsOversubscribedMessage.setMessage(it)
            } else {
                currentValidatorsOversubscribedMessage.makeGone()
            }
        }

        viewModel.titleFlow.observe(currentValidatorsToolbar::setTitle)
    }

    override fun infoClicked(stakeTargetModel: SelectedStakeTargetModel) {
        viewModel.stakeTargetInfoClicked(stakeTargetModel.addressModel.address)
    }
}
