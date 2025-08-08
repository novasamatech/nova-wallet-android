package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import android.database.sqlite.SQLiteConstraintException
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException

internal inline fun <R> transformingAccountInsertionErrors(action: () -> R) = try {
    action()
} catch (_: SQLiteConstraintException) {
    throw AccountAlreadyExistsException()
}
