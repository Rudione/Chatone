package io.rudione.chatone.util.settings

actual fun buildSettingsXlsx(backup: SettingsBackup): String {
    return SettingsImportExport.toCsv(backup)
}
