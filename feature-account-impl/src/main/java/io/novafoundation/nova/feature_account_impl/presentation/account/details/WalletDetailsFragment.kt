package io.novafoundation.nova.feature_account_impl.presentation.account.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.setGroupedListSpacings
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.copyAddressClicked
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.setupImportTypeChooser
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.ui.setupAddAccountLauncher
import kotlinx.android.synthetic.main.fragment_wallet_details.accountDetailsChainAccounts
import kotlinx.android.synthetic.main.fragment_wallet_details.accountDetailsNameField
import kotlinx.android.synthetic.main.fragment_wallet_details.accountDetailsToolbar
import kotlinx.android.synthetic.main.fragment_wallet_details.accountDetailsTypeAlert
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val ACCOUNT_ID_KEY = "ACCOUNT_ADDRESS_KEY"

class WalletDetailsFragment : BaseFragment<WalletDetailsViewModel>(), ChainAccountsAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ChainAccountsAdapter(this, imageLoader)
    }

    companion object {

        fun getBundle(metaAccountId: Long): Bundle {
            return Bundle().apply {
                putLong(ACCOUNT_ID_KEY, metaAccountId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wallet_details, container, false)

    override fun initViews() {
        accountDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        accountDetailsChainAccounts.setHasFixedSize(true)
        accountDetailsChainAccounts.adapter = adapter
        accountDetailsChainAccounts.setGroupedListSpacings(
            groupTopSpacing = 24,
            groupBottomSpacing = 12,
            firstItemTopSpacing = 16,
            itemBottomSpacing = 4,
        )
    }

    override fun inject() {
        val metaId = argument<Long>(ACCOUNT_ID_KEY)

        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .accountDetailsComponentFactory()
            .create(this, metaId)
            .inject(this)
    }

    override fun subscribe(viewModel: WalletDetailsViewModel) {
        setupExternalActions(viewModel) { context, payload ->
            ChainAccountActionsSheet(
                context,
                payload,
                onCopy = viewModel::copyAddressClicked,
                onViewExternal = viewModel::viewExternalClicked,
                onChange = viewModel::changeChainAccountClicked,
                onExport = viewModel::exportClicked,
                availableAccountActions = viewModel.availableAccountActions.first()
            )
        }
        setupImportTypeChooser(viewModel)
        setupAddAccountLauncher(viewModel)

        accountDetailsNameField.content.bindTo(viewModel.accountNameFlow, viewLifecycleOwner.lifecycleScope)

        viewModel.chainAccountProjections.observe { adapter.submitList(it) }

        viewModel.typeAlert.observe {
            if (it != null) {
                accountDetailsTypeAlert.makeVisible()
                accountDetailsTypeAlert.setMessage(it.message)
                accountDetailsTypeAlert.setSubMessage(it.subMessage)
                accountDetailsTypeAlert.setStyle(it.style)
            } else {
                accountDetailsTypeAlert.makeGone()
            }
        }
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
