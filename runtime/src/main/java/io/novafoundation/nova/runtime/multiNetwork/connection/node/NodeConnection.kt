package io.novafoundation.nova.runtime.multiNetwork.connection.node


interface NodeConnection {

    /**
     * Should return connection delay in ms
     */
    suspend fun testNodeHealthState(): Result<Long>

}
