package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSetupStakingTypeBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.SetupStakingTypeAdapter

class SetupStakingTypeFragment : BaseFragment<SetupStakingTypeViewModel, FragmentSetupStakingTypeBinding>(), SetupStakingTypeAdapter.ItemAssetHandler {

    companion object {

        private val PAYLOAD_KEY = "SetupStakingTypeFragment.Payload"

        fun getArguments(payload: SetupStakingTypePayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun createBinding() = FragmentSetupStakingTypeBinding.inflate(layoutInflater)

    private val adapter = SetupStakingTypeAdapter(this)

    override fun initViews() {
        binder.setupStakingTypeToolbar.applyStatusBarInsets()
        binder.setupStakingTypeToolbar.setRightActionClickListener { viewModel.donePressed() }
        binder.setupStakingTypeToolbar.setHomeButtonListener { viewModel.backPressed() }
        binder.setupStakingTypeList.adapter = adapter
        binder.setupStakingTypeList.itemAnimator = null

        onBackPressed { viewModel.backPressed() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setupStakingType()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SetupStakingTypeViewModel) {
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.closeConfirmationAction)
        observeValidations(viewModel)

        viewModel.dataHasBeenChanged.observe { binder.setupStakingTypeToolbar.setRightActionEnabled(it) }

        viewModel.stakingTypeModels.observe {
            adapter.submitList(it)
        }
    }

    override fun stakingTypeClicked(stakingTypeRVItem: EditableStakingTypeRVItem, position: Int) {
        viewModel.stakingTypeClicked(stakingTypeRVItem, position)
    }

    override fun stakingTargetClicked(position: Int) {
        viewModel.stakingTargetClicked(position)
    }
}
