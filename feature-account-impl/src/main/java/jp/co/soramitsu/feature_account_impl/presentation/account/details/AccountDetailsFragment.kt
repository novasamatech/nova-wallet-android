package jp.co.soramitsu.feature_account_impl.presentation.account.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.di.FeatureUtils
import jp.co.soramitsu.common.utils.bindTo
import jp.co.soramitsu.common.utils.nameInputFilters
import jp.co.soramitsu.feature_account_api.di.AccountFeatureApi
import jp.co.soramitsu.feature_account_api.presenatation.actions.copyAddressClicked
import jp.co.soramitsu.feature_account_api.presenatation.actions.setupExternalActions
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.di.AccountFeatureComponent
import jp.co.soramitsu.feature_account_impl.presentation.common.accountSource.SourceTypeChooserBottomSheetDialog
import kotlinx.android.synthetic.main.fragment_account_details.accountDetailsChainAccounts
import kotlinx.android.synthetic.main.fragment_account_details.accountDetailsNameField
import kotlinx.android.synthetic.main.fragment_account_details.accountDetailsToolbar
import javax.inject.Inject

private const val ACCOUNT_ID_KEY = "ACCOUNT_ADDRESS_KEY"

class AccountDetailsFragment : BaseFragment<AccountDetailsViewModel>(), ChainAccountsAdapter.Handler {

    @Inject lateinit var imageLoader: ImageLoader

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
    ) = layoutInflater.inflate(R.layout.fragment_account_details, container, false)

    override fun initViews() {
        accountDetailsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        accountDetailsNameField.content.filters = nameInputFilters()
        accountDetailsChainAccounts.setHasFixedSize(true)
        accountDetailsChainAccounts.adapter = adapter
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

    override fun subscribe(viewModel: AccountDetailsViewModel) {
        setupExternalActions(viewModel) { context, payload ->
            ChainAccountActionsSheet(
                context,
                payload,
                onCopy = viewModel::copyAddressClicked,
                onViewExternal = viewModel::viewExternalClicked,
                onChange = viewModel::changeChainAccountClicked,
                onExport = viewModel::exportClicked
            )
        }

        viewModel.showExportSourceChooser.observeEvent { payload ->
            SourceTypeChooserBottomSheetDialog(
                context = requireContext(),
                payload = payload.dynamicListPayload,
                onClicked = { viewModel.exportTypeChosen(it, payload.chain) }
            ).show()
        }

        accountDetailsNameField.content.bindTo(viewModel.accountNameFlow, viewLifecycleOwner.lifecycleScope)

        viewModel.chainAccountProjections.observe { adapter.submitList(it) }
    }

    override fun chainAccountClicked(item: AccountInChainUi) {
        viewModel.chainAccountClicked(item)
    }
}
