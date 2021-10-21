package io.novafoundation.nova.feature_staking_impl.presentation.validators

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun List<Validator>.findSelectedValidator(accountIdHex: String) = withContext(Dispatchers.Default) {
    firstOrNull { it.accountIdHex == accountIdHex }
}
