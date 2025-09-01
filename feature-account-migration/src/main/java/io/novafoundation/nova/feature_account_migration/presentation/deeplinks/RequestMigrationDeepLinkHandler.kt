package io.novafoundation.nova.feature_account_migration.presentation.deeplinks

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.common.utils.splash.SplashPassedObserver
import io.novafoundation.nova.common.utils.splash.awaitSplashPassed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ACTION_MIGRATE_PATH_REGEX = "/migrate"

class RequestMigrationDeepLinkHandler(
    private val router: AccountMigrationRouter,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val splashPassedObserver: SplashPassedObserver,
    private val repository: AccountRepository
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(ACTION_MIGRATE_PATH_REGEX)
    }

    override suspend fun handleDeepLink(data: Uri): Result<Unit> = runCatching {
        if (repository.isAccountSelected()) {
            automaticInteractionGate.awaitInteractionAllowed()
        } else {
            splashPassedObserver.awaitSplashPassed()
        }

        val scheme = data.getQueryParameter("scheme") ?: error("No scheme was passed")
        router.openAccountMigrationPairing(scheme)
    }
}
