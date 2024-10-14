package io.novafoundation.nova.feature_account_impl.presentation.account.list

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentWalletListBinding
import javax.inject.Inject

abstract class WalletListFragment<T : WalletListViewModel> :
    BaseBottomSheetFragment<T, FragmentWalletListBinding>(),
    AccountHolder.AccountItemHandler {

    override val binder by viewBinding(FragmentWalletListBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(this, imageLoader, initialMode = viewModel.mode, chainBorderColor = R.color.bottom_sheet_background)
    }

    override fun initViews() {
        binder.walletListContent.adapter = adapter
    }

    override fun subscribe(viewModel: T) {
        viewModel.walletsListingMixin.metaAccountsFlow.observe(adapter::submitList)
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun deleteClicked(accountModel: AccountUi) {
        // no delete possible
    }

    fun setTitleRes(@StringRes titleRes: Int) {
        binder.walletListTitle.setText(titleRes)
    }

    fun setActionIcon(@DrawableRes drawableRes: Int) {
        binder.walletListBarAction.setImageResource(drawableRes)
    }

    fun setActionClickListener(listener: View.OnClickListener?) {
        binder.walletListBarAction.setOnClickListener(listener)
    }
}
