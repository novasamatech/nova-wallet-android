package io.novafoundation.nova.runtime.multiNetwork.chain

import android.content.Context
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.readAssetFile
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetsRemote

private const val BUNDLED_DEFAULT_ASSETS = "default_assets.json"

/**
 * The default token list shipped inside the app.
 *
 * It exists so that a first launch always has a list to apply: without it, a wallet created while
 * the remote config is unreachable would fall back to showing every token there is, and would then
 * look like an install that predates the feature - with no way back to the curated list.
 */
class BundledDefaultAssets(
    private val context: Context,
    private val gson: Gson
) {

    fun read(): DefaultAssetsRemote {
        return gson.fromJson(context.readAssetFile(BUNDLED_DEFAULT_ASSETS), DefaultAssetsRemote::class.java)
    }
}
