package io.novafoundation.nova.feature_account_impl.presentation.account.details

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.setGroupedListSpacings
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.setupImportTypeChooser
import io.novafoundation.nova.feature_account_impl.databinding.FragmentWalletDetailsBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.ui.setupAddAccountLauncher

import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val ACCOUNT_ID_KEY = "ACCOUNT_ADDRESS_KEY"

class WalletDetailsFragment : BaseFragment<WalletDetailsViewModel, FragmentWalletDetailsBinding>(), ChainAccountsAdapter.Handler {

    override fun createBinding() = FragmentWalletDetailsBinding.inflate(layoutInflater)

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

    override fun initViews() {
        binder.accountDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        binder.accountDetailsChainAccounts.setHasFixedSize(true)
        binder.accountDetailsChainAccounts.adapter = adapter
        binder.accountDetailsChainAccounts.setGroupedListSpacings(
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
                onCopy = viewModel::copyValue,
                onViewExternal = viewModel::viewExternalClicked,
                onChange = viewModel::changeChainAccountClicked,
                onExport = viewModel::exportClicked,
                availableAccountActions = viewModel.availableAccountActions.first()
            )
        }
        setupImportTypeChooser(viewModel)
        setupAddAccountLauncher(viewModel.addAccountLauncherMixin)

        binder.accountDetailsNameField.content.bindTo(viewModel.accountNameFlow, viewLifecycleOwner.lifecycleScope)

        viewModel.chainAccountProjections.observe { adapter.submitList(it) }

        viewModel.typeAlert.observe(binder.accountDetailsTypeAlert::setModelOrHide)
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
