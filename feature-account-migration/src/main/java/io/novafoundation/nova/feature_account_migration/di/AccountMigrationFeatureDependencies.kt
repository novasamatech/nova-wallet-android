package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.common.presentation.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.usecase.GetSelectedAccountMnemonicUseCase
import io.novafoundation.nova.feature_account_api.domain.usecase.GetSelectedMetaAccountUseCase

interface AccountMigrationFeatureDependencies {

    val resourceManager: ResourceManager

    val getSelectedAccountMnemonicUseCase: GetSelectedAccountMnemonicUseCase

    val getSelectedMetaAccountUseCase: GetSelectedMetaAccountUseCase
}
