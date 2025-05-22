package io.novafoundation.nova.feature_account_migration.presentation.pairing

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_account_migration.databinding.FragmentAccountMigrationPairingBinding
import io.novafoundation.nova.feature_account_migration.di.AccountMigrationFeatureApi
import io.novafoundation.nova.feature_account_migration.di.AccountMigrationFeatureComponent


class AccountMigrationPairingFragment : BaseFragment<AccountMigrationPairingViewModel, FragmentAccountMigrationPairingBinding>() {

    companion object : PayloadCreator<AccountMigrationPairingPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentAccountMigrationPairingBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.accountMigrationPair.setOnClickListener { viewModel.acceptMigration() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountMigrationFeatureComponent>(
            requireContext(),
            AccountMigrationFeatureApi::class.java
        )
            .accountMigrationPairingComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: AccountMigrationPairingViewModel) {
        observeBrowserEvents(viewModel)
    }
}
