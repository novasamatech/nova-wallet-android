package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentContributeConfirmBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload

import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ConfirmContributeFragment : BaseFragment<ConfirmContributeViewModel, FragmentContributeConfirmBinding>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(payload: ConfirmContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentContributeConfirmBinding::bind)

    override fun initViews() {
        binder.confirmContributeContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        binder.confirmContributeToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.confirmContributeConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmContributeConfirm.setOnClickListener { viewModel.nextClicked() }

        binder.confirmContributeOriginAcount.setWholeClickListener { viewModel.originAccountClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmContributePayload>("KEY_PAYLOAD")

        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .confirmContributeFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmContributeViewModel) {
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(binder.confirmContributeConfirm::setProgressState)

        viewModel.assetModelFlow.observe {
            binder.confirmContributeAmount.setAssetBalance(it.assetBalance)
            binder.confirmContributeAmount.setAssetName(it.tokenSymbol)
            binder.confirmContributeAmount.loadAssetImage(it.imageUrl)
        }

        binder.confirmContributeAmount.amountInput.setText(viewModel.selectedAmount)

        viewModel.enteredFiatAmountFlow.observe {
            it.let(binder.confirmContributeAmount::setFiatAmount)
        }

        viewModel.feeFlow.observe(binder.confirmContributeFee::setFeeStatus)

        with(binder.confirmContributeReward) {
            val reward = viewModel.estimatedReward

            setVisible(reward != null)

            reward?.let { showValue(it) }
        }

        viewModel.crowdloanInfoFlow.observe {
            binder.confirmContributeLeasingPeriod.showValue(it.leasePeriod, it.leasedUntil)
        }

        viewModel.selectedAddressModelFlow.observe {
            binder.confirmContributeOriginAcount.setMessage(it.nameOrAddress)
            binder.confirmContributeOriginAcount.setTextIcon(it.image)
        }

        viewModel.bonusFlow.observe {
            binder.confirmContributeBonus.setVisible(it != null)

            it?.let(binder.confirmContributeBonus::showValue)
        }

        viewModel.customizationConfiguration.filterNotNull().observe { (customization, customViewState) ->
            customization.injectViews(binder.confirmContributeContainer, customViewState, viewLifecycleOwner.lifecycleScope)
        }
    }
}
