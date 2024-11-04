package io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview

import androidx.annotation.CallSuper
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.databinding.FragmentChainAccountPreviewBinding
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions

import javax.inject.Inject

abstract class BaseChainAccountsPreviewFragment<V : BaseChainAccountsPreviewViewModel> : BaseFragment<V, FragmentChainAccountPreviewBinding>(), ChainAccountsAdapter.Handler {

    override val binder by viewBinding(FragmentChainAccountPreviewBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ChainAccountsAdapter(this, imageLoader)
    }

    @CallSuper
    override fun initViews() {
        binder.previewChainAccountToolbar.applyStatusBarInsets()
        binder.previewChainAccountToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        binder.previewChainAccountAccounts.setHasFixedSize(true)
        binder.previewChainAccountAccounts.adapter = adapter

        binder.previewChainAccountContinue.setOnClickListener { viewModel.continueClicked() }
        binder.previewChainAccountContinue.prepareForProgress(viewLifecycleOwner)
    }

    @CallSuper
    override fun subscribe(viewModel: V) {
        setupExternalActions(viewModel)

        viewModel.chainAccountProjections.observe(adapter::submitList)

        binder.previewChainAccountDescription.setTextOrHide(viewModel.subtitle)

        viewModel.buttonState.observe(binder.previewChainAccountContinue::setState)
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
