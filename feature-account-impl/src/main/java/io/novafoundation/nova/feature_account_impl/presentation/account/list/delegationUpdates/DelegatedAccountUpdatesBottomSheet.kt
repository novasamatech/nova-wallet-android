package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.bottom_sheet_delegated_account_updates.delegatedAccountUpdatesDone
import kotlinx.android.synthetic.main.bottom_sheet_delegated_account_updates.delegatedAccountUpdatesLink
import kotlinx.android.synthetic.main.bottom_sheet_delegated_account_updates.delegatedAccountUpdatesList

class DelegatedAccountUpdatesBottomSheet() : BaseBottomSheetFragment<DelegatedAccountUpdatesViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DelegatedAccountsAdapter(imageLoader) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_delegated_account_updates, container, false)
    }

    override fun initViews() {
        delegatedAccountUpdatesLink.setOnClickListener { viewModel.clickAbout() }
        delegatedAccountUpdatesDone.setOnClickListener { viewModel.clickDone() }
        delegatedAccountUpdatesList.adapter = adapter
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
