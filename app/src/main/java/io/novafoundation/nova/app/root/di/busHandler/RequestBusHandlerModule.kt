package io.novafoundation.nova.app.root.di.busHandler

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.app.root.presentation.requestBusHandler.CloudBackupSyncRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.CompoundRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.MultisigExtrinsicValidationRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.ProxyExtrinsicValidationRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.PushSettingsSyncRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.RequestBusHandler
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory

@Module
class RequestBusHandlerModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun providePushSettingsSyncRequestBusHandler(
        scope: RootScope,
        metaAccountChangesEventBus: MetaAccountChangesEventBus,
        pushNotificationsInteractor: PushNotificationsInteractor
    ): RequestBusHandler {
        return PushSettingsSyncRequestBusHandler(
            scope,
            metaAccountChangesEventBus,
            pushNotificationsInteractor
        )
    }

    @Provides
    @FeatureScope
    @IntoSet
    fun provideProxyExtrinsicValidationRequestBusHandler(
        scope: RootScope,
        proxyProxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
        proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory
    ): RequestBusHandler {
        return ProxyExtrinsicValidationRequestBusHandler(
            scope,
            proxyProxyExtrinsicValidationRequestBus,
            proxyHaveEnoughFeeValidationFactory
        )
    }

    @Provides
    @FeatureScope
    @IntoSet
    fun provideMultisigExtrinsicValidationRequestBusHandler(
        scope: RootScope,
        multisigExtrinsicValidationRequestBus: MultisigExtrinsicValidationRequestBus,
        multisigExtrinsicValidationFactory: MultisigExtrinsicValidationFactory
    ): RequestBusHandler {
        return MultisigExtrinsicValidationRequestBusHandler(
            scope,
            multisigExtrinsicValidationRequestBus,
            multisigExtrinsicValidationFactory
        )
    }

    @Provides
    @FeatureScope
    @IntoSet
    fun provideCloudBackupSyncRequestBusHandler(
        scope: RootScope,
        rootRouter: RootRouter,
        resourceManager: ResourceManager,
        metaAccountChangesEventBus: MetaAccountChangesEventBus,
        applyLocalSnapshotToCloudBackupUseCase: ApplyLocalSnapshotToCloudBackupUseCase,
        accountRepository: AccountRepository,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        automaticInteractionGate: AutomaticInteractionGate
    ): RequestBusHandler {
        return CloudBackupSyncRequestBusHandler(
            rootRouter,
            scope,
            resourceManager,
            metaAccountChangesEventBus,
            applyLocalSnapshotToCloudBackupUseCase,
            accountRepository,
            actionBottomSheetLauncher,
            automaticInteractionGate
        )
    }

    @Provides
    @FeatureScope
    fun provideCompoundRequestBusHandler(
        handlers: Set<@JvmSuppressWildcards RequestBusHandler>
    ): CompoundRequestBusHandler {
        return CompoundRequestBusHandler(handlers)
    }
}
