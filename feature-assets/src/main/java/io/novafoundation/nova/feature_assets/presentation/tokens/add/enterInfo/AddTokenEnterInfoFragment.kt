package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoAddressInput
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoAddressLabel
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoContainer
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoDecimalsInput
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoPriceConfirm
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoPriceInput
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoScrollArea
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoSymbolInput
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoTitle
import kotlinx.android.synthetic.main.fragment_add_token_enter_info.addTokenEnterInfoToolbar

class AddTokenEnterInfoFragment : BaseFragment<AddTokenEnterInfoViewModel>() {

    companion object {

        private const val KEY_PAYLOAD = "AddTokenEnterInfoFragment.KEY_PAYLOAD"

        fun getBundle(payload: AddTokenEnterInfoPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_add_token_enter_info, container, false)
    }

    override fun initViews() {
        addTokenEnterInfoToolbar.applyStatusBarInsets()
        addTokenEnterInfoToolbar.setHomeButtonListener { viewModel.backClicked() }

        addTokenEnterInfoContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        addTokenEnterInfoScrollArea.scrollOnFocusTo(
            addTokenEnterInfoAddressInput,
            addTokenEnterInfoSymbolInput,
            addTokenEnterInfoDecimalsInput,
            addTokenEnterInfoPriceInput
        )

        addTokenEnterInfoPriceConfirm.setOnClickListener {
            viewModel.confirmClicked()
        }

        addTokenEnterInfoPriceConfirm.prepareForProgress(viewLifecycleOwner)
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

        viewModel.titleResId.observe(addTokenEnterInfoTitle::setText)
        viewModel.tokenIdTitleResId.observe(addTokenEnterInfoAddressLabel::setText)
        viewModel.tokenIdHintResId.observe(addTokenEnterInfoAddressInput::setHint)
        viewModel.tokenIdInputType.observe(addTokenEnterInfoAddressInput::setInputType)
        addTokenEnterInfoAddressInput.bindTo(viewModel.contractAddressInput, scope)
        addTokenEnterInfoSymbolInput.bindTo(viewModel.symbolInput, scope)
        addTokenEnterInfoDecimalsInput.bindTo(viewModel.decimalsInput, scope)
        addTokenEnterInfoPriceInput.bindTo(viewModel.priceLinkInput, scope)

        viewModel.continueButtonState.observe(addTokenEnterInfoPriceConfirm::setState)
    }
}
