package io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request

class ReferendumPreviewRequest {
    val query = """
        query {
            posts(
                where: {onchain_link: {onchain_referendum_id: {_is_null: false}}}
            ) {
                id
                title
                type {
                    name
                }
            }
        }
    """.trimIndent()
}
