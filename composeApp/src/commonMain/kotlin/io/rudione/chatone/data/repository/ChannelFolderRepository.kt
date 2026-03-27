package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.presentation.main.ChannelFolder

class ChannelFolderRepository(
    private val database: ChatoneDatabase
) {
    private val queries get() = database.channelFolderQueries

    fun getAllFolders(): List<ChannelFolder> {
        return queries.getAllFolders().executeAsList().map { entity ->
            ChannelFolder(
                id = entity.id,
                name = entity.name,
                color = entity.color,
                isExpanded = entity.isExpanded != 0L
            )
        }
    }

    fun insertFolder(folder: ChannelFolder, sortOrder: Int = 0) {
        queries.insertFolder(
            id = folder.id,
            name = folder.name,
            color = folder.color,
            sortOrder = sortOrder.toLong(),
            isExpanded = if (folder.isExpanded) 1L else 0L
        )
    }

    fun updateFolderName(folderId: String, name: String) {
        queries.updateFolderName(name = name, id = folderId)
    }

    fun updateFolderColor(folderId: String, color: String) {
        queries.updateFolderColor(color = color, id = folderId)
    }

    fun updateFolderExpanded(folderId: String, expanded: Boolean) {
        queries.updateFolderExpanded(isExpanded = if (expanded) 1L else 0L, id = folderId)
    }

    fun deleteFolder(folderId: String) {
        queries.deleteFolder(id = folderId)
    }

    fun addChannelToFolder(channelId: String, folderId: String, sortOrder: Int = 0) {
        queries.addChannelToFolder(
            channelId = channelId,
            folderId = folderId,
            sortOrder = sortOrder.toLong()
        )
    }

    fun removeChannelFromFolder(channelId: String, folderId: String) {
        queries.removeChannelFromFolder(channelId = channelId, folderId = folderId)
    }

    fun getChannelLoginsInFolder(folderId: String): List<String> {
        return queries.getChannelIdsInFolder(folderId).executeAsList()
    }
}
