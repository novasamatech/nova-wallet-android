package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentCurrentValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel

abstract class CurrentStakeTargetsFragment<V : CurrentStakeTargetsViewModel> : BaseFragment<V, FragmentCurrentValidatorsBinding>(),
    CurrentStakeTargetAdapter.Handler {

    override val binder by viewBinding(FragmentCurrentValidatorsBinding::bind)

    lateinit var adapter: CurrentStakeTargetAdapter

    override fun initViews() {
        binder.currentValidatorsContainer.applyStatusBarInsets()

        adapter = CurrentStakeTargetAdapter(this)
        binder.currentValidatorsList.adapter = adapter

        binder.currentValidatorsList.setHasFixedSize(true)

        binder.currentValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.currentValidatorsToolbar.setRightActionClickListener { viewModel.changeClicked() }
    }

    override fun subscribe(viewModel: V) {
        viewModel.currentStakeTargetsFlow.observe { loadingState ->
            when (loadingState) {
                is LoadingState.Loading -> {
                    binder.currentValidatorsList.makeGone()
                    binder.currentValidatorsProgress.makeVisible()
                }

                is LoadingState.Loaded -> {
                    binder.currentValidatorsList.makeVisible()
                    binder.currentValidatorsProgress.makeGone()

                    adapter.submitList(loadingState.data)
                }
            }
        }

        viewModel.warningFlow.observe {
            if (it != null) {
                binder.currentValidatorsOversubscribedMessage.makeVisible()
                binder.currentValidatorsOversubscribedMessage.setMessage(it)
            } else {
                binder.currentValidatorsOversubscribedMessage.makeGone()
            }
        }

        viewModel.titleFlow.observe(binder.currentValidatorsToolbar::setTitle)
    }

    override fun infoClicked(stakeTargetModel: SelectedStakeTargetModel) {
        viewModel.stakeTargetInfoClicked(stakeTargetModel.addressModel.address)
    }
}
