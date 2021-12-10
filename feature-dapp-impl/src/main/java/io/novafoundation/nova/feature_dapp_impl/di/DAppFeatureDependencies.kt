package io.novafoundation.nova.feature_dapp_impl.di

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin

interface DAppFeatureDependencies {

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider

    fun importTypeChooserMixin(): ImportTypeChooserMixin.Presentation
}
