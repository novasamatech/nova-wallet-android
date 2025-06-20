package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates

import androidx.core.view.isVisible
import coil.ImageLoader
import com.google.android.material.tabs.TabLayout
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.setTabSelectedListener
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.BottomSheetDelegatedAccountUpdatesBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject

class DelegatedAccountUpdatesBottomSheet : BaseBottomSheetFragment<DelegatedAccountUpdatesViewModel, BottomSheetDelegatedAccountUpdatesBinding>() {

    override fun createBinding() = BottomSheetDelegatedAccountUpdatesBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DelegatedAccountsAdapter(imageLoader) }

    override fun initViews() {
        binder.delegatedAccountUpdatesLink.setOnClickListener { viewModel.clickAbout() }
        binder.delegatedAccountUpdatesDone.setOnClickListener { viewModel.clickDone() }
        binder.delegatedAccountUpdatesList.adapter = adapter
        binder.delegatedAccountUpdatesList.itemAnimator = null

        binder.delegatedAccountUpdatesMode.createTab(R.string.account_proxied)
        binder.delegatedAccountUpdatesMode.createTab(R.string.account_multisig)
        binder.delegatedAccountUpdatesMode.setTabSelectedListener {
            when (it.position) {
                0 -> viewModel.showProxieds()
                1 -> viewModel.showMultisig()
            }
        }
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
        viewModel.filtersAvailableFlow.observe { binder.delegatedAccountUpdatesMode.isVisible = it }
        viewModel.accounts.observe { adapter.submitList(it) }
    }

    private fun TabLayout.createTab(textResId: Int) {
        val tab = newTab()
        tab.setText(textResId)

        addTab(tab)
    }
}
