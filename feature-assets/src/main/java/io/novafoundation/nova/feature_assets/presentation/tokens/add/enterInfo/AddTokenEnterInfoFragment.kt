package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_assets.databinding.FragmentAddTokenEnterInfoBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import javax.inject.Inject

class AddTokenEnterInfoFragment : BaseFragment<AddTokenEnterInfoViewModel, FragmentAddTokenEnterInfoBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "AddTokenEnterInfoFragment.KEY_PAYLOAD"

        fun getBundle(payload: AddTokenEnterInfoPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override fun createBinding() = FragmentAddTokenEnterInfoBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.addTokenEnterInfoToolbar.applyStatusBarInsets()
        binder.addTokenEnterInfoToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.addTokenEnterInfoContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        binder.addTokenEnterInfoScrollArea.scrollOnFocusTo(
            binder.addTokenEnterInfoAddressInput,
            binder.addTokenEnterInfoSymbolInput,
            binder.addTokenEnterInfoDecimalsInput,
            binder.addTokenEnterInfoPriceInput
        )

        binder.addTokenEnterInfoPriceConfirm.setOnClickListener {
            viewModel.confirmClicked()
        }

        binder.addTokenEnterInfoPriceConfirm.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .addTokenEnterInfoComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: AddTokenEnterInfoViewModel) {
        observeValidations(viewModel)
        val scope = viewLifecycleOwner.lifecycleScope

        binder.addTokenEnterInfoAddressInput.bindTo(viewModel.contractAddressInput, scope)
        binder.addTokenEnterInfoSymbolInput.bindTo(viewModel.symbolInput, scope)
        binder.addTokenEnterInfoDecimalsInput.bindTo(viewModel.decimalsInput, scope)
        binder.addTokenEnterInfoPriceInput.bindTo(viewModel.priceLinkInput, scope)

        viewModel.continueButtonState.observe(binder.addTokenEnterInfoPriceConfirm::setState)
    }
}
