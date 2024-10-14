package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentCustomNodeBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import javax.inject.Inject

class CustomNodeFragment : BaseFragment<CustomNodeViewModel, FragmentCustomNodeBinding>() {

    companion object {

        private const val EXTRA_PAYLOAD = "payload"

        fun getBundle(payload: CustomNodePayload): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentCustomNodeBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.customNodeToolbar.applyStatusBarInsets()
        binder.customNodeToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.customNodeApplyButton.prepareForProgress(this)
        binder.customNodeApplyButton.setOnClickListener { viewModel.saveNodeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .customNodeFactory()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: CustomNodeViewModel) {
        observeValidations(viewModel)

        binder.customNodeTitle.text = viewModel.getTitle()
        binder.customNodeUrlInput.bindTo(viewModel.nodeUrlInput, viewModel)
        binder.customNodeNameInput.bindTo(viewModel.nodeNameInput, viewModel)
        viewModel.buttonState.observe {
            binder.customNodeApplyButton.setState(it)
        }

        viewModel.chainModel.observe {
            binder.customNodeChain.setChain(it)
        }
    }
}
