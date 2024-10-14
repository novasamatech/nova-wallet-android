package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select

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
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentContributeBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload

import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class CrowdloanContributeFragment : BaseFragment<CrowdloanContributeViewModel, FragmentContributeBinding>() {

    @Inject protected lateinit var imageLoader: ImageLoader

    companion object {

        const val KEY_BONUS_LIVE_DATA = "KEY_BONUS_LIVE_DATA"

        fun getBundle(payload: ContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentContributeBinding::bind)

    override fun initViews() {
        binder.crowdloanContributeContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        binder.crowdloanContributeToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.crowdloanContributeContinue.prepareForProgress(viewLifecycleOwner)
        binder.crowdloanContributeContinue.setOnClickListener { viewModel.nextClicked() }

        binder.crowdloanContributeLearnMore.setOnClickListener { viewModel.learnMoreClicked() }

        binder.crowdloanContributeBonus.setOnClickListener { viewModel.bonusClicked() }
    }

    override fun inject() {
        val payload = argument<ContributePayload>(KEY_PAYLOAD)

        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .selectContributeFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: CrowdloanContributeViewModel) {
        observeRetries(viewModel)
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)

        viewModel.showNextProgress.observe(binder.crowdloanContributeContinue::setProgressState)

        viewModel.assetModelFlow.observe {
            binder.crowdloanContributeAmount.setAssetBalance(it.assetBalance)
            binder.crowdloanContributeAmount.setAssetName(it.tokenSymbol)
            binder.crowdloanContributeAmount.loadAssetImage(it.imageUrl)
        }

        binder.crowdloanContributeAmount.amountInput.bindTo(viewModel.enteredAmountFlow, lifecycleScope)

        viewModel.enteredFiatAmountFlow.observe {
            it.let(binder.crowdloanContributeAmount::setFiatAmount)
        }

        viewModel.feeLiveData.observe(binder.crowdloanContributeFee::setFeeStatus)

        viewModel.estimatedRewardFlow.observe { reward ->
            binder.crowdloanContributeReward.setVisible(reward != null)

            reward?.let {
                binder.crowdloanContributeReward.showValue(reward)
            }
        }

        viewModel.unlockHintFlow.observe(binder.crowdloanContributeUnlockHint::setText)

        viewModel.crowdloanDetailModelFlow.observe {
            binder.crowdloanContributeLeasingPeriod.showValue(it.leasePeriod, it.leasedUntil)
        }

        binder.crowdloanContributeToolbar.setTitle(viewModel.title)

        binder.crowdloanContributeLearnMore.setVisible(viewModel.learnCrowdloanModel != null)

        viewModel.learnCrowdloanModel?.let {
            binder.crowdloanContributeLearnMore.title.text = it.text
            binder.crowdloanContributeLearnMore.loadIcon(it.iconLink, imageLoader)
        }

        viewModel.bonusDisplayFlow.observe {
            binder.crowdloanContributeBonus.setVisible(it != null)

            binder.crowdloanContributeBonusReward.text = it
        }

        viewModel.customizationConfiguration.filterNotNull().observe { (customization, customViewState) ->
            customization.injectViews(binder.crowdloanContributeContainer, customViewState, viewLifecycleOwner.lifecycleScope)
        }
    }
}
