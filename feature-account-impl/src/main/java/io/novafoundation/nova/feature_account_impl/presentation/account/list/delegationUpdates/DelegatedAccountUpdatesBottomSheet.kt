package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates

import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.BottomSheetDelegatedAccountUpdatesBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject

class DelegatedAccountUpdatesBottomSheet() : BaseBottomSheetFragment<DelegatedAccountUpdatesViewModel, BottomSheetDelegatedAccountUpdatesBinding>() {

    override val binder by viewBinding(BottomSheetDelegatedAccountUpdatesBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DelegatedAccountsAdapter(imageLoader) }

    override fun initViews() {
        binder.delegatedAccountUpdatesLink.setOnClickListener { viewModel.clickAbout() }
        binder.delegatedAccountUpdatesDone.setOnClickListener { viewModel.clickDone() }
        binder.delegatedAccountUpdatesList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .delegatedAccountUpdatesFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DelegatedAccountUpdatesViewModel) {
        observeBrowserEvents(viewModel)
        viewModel.accounts.observe { adapter.submitList(it) }
    }
}
