package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
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
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeAmount
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeBonus
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeBonusReward
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeContainer
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeContinue
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeFee
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeLearnMore
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeLeasingPeriod
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeReward
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeToolbar
import kotlinx.android.synthetic.main.fragment_contribute.crowdloanContributeUnlockHint
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class CrowdloanContributeFragment : BaseFragment<CrowdloanContributeViewModel>() {

    @Inject protected lateinit var imageLoader: ImageLoader

    companion object {

        const val KEY_BONUS_LIVE_DATA = "KEY_BONUS_LIVE_DATA"

        fun getBundle(payload: ContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_contribute, container, false)
    }

    override fun initViews() {
        crowdloanContributeContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        crowdloanContributeToolbar.setHomeButtonListener { viewModel.backClicked() }
        crowdloanContributeContinue.prepareForProgress(viewLifecycleOwner)
        crowdloanContributeContinue.setOnClickListener { viewModel.nextClicked() }

        crowdloanContributeLearnMore.setOnClickListener { viewModel.learnMoreClicked() }

        crowdloanContributeBonus.setOnClickListener { viewModel.bonusClicked() }
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

        viewModel.showNextProgress.observe(crowdloanContributeContinue::setProgressState)

        viewModel.assetModelFlow.observe {
            crowdloanContributeAmount.setAssetBalance(it.assetBalance)
            crowdloanContributeAmount.setAssetName(it.tokenSymbol)
            crowdloanContributeAmount.loadAssetImage(it.icon)
        }

        crowdloanContributeAmount.amountInput.bindTo(viewModel.enteredAmountFlow, lifecycleScope)

        viewModel.enteredFiatAmountFlow.observe {
            it.let(crowdloanContributeAmount::setFiatAmount)
        }

        viewModel.feeLiveData.observe(crowdloanContributeFee::setFeeStatus)

        viewModel.estimatedRewardFlow.observe { reward ->
            crowdloanContributeReward.setVisible(reward != null)

            reward?.let {
                crowdloanContributeReward.showValue(reward)
            }
        }

        viewModel.unlockHintFlow.observe(crowdloanContributeUnlockHint::setText)

        viewModel.crowdloanDetailModelFlow.observe {
            crowdloanContributeLeasingPeriod.showValue(it.leasePeriod, it.leasedUntil)
        }

        crowdloanContributeToolbar.setTitle(viewModel.title)

        crowdloanContributeLearnMore.setVisible(viewModel.learnCrowdloanModel != null)

        viewModel.learnCrowdloanModel?.let {
            crowdloanContributeLearnMore.title.text = it.text
            crowdloanContributeLearnMore.loadIcon(it.iconLink, imageLoader)
        }

        viewModel.bonusDisplayFlow.observe {
            crowdloanContributeBonus.setVisible(it != null)

            crowdloanContributeBonusReward.text = it
        }

        viewModel.customizationConfiguration.filterNotNull().observe { (customization, customViewState) ->
            customization.injectViews(crowdloanContributeContainer, customViewState, viewLifecycleOwner.lifecycleScope)
        }
    }
}
