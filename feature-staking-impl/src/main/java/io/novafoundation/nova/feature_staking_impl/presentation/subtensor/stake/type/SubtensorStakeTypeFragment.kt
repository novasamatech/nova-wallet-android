package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type

import androidx.core.text.HtmlCompat
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorStakeTypeBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorStakeTypeFragment : BaseFragment<SubtensorStakeTypeViewModel, FragmentSubtensorStakeTypeBinding>() {

    override fun createBinding() = FragmentSubtensorStakeTypeBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorStakeTypeToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorStakeTypeRootCard.setOnClickListener { viewModel.selectRoot() }
        binder.subtensorStakeTypeSubnetCard.setOnClickListener { viewModel.selectSubnet() }
        binder.subtensorStakeTypeContinue.setOnClickListener { viewModel.continueClicked() }

        // The wiki footer has an inline accent-coloured "Nova Wiki >" link.
        // The string is HTML-formatted so the colour stays in res/.
        binder.subtensorStakeTypeWikiFooter.text = HtmlCompat.fromHtml(
            getString(R.string.subtensor_stake_type_wiki_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorStakeTypeFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorStakeTypeViewModel) {
        viewModel.selection.observe { selection ->
            val root = selection == SubtensorStakeTypeViewModel.Selection.ROOT
            binder.subtensorStakeTypeRootRadio.isChecked = root
            binder.subtensorStakeTypeSubnetRadio.isChecked = !root
            binder.subtensorStakeTypeRootCard.isSelected = root
            binder.subtensorStakeTypeSubnetCard.isSelected = !root
        }
    }
}
