package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class MoonbeamCrowdloanTermsFragment : BaseFragment<MoonbeamCrowdloanTermsViewModel>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(payload: ContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_moonbeam_terms, container, false)
    }

    override fun initViews() {
        moonbeamTermsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        moonbeamTermsToolbar.setHomeButtonListener { viewModel.backClicked() }

        moonbeamTermsConfirm.prepareForProgress(viewLifecycleOwner)
        moonbeamTermsConfirm.setOnClickListener { viewModel.submitClicked() }

        moonbeamTermsLink.setOnClickListener { viewModel.termsLinkClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .moonbeamTermsFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: MoonbeamCrowdloanTermsViewModel) {
        setupFeeLoading(viewModel, moonbeamTermsFee)
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)

        moonbeamTermsLink.title.text = viewModel.termsLinkContent.title
        moonbeamTermsLink.icon.load(viewModel.termsLinkContent.iconUrl, imageLoader)

        moonbeamTermsCheckbox.bindTo(viewModel.termsCheckedFlow, viewLifecycleOwner.lifecycleScope)

        viewModel.submitButtonState.observe {
            when (it) {
                is SubmitActionState.Loading -> moonbeamTermsConfirm.setState(ButtonState.PROGRESS)
                is SubmitActionState.Unavailable -> {
                    moonbeamTermsConfirm.setState(ButtonState.DISABLED)
                    moonbeamTermsConfirm.text = it.reason
                }
                is SubmitActionState.Available -> {
                    moonbeamTermsConfirm.setState(ButtonState.NORMAL)
                    moonbeamTermsConfirm.setText(R.string.common_apply)
                }
            }
        }
    }
}
