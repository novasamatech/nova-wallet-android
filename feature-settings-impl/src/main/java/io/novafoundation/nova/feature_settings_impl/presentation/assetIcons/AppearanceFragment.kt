package io.novafoundation.nova.feature_settings_impl.presentation.assetIcons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_appearance.appearanceColoredButton
import kotlinx.android.synthetic.main.fragment_appearance.appearanceColoredIcon
import kotlinx.android.synthetic.main.fragment_appearance.appearanceColoredText
import kotlinx.android.synthetic.main.fragment_appearance.appearanceToolbar
import kotlinx.android.synthetic.main.fragment_appearance.appearanceWhiteButton
import kotlinx.android.synthetic.main.fragment_appearance.appearanceWhiteIcon
import kotlinx.android.synthetic.main.fragment_appearance.appearanceWhiteText

class AppearanceFragment : BaseFragment<AppearanceViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_appearance, container, false)
    }

    override fun initViews() {
        appearanceToolbar.applyStatusBarInsets()
        appearanceToolbar.setHomeButtonListener { viewModel.backClicked() }

        appearanceWhiteButton.setOnClickListener { viewModel.selectWhiteIcon() }
        appearanceColoredButton.setOnClickListener { viewModel.selectColoredIcon() }

        appearanceWhiteButton.background = getRippleDrawable(cornerSizeInDp = 10)
        appearanceColoredButton.background = getRippleDrawable(cornerSizeInDp = 10)
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .appearanceFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AppearanceViewModel) {
        viewModel.assetIconsStateFlow.observe {
            appearanceWhiteIcon.isSelected = it.whiteActive
            appearanceWhiteText.isSelected = it.whiteActive

            appearanceColoredIcon.isSelected = it.coloredActive
            appearanceColoredText.isSelected = it.coloredActive
        }
    }
}
