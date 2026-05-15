package io.rudione.chatone.util

actual fun buildSettingsXlsx(backup: SettingsBackup): String {
    return SettingsImportExport.toCsv(backup)
}
