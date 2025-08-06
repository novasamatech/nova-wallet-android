package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applySystemBarInsets
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

    override fun applyInsets(rootView: View) {
        binder.root.applySystemBarInsets(imeInsets = true)
    }

    override fun initViews() {
        binder.addTokenEnterInfoToolbar.setHomeButtonListener { viewModel.backClicked() }

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
