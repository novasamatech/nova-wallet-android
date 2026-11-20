package io.novafoundation.nova.feature_swap_impl.data

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.domain.config.SwapPairConfig
import javax.inject.Inject

/** Only token selections survive a new flow; amounts and quotes belong to the live form. */
class SwapSelectionDataSource @Inject constructor(
    private val preferences: Preferences,
    private val gson: Gson,
) {

    data class Selection(val assetIn: SwapPairConfig.Asset?, val assetOut: SwapPairConfig.Asset?)

    fun get(metaAccountId: Long): Selection? {
        val json = preferences.getString(key(metaAccountId)) ?: return null
        return runCatching { gson.fromJson(json, Selection::class.java) }.getOrNull()
    }

    fun set(metaAccountId: Long, selection: Selection) {
        preferences.putString(key(metaAccountId), gson.toJson(selection))
    }

    private fun key(metaAccountId: Long) = "swap_selected_tokens_$metaAccountId"
}
