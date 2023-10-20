package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.SetupStakingTypeAdapter
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeList
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeToolbar

class SetupStakingTypeFragment : BaseFragment<SetupStakingTypeViewModel>(), SetupStakingTypeAdapter.ItemAssetHandler {

    companion object {

        private val PAYLOAD_KEY = "SetupStakingTypeFragment.Payload"

        fun getArguments(payload: SetupStakingTypePayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    private val adapter = SetupStakingTypeAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_staking_type, container, false)
    }

    override fun initViews() {
        setupStakingTypeToolbar.applyStatusBarInsets()
        setupStakingTypeToolbar.setRightActionClickListener { viewModel.donePressed() }
        setupStakingTypeToolbar.setHomeButtonListener { viewModel.backPressed() }
        setupStakingTypeList.adapter = adapter
        setupStakingTypeList.itemAnimator = null

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

        viewModel.dataHasBeenChanged.observe { setupStakingTypeToolbar.setRightActionEnabled(it) }

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
