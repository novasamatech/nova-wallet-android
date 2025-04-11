package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentChainAddressSelectorBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class ChainAddressSelectorFragment : BaseBottomSheetFragment<ChainAddressSelectorViewModel, FragmentChainAddressSelectorBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_REQUEST"

        fun getBundle(request: ChainAddressSelectorPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, request)
            }
        }
    }

    override fun createBinding() = FragmentChainAddressSelectorBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.selectLegacyAddressSubtitle.movementMethod = LinkMovementMethod.getInstance()
        binder.selectLegacyAddressSubtitle.text = getDescriptionSpannableText()

        binder.legacyAddressCheckbox.isChecked = viewModel.addressSelectorDisabled()
        binder.legacyAddressCheckbox.setOnCheckedChangeListener { _, isChecked -> viewModel.disableAddressSelector(isChecked) }

        binder.legacyAddressButton.setOnClickListener { viewModel.back() }
        binder.addressNewContainer.setOnClickListener { viewModel.copyNewAddress() }
        binder.addressLegacyContainer.setOnClickListener { viewModel.copyLegacyAddress() }

        binder.addressNewContainer.background = getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeDp = 16).withRippleMask()
        binder.addressLegacyContainer.background = getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeDp = 16).withRippleMask()
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .chainAddressSelectorComponent()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChainAddressSelectorViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.newAddressFlow.observe { binder.addressFormatAddress.text = it }
        viewModel.legacyAddressFlow.observe { binder.addressFormatAddressLegacy.text = it }
    }

    private fun getDescriptionSpannableText(): CharSequence {
        val chevronIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_right_16)!!
        chevronIcon.setTint(requireContext().getColor(R.color.icon_accent))
        chevronIcon.setBounds(0, 0, chevronIcon.intrinsicWidth, chevronIcon.intrinsicHeight)

        val learnMoreButton = getString(R.string.common_learn_more).toSpannable(clickableSpan { viewModel.openLearnMore() })
            .setFullSpan(colorSpan(requireContext().getColor(R.color.button_text_accent)))
            .setEndSpan(drawableSpan(chevronIcon))

        return SpannableFormatter.format(
            getString(R.string.unified_address_subtitle),
            learnMoreButton
        )
    }
}
