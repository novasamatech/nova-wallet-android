package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentMoonbeamTermsBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class MoonbeamCrowdloanTermsFragment : BaseFragment<MoonbeamCrowdloanTermsViewModel, FragmentMoonbeamTermsBinding>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(payload: ContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentMoonbeamTermsBinding::bind)

    override fun initViews() {
        binder.moonbeamTermsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        binder.moonbeamTermsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.moonbeamTermsConfirm.prepareForProgress(viewLifecycleOwner)
        binder.moonbeamTermsConfirm.setOnClickListener { viewModel.submitClicked() }

        binder.moonbeamTermsLink.setOnClickListener { viewModel.termsLinkClicked() }
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
        setupFeeLoading(viewModel, binder.moonbeamTermsFee)
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)

        binder.moonbeamTermsLink.title.text = viewModel.termsLinkContent.title
        binder.moonbeamTermsLink.icon.load(viewModel.termsLinkContent.iconUrl, imageLoader)

        binder.moonbeamTermsCheckbox.bindTo(viewModel.termsCheckedFlow, viewLifecycleOwner.lifecycleScope)

        viewModel.submitButtonState.observe {
            when (it) {
                is SubmitActionState.Loading -> binder.moonbeamTermsConfirm.setState(ButtonState.PROGRESS)
                is SubmitActionState.Unavailable -> {
                    binder.moonbeamTermsConfirm.setState(ButtonState.DISABLED)
                    binder.moonbeamTermsConfirm.text = it.reason
                }

                is SubmitActionState.Available -> {
                    binder.moonbeamTermsConfirm.setState(ButtonState.NORMAL)
                    binder.moonbeamTermsConfirm.setText(R.string.common_apply)
                }
            }
        }
    }
}
