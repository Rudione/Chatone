@file:OptIn(ExperimentalForeignApi::class)

package io.rudione.chatone.util.network

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.toKString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_interface_get_index
import platform.Network.nw_interface_get_name
import platform.Network.nw_path_enumerate_interfaces
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_global_queue
import kotlinx.cinterop.ExperimentalForeignApi

internal actual fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
    val monitor = nw_path_monitor_create()
    val revision = atomic(0L)
    nw_path_monitor_set_update_handler(monitor) { path ->
        val interfaces = mutableListOf<String>()
        nw_path_enumerate_interfaces(path) { network ->
            interfaces.add("${nw_interface_get_index(network)}:${nw_interface_get_name(network)?.toKString()}")
            true
        }
        trySend(NetworkStatus(nw_path_get_status(path) == nw_path_status_satisfied, interfaces.sorted().joinToString("|"), routeRevision = revision.incrementAndGet()))
    }
    nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(0, 0uL))
    nw_path_monitor_start(monitor)
    awaitClose { nw_path_monitor_cancel(monitor) }
}.distinctUntilChanged().withAppResumes()
