package io.novafoundation.nova.feature_settings_impl.presentation.assetIcons

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentAppearanceBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent

class AppearanceFragment : BaseFragment<AppearanceViewModel, FragmentAppearanceBinding>() {

    override fun createBinding() = FragmentAppearanceBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.appearanceToolbar.applyStatusBarInsets()
        binder.appearanceToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.appearanceWhiteButton.setOnClickListener { viewModel.selectWhiteIcon() }
        binder.appearanceColoredButton.setOnClickListener { viewModel.selectColoredIcon() }

        binder.appearanceWhiteButton.background = getRippleDrawable(cornerSizeInDp = 10)
        binder.appearanceColoredButton.background = getRippleDrawable(cornerSizeInDp = 10)
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
            binder.appearanceWhiteIcon.isSelected = it.whiteActive
            binder.appearanceWhiteText.isSelected = it.whiteActive

            binder.appearanceColoredIcon.isSelected = it.coloredActive
            binder.appearanceColoredText.isSelected = it.coloredActive
        }
    }
}
