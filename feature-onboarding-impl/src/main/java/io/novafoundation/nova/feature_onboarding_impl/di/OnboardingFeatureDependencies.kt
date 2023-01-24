package io.novafoundation.nova.feature_onboarding_impl.di

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

interface OnboardingFeatureDependencies {

    fun updateNotificationsInteractor(): UpdateNotificationsInteractor

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider

    fun importTypeChooserMixin(): ImportTypeChooserMixin.Presentation

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
}
