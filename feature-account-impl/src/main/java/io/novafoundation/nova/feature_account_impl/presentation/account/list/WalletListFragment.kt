package io.novafoundation.nova.feature_account_impl.presentation.account.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_impl.R
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_wallet_list.walletListBarAction
import kotlinx.android.synthetic.main.fragment_wallet_list.walletListContent
import kotlinx.android.synthetic.main.fragment_wallet_list.walletListTitle

abstract class WalletListFragment<T : WalletListViewModel> :
    BaseBottomSheetFragment<T>(),
    AccountHolder.AccountItemHandler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(this, imageLoader, initialMode = viewModel.mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = layoutInflater.inflate(R.layout.fragment_wallet_list, container, false)

    override fun initViews() {
        walletListContent.adapter = adapter
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
        walletListTitle.setText(titleRes)
    }

    fun setActionIcon(@DrawableRes drawableRes: Int) {
        walletListBarAction.setImageResource(drawableRes)
    }

    fun setActionClickListener(listener: View.OnClickListener?) {
        walletListBarAction.setOnClickListener(listener)
    }
}
