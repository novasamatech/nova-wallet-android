package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.HintModel
import io.novafoundation.nova.common.mixin.hints.setHints
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_ahm_api.di.ChainMigrationFeatureApi
import io.novafoundation.nova.feature_ahm_impl.R
import io.novafoundation.nova.feature_ahm_impl.di.ChainMigrationFeatureComponent
import io.novafoundation.nova.feature_ahm_impl.databinding.FragmentChainMigrationDetailsBinding
import io.novafoundation.nova.feature_banners_api.presentation.bind

class ChainMigrationDetailsFragment : BaseFragment<ChainMigrationDetailsViewModel, FragmentChainMigrationDetailsBinding>() {

    companion object : PayloadCreator<ChainMigrationDetailsPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentChainMigrationDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.chainMigrationDetailsHints.setHints(
            HintModel(R.drawable.ic_recent_history, requireContext().getString(R.string.chaim_migration_details_hint_history)),
            HintModel(R.drawable.ic_nova, requireContext().getString(R.string.chaim_migration_details_hint_auto_migration))
        )
        binder.chainMigrationDetailsButton.setOnClickListener { viewModel.okButtonClicked() }

        binder.chainMigrationDetailsToolbar.setRightActionClickListener { viewModel.learnMoreClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<ChainMigrationFeatureComponent>(
            requireContext(),
            ChainMigrationFeatureApi::class.java
        ).chainMigrationDetailsComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: ChainMigrationDetailsViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.bannersFlow.observe {
            it.bind(binder.chainMigrationDetailsBanner)
        }

        viewModel.configUIFlow.observe {
            binder.chainMigrationDetailsTitle.text = it.title
            binder.chainMigrationDetailsMinBalance.text = it.minimalBalance
            binder.chainMigrationDetailsLowerFee.text = it.lowerFee
            binder.chainMigrationDetailsTokens.text = it.tokens
            binder.chainMigrationDetailsAccess.text = it.unifiedAccess
            binder.chainMigrationDetailsAnyTokenFee.text = it.anyTokenFee
        }
    }
}
