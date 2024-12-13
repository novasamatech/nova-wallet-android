package io.novafoundation.nova.common.view

interface TableItem {

    fun disableOwnDividers()

    // TODO this is only needed until TableView has its own divider
    fun shouldDrawDivider(): Boolean
}
