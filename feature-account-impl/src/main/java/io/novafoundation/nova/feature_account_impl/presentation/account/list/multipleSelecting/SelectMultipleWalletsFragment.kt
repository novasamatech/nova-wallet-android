package io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_select_multiple_wallets.selectMultipleWalletsConfirm
import kotlinx.android.synthetic.main.fragment_select_multiple_wallets.selectMultipleWalletsList
import kotlinx.android.synthetic.main.fragment_select_multiple_wallets.selectMultipleWalletsToolbar

class SelectMultipleWalletsFragment : BaseFragment<SelectMultipleWalletsViewModel>(), AccountHolder.AccountItemHandler {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: SelectMultipleWalletsRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val titleAdapter by lazy(LazyThreadSafetyMode.NONE) { TextAdapter() }

    private val walletsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(
            this,
            imageLoader,
            initialMode = viewModel.mode,
            chainBorderColor = R.color.bottom_sheet_background
        )
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(titleAdapter, walletsAdapter) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_multiple_wallets, container, false)
    }

    override fun initViews() {
        selectMultipleWalletsToolbar.applyStatusBarInsets()
        selectMultipleWalletsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        selectMultipleWalletsList.adapter = adapter
        selectMultipleWalletsConfirm.setOnClickListener { viewModel.confirm() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .selectMultipleWalletsComponentFactory()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectMultipleWalletsViewModel) {
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.closeConfirmationAction)

        viewModel.titleFlow.observe(titleAdapter::setText)
        viewModel.walletsListingMixin.metaAccountsFlow.observe(walletsAdapter::submitList)
        viewModel.confirmButtonState.observe(selectMultipleWalletsConfirm::setState)
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }
}
