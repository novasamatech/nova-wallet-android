package io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentSelectSingleWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject

class SelectSingleWalletFragment : BaseFragment<SelectSingleWalletViewModel, FragmentSelectSingleWalletBinding>(), AccountHolder.AccountItemHandler {

    companion object : PayloadCreator<SelectSingleWalletRequester.Request> by FragmentPayloadCreator()

    override fun createBinding() = FragmentSelectSingleWalletBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(
            this,
            imageLoader,
            initialMode = AccountHolder.Mode.SELECT,
            chainBorderColor = R.color.secondary_screen_background
        )
    }

    override fun initViews() {
        binder.selectSingleWalletToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.selectSingleWalletList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .selectSingleWalletComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: SelectSingleWalletViewModel) {
        viewModel.walletsListingMixin.metaAccountsFlow.observe(adapter::submitList)
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }
}
