package io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentRebagBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class RebagFragment : BaseFragment<RebagViewModel, FragmentRebagBinding>() {

    override val binder by viewBinding(FragmentRebagBinding::bind)

    override fun initViews() {
        binder.rebagToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.rebagConfirm.prepareForProgress(viewLifecycleOwner)
        binder.rebagConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.rebagExtrinsicInfo.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .rebagComponentFractory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: RebagViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.rebagHints)

        setupFeeLoading(viewModel, binder.rebagExtrinsicInfo.fee)
        viewModel.originAddressModelFlow.observe(binder.rebagExtrinsicInfo::setAccount)
        viewModel.walletModel.observe(binder.rebagExtrinsicInfo::setWallet)

        viewModel.rebagMovementModel.observe {
            binder.rebagCurrentBag.showValue(it.currentBag)
            binder.rebagNewBag.showValue(it.newBag)
        }

        viewModel.showNextProgress.observe(binder.rebagConfirm::setProgressState)
    }
}
