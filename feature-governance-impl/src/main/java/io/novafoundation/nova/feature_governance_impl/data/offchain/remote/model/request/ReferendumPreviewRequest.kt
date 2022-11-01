package io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request

class ReferendumPreviewRequest() {
    val query = """
        query {
            posts(
                where: {type: {id: {_eq: 2}}, onchain_link: {onchain_referendum_id: {_is_null: false}}}
            ) {
                id
                title
            }
        }
    """.trimIndent()
}
// where: {title: {_is_null: false}, id: {_in: [${ids.joinToString()}]}}
