package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmAmount
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmCollator
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmConfirm
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmContainer
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_mythos_unbond_confirm.mythosUnbondConfirmToolbar

class ConfirmUnbondMythosFragment : BaseFragment<ConfirmUnbondMythosViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmUnbondMythosFragment.Payload"

        fun getBundle(payload: ConfirmUnbondMythosPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_mythos_unbond_confirm, container, false)
    }

    override fun initViews() {
        mythosUnbondConfirmContainer.applyStatusBarInsets()

        mythosUnbondConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        mythosUnbondConfirmExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        mythosUnbondConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        mythosUnbondConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        mythosUnbondConfirmCollator.setOnClickListener { viewModel.collatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmUnbondMythosFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmUnbondMythosViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, mythosUnbondConfirmExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(mythosUnbondConfirmConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(mythosUnbondConfirmExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(mythosUnbondConfirmExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(mythosUnbondConfirmCollator::showAddress)
        viewModel.amountModel.observe(mythosUnbondConfirmAmount::setAmount)
    }
}
