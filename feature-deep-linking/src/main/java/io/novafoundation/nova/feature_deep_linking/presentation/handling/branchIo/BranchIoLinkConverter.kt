package io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo

import android.net.Uri
import io.branch.indexing.BranchUniversalObject
import io.novafoundation.nova.common.utils.appendPathOrSkip
import io.novafoundation.nova.common.utils.appendQueries
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkingPreferences

private val BRANCH_PARAMS_PREFIX = listOf("~", "$", "+")
private const val ACTION_QUERY = "action"
private const val SCREEN_QUERY = "screen"
private const val ENTITY_QUERY = "entity"

class BranchIoLinkConverter(
    private val deepLinkingPreferences: DeepLinkingPreferences
) {

    fun formatToDeepLink(data: BranchUniversalObject): Uri {
        val queries = data.contentMetadata.customMetadata
            .excludeInternalIOQueries()
            .toMutableMap()

        return Uri.Builder()
            .scheme(deepLinkingPreferences.deepLinkScheme)
            .authority(deepLinkingPreferences.deepLinkHost)
            .appendPathOrSkip(queries.extractAction())
            .appendPathOrSkip(queries.extractSubject())
            .appendQueries(queries)
            .build()
    }

    private fun Map<String, String>.excludeInternalIOQueries(): Map<String, String> {
        return filterKeys { key ->
            val isBranchIOQuery = BRANCH_PARAMS_PREFIX.any { prefix -> key.startsWith(prefix) }
            !isBranchIOQuery
        }
    }

    private fun MutableMap<String, String>.extractAction(): String? {
        return remove(ACTION_QUERY)
    }

    private fun MutableMap<String, String>.extractSubject(): String? {
        return remove(SCREEN_QUERY)
            ?: remove(ENTITY_QUERY)
    }
}
