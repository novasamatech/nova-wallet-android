package io.novafoundation.nova.common.data.preferences

/**
 * Static configuration for the mandatory ToS / Privacy Notice consent banner
 * required by Aurum (Nova's legal counsel) on every wallet creation / import flow.
 *
 * The hosted novasama.io URLs depend on rollout action items C-01 / C-02 and are not
 * yet live. Until they are, the placeholders below are intentionally invalid so that
 * any accidental release before the URLs land will be immediately visible.
 */
object ConsentBannerConstants {

    /**
     * Bump this when a new ToS / Privacy Notice revision ships and existing acceptances
     * must be re-collected. The persisted accepted version is compared against this value
     * to decide whether the user has accepted the current revision.
     */
    const val CURRENT_CONSENT_VERSION: Int = 1

    /**
     * Placeholder until rollout action item C-01 lands the canonical novasama.io page.
     * MUST be replaced before merging to the upstream develop branch.
     */
    const val TERMS_OF_SERVICE_URL: String = "https://novasama.io/TODO_CONSENT_URL_TERMS"

    /**
     * Placeholder until rollout action item C-02 lands the canonical novasama.io page.
     * MUST be replaced before merging to the upstream develop branch.
     */
    const val PRIVACY_NOTICE_URL: String = "https://novasama.io/TODO_CONSENT_URL_PRIVACY"
}
