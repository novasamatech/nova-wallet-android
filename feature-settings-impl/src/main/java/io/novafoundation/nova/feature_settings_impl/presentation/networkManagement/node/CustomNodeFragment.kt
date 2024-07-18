package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeApplyButton
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeChain
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeNameInput
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeTitle
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeToolbar
import kotlinx.android.synthetic.main.fragment_custom_node.customNodeUrlInput

class CustomNodeFragment : BaseFragment<CustomNodeViewModel>() {

    companion object {

        private const val EXTRA_PAYLOAD = "payload"

        fun getBundle(payload: CustomNodePayload): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_node, container, false)
    }

    override fun initViews() {
        customNodeToolbar.applyStatusBarInsets()
        customNodeToolbar.setHomeButtonListener { viewModel.backClicked() }

        customNodeApplyButton.prepareForProgress(this)
        customNodeApplyButton.setOnClickListener { viewModel.saveNodeClicked() }
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

        customNodeTitle.text = viewModel.getTitle()
        customNodeUrlInput.bindTo(viewModel.nodeUrlInput, viewModel)
        customNodeNameInput.bindTo(viewModel.nodeNameInput, viewModel)
        viewModel.buttonState.observe {
            customNodeApplyButton.setState(it)
        }

        viewModel.chainModel.observe {
            customNodeChain.setChain(it)
        }
    }
}
