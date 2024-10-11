package io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions

import javax.inject.Inject

abstract class BaseChainAccountsPreviewFragment<V : BaseChainAccountsPreviewViewModel> : BaseFragment<V>(), ChainAccountsAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ChainAccountsAdapter(this, imageLoader)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_chain_account_preview, container, false)

    @CallSuper
    override fun initViews() {
        previewChainAccountToolbar.applyStatusBarInsets()
        previewChainAccountToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        previewChainAccountAccounts.setHasFixedSize(true)
        previewChainAccountAccounts.adapter = adapter

        previewChainAccountContinue.setOnClickListener { viewModel.continueClicked() }
        previewChainAccountContinue.prepareForProgress(viewLifecycleOwner)
    }

    @CallSuper
    override fun subscribe(viewModel: V) {
        setupExternalActions(viewModel)

        viewModel.chainAccountProjections.observe(adapter::submitList)

        previewChainAccountDescription.setTextOrHide(viewModel.subtitle)

        viewModel.buttonState.observe(previewChainAccountContinue::setState)
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
