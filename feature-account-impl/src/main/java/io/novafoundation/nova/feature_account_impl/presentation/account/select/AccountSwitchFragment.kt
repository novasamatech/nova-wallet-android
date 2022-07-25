package io.novafoundation.nova.feature_account_impl.presentation.account.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.android.synthetic.main.fragment_switch_account.switchAccountContent
import kotlinx.android.synthetic.main.fragment_switch_account.switchAccountRightAction

class AccountSwitchFragment : BaseBottomSheetFragment<AccountSwitchViewModel>(), AccountsAdapter.AccountItemHandler {

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(this, initialMode = viewModel.mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = layoutInflater.inflate(R.layout.fragment_switch_account, container, false)

    override fun initViews() {
        switchAccountRightAction.setOnClickListener {
            viewModel.settingsClicked()
        }

        switchAccountContent.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .switchAccountComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AccountSwitchViewModel) {
        viewModel.walletsListingMixin.metaAccountsFlow.observe(adapter::submitList)
    }

    override fun itemClicked(accountModel: MetaAccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun deleteClicked(accountModel: MetaAccountUi) {
        // no delete possible
    }
}
