package io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.model.AuthorizedDAppModel

class AuthorizedDAppsFragment : BaseFragment<AuthorizedDAppsViewModel>(), AuthorizedDAppAdapter.Handler {

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        AuthorizedDAppAdapter(this)
    }

    private val placeholderViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(authorizedPlaceholderSpacerTop, authorizedPlaceholder, authorizedPlaceholderSpacerBottom)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_authorized_dapps, container, false)
    }

    override fun initViews() {
        authorizedDAppsToolbar.applyStatusBarInsets()
        authorizedDAppsToolbar.setHomeButtonListener { viewModel.backClicked() }

        authorizedDAppsList.setHasFixedSize(true)
        authorizedDAppsList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .authorizedDAppsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AuthorizedDAppsViewModel) {
        viewModel.authorizedDApps.observe {
            val showPlaceholder = it.isEmpty()

            authorizedDAppsList.setVisible(!showPlaceholder)
            placeholderViews.forEach { view -> view.setVisible(showPlaceholder) }

            adapter.submitList(it)
        }

        viewModel.walletUi.observe {
            authorizedDAppsWallet.showWallet(it)
        }

        viewModel.revokeAuthorizationConfirmation.awaitableActionLiveData.observeEvent {
            warningDialog(
                context = requireContext(),
                onPositiveClick = { it.onSuccess(Unit) },
                onNegativeClick = it.onCancel,
                positiveTextRes = R.string.common_remove
            ) {
                setTitle(R.string.dapp_authorized_remove_title)
                setMessage(getString(R.string.dapp_authorized_remove_description, it.payload))
            }
        }
    }

    override fun onRevokeClicked(item: AuthorizedDAppModel) {
        viewModel.revokeClicked(item)
    }
}
