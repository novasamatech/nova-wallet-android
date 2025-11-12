package io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentClaimContributionsBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ClaimContributionFragment : BaseFragment<ClaimContributionViewModel, FragmentClaimContributionsBinding>() {

    override fun createBinding() = FragmentClaimContributionsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.crowdloanClaimContributionsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.crowdloanClaimContributionsExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.crowdloanClaimContributionsConfirm.prepareForProgress(viewLifecycleOwner)
        binder.crowdloanClaimContributionsConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .claimContributions()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ClaimContributionViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.originFeeMixin, binder.crowdloanClaimContributionsExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(binder.crowdloanClaimContributionsConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.crowdloanClaimContributionsExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.crowdloanClaimContributionsExtrinsicInfo::setWallet)

        viewModel.redeemableAmountModelFlow.observe(binder.crowdloanClaimContributionsAmount::showLoadingState)
    }
}
