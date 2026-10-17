package io.rudione.chatone.presentation.settings.search

import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.settings.isAvailable
import io.rudione.chatone.presentation.settings.sections.hotkeyActionLabel
import io.rudione.chatone.presentation.settings.sections.moderationTabLabel
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.util.platform.InputCapabilities
import io.rudione.chatone.util.system.HotkeyAction

internal class CatalogText(
    val title: String,
    val group: String?,
    val description: String?,
    val sectionLabel: String
)

internal class SettingsSearchEntry(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val groupTitle: String?,
    text: CatalogText,
    translations: List<CatalogText>,
    keywords: String
) {
    val title: String = text.title
    val group: String? = text.group
    val description: String? = text.description
    val anchor: String get() = title
    val fallbackAnchor: String? get() = group
    val id: String = "${section.name}:${tab?.name.orEmpty()}:$title:${group.orEmpty()}"

    val document = SearchDocument(
        buildList {
            add(SearchField(text.title, TITLE_WEIGHT))
            if (keywords.isNotBlank()) add(SearchField(keywords, KEYWORD_WEIGHT))
            text.group?.let { add(SearchField(it, GROUP_WEIGHT)) }
            text.description?.let { add(SearchField(it, DESCRIPTION_WEIGHT)) }
            add(SearchField(text.sectionLabel, SECTION_WEIGHT))
            for (translation in translations) {
                add(SearchField(translation.title, TITLE_WEIGHT * TRANSLATION_FACTOR))
                translation.group?.let { add(SearchField(it, GROUP_WEIGHT * TRANSLATION_FACTOR)) }
                translation.description?.let { add(SearchField(it, DESCRIPTION_WEIGHT * TRANSLATION_FACTOR)) }
                add(SearchField(translation.sectionLabel, SECTION_WEIGHT * TRANSLATION_FACTOR))
            }
        }
    )

    private companion object {
        const val TITLE_WEIGHT = 3f
        const val KEYWORD_WEIGHT = 2f
        const val GROUP_WEIGHT = 1.4f
        const val DESCRIPTION_WEIGHT = 1f
        const val SECTION_WEIGHT = 0.9f
        const val TRANSLATION_FACTOR = 0.9f
    }
}

internal data class SettingsSubsection(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val title: String,
    val anchor: String?
)

internal fun settingsSubsections(
    catalog: List<SettingsSearchEntry>,
    strings: AppStrings
): Map<SettingsSection, List<SettingsSubsection>> =
    catalog
        .mapNotNull { entry ->
            val tab = entry.tab
            when {
                tab != null -> SettingsSubsection(entry.section, tab, moderationTabLabel(tab, strings.modSlider), anchor = null)
                entry.groupTitle != null -> SettingsSubsection(entry.section, null, entry.groupTitle, anchor = entry.groupTitle)
                else -> null
            }
        }
        .distinct()
        .groupBy { it.section }

internal data class SettingsSearchHit(val entry: SettingsSearchEntry, val score: Float)

internal fun searchSettings(query: String, catalog: List<SettingsSearchEntry>): List<SettingsSearchHit> {
    if (query.isBlank()) return emptyList()
    val scored = catalog
        .mapIndexed { index, entry -> Triple(index, entry, SettingsSearchEngine.score(query, entry.document)) }
        .filter { it.third > 0f }
    val floor = (scored.maxOfOrNull { it.third } ?: 0f) * RELATIVE_SCORE_FLOOR
    return scored
        .filter { it.third >= floor }
        .sortedWith(compareByDescending<Triple<Int, SettingsSearchEntry, Float>> { it.third }.thenBy { it.first })
        .map { SettingsSearchHit(it.second, it.third) }
}

private const val RELATIVE_SCORE_FLOOR = 0.3f

data class SettingsCatalogContext(
    val input: InputCapabilities,
    val desktop: Boolean,
    val phone: Boolean,
    val macOs: Boolean = false
)

internal fun buildSettingsCatalog(
    s: AppStrings,
    context: SettingsCatalogContext,
    languages: List<AppStrings> = AppLocale.all.map { AppStrings.forLocale(it.code) }
): List<SettingsSearchEntry> {
    val primary = collectCatalog(s, context)
    val translations = languages.filter { it !== s }.map { collectCatalog(it, context) }
        .filter { it.size == primary.size }
    return primary.mapIndexed { index, item ->
        SettingsSearchEntry(
            section = item.section,
            tab = item.tab,
            groupTitle = item.groupTitle,
            text = item.text,
            translations = translations.map { it[index].text },
            keywords = item.keywords
        )
    }
}

private class CatalogItem(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val groupTitle: String?,
    val text: CatalogText,
    val keywords: String
)

private fun collectCatalog(s: AppStrings, context: SettingsCatalogContext): List<CatalogItem> {
    val hub = s.settingsHub
    val slider = s.modSlider
    val builder = CatalogBuilder(s, context)
    with(builder) {
        section(SettingsSection.APPEARANCE) {
            group(s.settingsTheme) {
                entry(s.themeAccentColor, s.themeAccentColorHint, "тема цвет акцент палитра оформление темная светлая theme color accent palette dark light")
                entry(s.settingsSetCustom, keywords = "создать тему редактор свою тему theme creator custom")
            }
            group(hub.groupInterface) {
                entry(s.settingsUiScale, keywords = "масштаб размер интерфейса зум ui scale zoom")
                entry(s.settingsLanguage, s.settingsLanguageDesc, "язык локаль русский английский language locale english russian")
                if (!context.phone) {
                    entry(s.settingsChannelNavigation, keywords = "вкладки мини панель rail tabs навигация каналов")
                    entry(s.settingsNavigationHidden, s.settingsNavigationHiddenDesc, "скрыть вкладки hide tabs navigation")
                }
            }
            group(s.settingsTypography) {
                entry(s.settingsFontFamily, keywords = "шрифт семейство font family typeface")
                entry(s.settingsFontItalic, keywords = "курсив italic")
                entry(s.settingsFontUnderline, keywords = "подчеркивание underline")
                entry(s.settingsFontStrikethrough, keywords = "зачеркивание strikethrough")
            }
            if (context.desktop || context.phone) {
                group(s.settingsWindow) {
                    if (context.desktop) entry(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, "поверх всех окон always on top pin")
                    if (context.desktop) entry(s.settingsAlwaysOnTopMode, s.settingsAlwaysOnTopModeDesc, "поверх браузера системных окон above browser system windows")
                    entry(s.settingsHideSidebar, s.settingsHideSidebarDesc, "боковая панель боковую панель скрывать sidebar")
                    if (context.desktop) entry(s.settingsTitleBarMode, s.settingsTitleBarModeDesc, "заголовок окна title bar titlebar")
                }
            }
        }
        section(SettingsSection.CHAT) {
            group(hub.groupMessageLook) {
                entry(s.settingsChatMessageScale, s.settingsChatMessageScaleHint, "масштаб сообщений размер zoom scale")
                entry(s.settingsFontSize, keywords = "размер шрифта текст font size")
                entry(s.settingsEmoteSize, keywords = "эмоуты смайлы emotes emote size")
                entry(s.settingsMessageSpacing, keywords = "плотность отступы интервал spacing density")
            }
            group(hub.groupRowDecor) {
                entry(s.settingsTimestamps, s.settingsShowTimestampsDesc, "время метки времени timestamp time 12h 24h")
                entry(s.settingsTimestampFormat, keywords = "формат времени 12 24 часа format")
                entry(s.chatShowBadges, s.chatShowBadgesDesc, "значки бейджи badges")
                entry(s.settingsAlternateRowBg, s.settingsAlternateRowBgDesc, "зебра полосы чередование stripes zebra")
                entry(s.settingsReadableNickColors, s.settingsReadableNickColorsDesc, "цвет ника читаемые username color contrast")
            }
            group(s.settingsMessages) {
                entry(s.settingsShowDeletedMessages, s.settingsShowDeletedMessagesDesc, "удаленные сообщения deleted removed")
                entry(s.settingsShowViewerJoinLeave, s.settingsShowViewerJoinLeaveDesc, "заходы выходы зрителей join leave")
                entry(s.settingsSmoothChat, s.settingsSmoothChatDesc, "плавный чат анимация smooth")
                entry(s.settingsShowRepeatedCounter, s.settingsShowRepeatedCounterDesc, "повторы счетчик спам repeat counter")
                entry(s.settingsRepeatedWindow, keywords = "окно повторов секунды repeat window")
            }
            group(hub.groupChatPanel) {
                entry(s.settingsShowChatHeader, s.settingsShowChatHeaderDesc, "шапка заголовок канала header")
                entry(s.settingsChatScrollbarWidth, keywords = "полоса прокрутки скроллбар ширина scrollbar width")
                entry(s.settingsChatTopBarHeight, keywords = "верхняя панель высота top bar height")
            }
            group(s.settingsChatInputGroup) {
                entry(s.settingsHidePlaceholder, s.settingsHidePlaceholderDesc, "подсказка поле ввода placeholder")
                entry(s.settingsHideEmojiButton, s.settingsHideEmojiButtonDesc, "кнопка эмоутов смайлики emoji button")
                entry(s.settingsChatInputGlow, s.settingsChatInputGlowDesc, "свечение подсветка поля glow")
            }
            if (context.input.pointer) {
                group(hub.groupMouse) {
                    entry(s.settingsPauseOnHover, s.settingsPauseOnHoverDesc, "пауза при наведении автопрокрутка hover pause scroll")
                    entry(s.settingsCloseOnMouseLeave, s.settingsCloseOnMouseLeaveDesc, "закрывать панель эмоутов emote picker")
                }
            }
            group(s.settingsHistoryGroup) {
                entry(s.settingsMessageHistoryLimit, keywords = "история лимит сообщений scrollback history limit")
            }
            group(s.settingsMentionTabs) {
                entry(s.settingsMentionTabs, s.settingsMentionTabsDesc, "упоминания вкладки mentions")
            }
            group(s.settingsTranslationLang) {
                entry(s.settingsTranslationLangDesc, keywords = "перевод язык translate translation")
                entry(s.settingsAutoTranslateInput, s.settingsAutoTranslateInputDesc, "автоперевод перевод исходящих auto translate")
                entry(s.settingsOutgoingTranslationLang, s.settingsOutgoingTranslationLangDesc, "язык исходящих outgoing")
            }
        }
        section(SettingsSection.SCREEN_CHAT) {
            val screen = s.screenChat
            group(null) {
                entry(screen.settingsTitle, screen.settingsHelp, "экранный чат оверлей окно поверх прозрачный стрим игра overlay screen chat")
            }
            group(screen.groupWindow) {
                entry(screen.layerTitle, screen.layerHelp, "поверх всех окон слой always on top layer")
                if (context.macOs) {
                    entry(screen.overFullScreenTitle, screen.overFullScreenHelp, "полноэкранный фуллскрин весь экран dock full screen fullscreen")
                    entry(screen.keepDockTitle, screen.keepDockHelp, "значок док иконка dock icon cmd tab")
                }
                entry(screen.lockedTitle, screen.lockedHelp, "закрепить окна заблокировать lock pin")
                entry(screen.hideDelayTitle, keywords = "скрывать панель задержка hide delay")
            }
            group(screen.groupMessages) {
                entry(screen.replyAboveTitle, screen.replyAboveHelp, "ответ реплай reply строка сверху")
                entry(screen.moderationTitle, screen.moderationHelp, "логи модерации муты баны таймауты удаления mod log timeouts bans")
            }
            group(screen.groupLook) {
                entry(screen.fontSizeTitle, keywords = "шрифт размер текста ники font size")
                entry(screen.scaleTitle, keywords = "масштаб экранного чата size scale zoom")
                entry(screen.textTitle, keywords = "прозрачность сообщений текст opacity")
                entry(screen.backgroundTitle, keywords = "фон прозрачность background opacity")
                entry(screen.backdropTitle, screen.backdropHelp, "подложка плашка фон под сообщениями backdrop")
                entry(screen.backdropOpacityTitle, keywords = "плотность подложки backdrop opacity")
            }
        }
        section(SettingsSection.MEDIA) {
            group(hub.groupLinksMedia) {
                entry(s.settingsOpenLinks, keywords = "ссылки браузер инкогнито links browser incognito")
                entry(s.settingsShowInlineImages, s.settingsShowInlineImagesDesc, "картинки изображения превью размытие inline images blur preview")
                entry(s.settingsImageMaxHeight, keywords = "высота картинок image height")
                entry(s.settingsClipPreviewWidth, keywords = "клипы превью clip preview")
                entry(s.settingsShowChatGifs, s.settingsShowChatGifsDesc, "гифки gif анимации")
                entry(s.settingsGiphyApiKey, s.settingsGiphyApiKeyDesc, "giphy ключ api key")
            }
            if (context.desktop) {
                group(hub.uploaderBehaviourGroup) {
                    entry(s.uploaderEnable, s.uploaderDesc, "загрузчик картинки скриншоты хостинг uploader image upload")
                    entry(s.uploaderAskOnUpload, keywords = "подтверждение confirm ask")
                }
                group(hub.uploaderServiceGroup) {
                    entry(s.uploaderRequestUrl, keywords = "адрес url сервер endpoint")
                    entry(s.uploaderFormField, keywords = "поле формы form field multipart")
                    entry(hub.uploaderFieldsTitle, hub.uploaderFieldsHelp, "свои поля параметры ключ api key fields")
                    entry(s.uploaderExtraHeaders, keywords = "заголовки headers authorization токен")
                    entry(s.uploaderImageLink, hub.uploaderLinkHelp, "ссылка json путь link")
                    entry(s.uploaderDeletionLink, keywords = "удаление ссылка delete link")
                    entry(s.uploaderCheckStatus, keywords = "проверить доступность статус check status")
                }
            }
        }
        section(SettingsSection.NOTIFICATIONS) {
            if (context.phone) {
                group(null) {
                    entry(s.settingsMentionAlerts, s.settingsMentionAlertsDesc, "пуш уведомления телефон push")
                }
            }
            group(s.settingsMentionSound) {
                entry(s.settingsEnableMentionSound, s.settingsEnableMentionSoundDesc, "звук упоминания пинг mention sound ping")
                entry(s.settingsVolume, keywords = "громкость volume")
            }
            group(s.settingsCustomSound) {
                entry(s.settingsCustomSound, s.settingsSoundSupported, "свой звук wav ogg custom sound")
            }
        }
        section(SettingsSection.HIGHLIGHTS) {
            group(hub.highlightBuiltIn) {
                entry(s.highlightRuleUsername, keywords = "ник упоминание mention username")
                entry(s.highlightRuleWhispers, keywords = "шепот личные whispers")
                entry(s.highlightRuleSubscriptions, keywords = "подписки сабы subs subscriptions")
                entry(s.highlightRuleFirstMessage, keywords = "первое сообщение first message")
                entry(s.highlightRuleSearchMatch, keywords = "поиск совпадения search")
                entry(s.highlightRuleChannelPoints, keywords = "баллы поинты награды channel points")
            }
            group(hub.highlightCustom) {
                entry(s.settingsAddRule, s.settingsHighlightRulesHint, "правило слово регулярка regex keyword rule цвет color")
            }
        }
        section(SettingsSection.BACKGROUND) {
            group(s.settingsChatBackgroundImage) {
                entry(s.settingsChatBackgroundImage, keywords = "фон обои картинка wallpaper background image")
                entry(s.settingsOverlayOpacity, keywords = "затемнение прозрачность opacity dim")
            }
        }
        if (SettingsSection.HOTKEYS.isAvailable(context.input)) {
            section(SettingsSection.HOTKEYS) {
                group(s.hotkeysCombinationsTitle) {
                    HotkeyAction.entries.forEach { action ->
                        entry(hotkeyActionLabel(action, s), keywords = "горячая клавиша сочетание hotkey shortcut")
                    }
                }
                group(s.settingsChatControls) {
                    entry(s.settingsPauseAutoScroll, s.settingsPauseAutoScrollDesc, "пауза клавиша мышь pause hotkey")
                    entry(s.settingsPauseMode, keywords = "удерживать переключать hold toggle")
                    entry(s.settingsBlockScroll, s.settingsBlockScrollDesc, "прокрутка колесико alt scroll")
                }
            }
        }
        section(SettingsSection.COMMANDS) {
            group(s.settingsCommandsTitle) {
                entry(s.settingsCommandsTitle, s.settingsCommandsDesc, "команды сокращения алиасы замена триггер commands alias trigger expander")
                entry(s.settingsCommandsAdd, keywords = "новая команда add command")
            }
        }
        section(SettingsSection.ACTIONS) {
            group(s.actionsAutomations) {
                entry(s.actionsAutomations, s.actionsHint, "автоматизации таймер автоответ автоответчик automation timer auto reply")
                entry(s.actionsKindTimer, s.actionsAutomationsHint, "сообщения по таймеру timed message")
                entry(s.actionsKindReply, s.actionsSafetyNote, "автоответ ключевое слово keyword reply")
                entry(s.actionsKindSound, keywords = "звук оповещение sound alert")
            }
            group(s.actionsMutedPhrases) {
                entry(s.actionsMutedPhrases, s.actionsMutedPhrasesHint, "скрытые фразы мьют игнор muted phrases ignore filter")
            }
        }
        section(SettingsSection.MODERATION) {
            tab(ModerationTab.GENERAL) {
                group(s.modDefaultTimeoutDuration) {
                    entry(s.modDefaultTimeoutDuration, keywords = "таймаут по умолчанию длительность default timeout")
                }
                group(s.modSavedReasonsTitle) {
                    entry(s.modSavedTimeoutReasonLabel, s.modSavedReasonsDesc, "причина таймаута reason")
                    entry(s.modSavedBanReasonLabel, keywords = "причина бана ban reason")
                }
                group(s.modLocalAutomod) {
                    entry(s.modLocalAutomod, s.modLocalAutomodDesc, "автомод правила automod rules local")
                }
            }
            tab(ModerationTab.BUTTONS) {
                group(s.modModActionButtons) {
                    entry(s.modModActionButtons, s.modDragReorderHint, "кнопки модерации удалить таймаут бан порядок mod buttons delete timeout ban")
                    entry(s.modButtonColors, s.modButtonColorHint, "цвета кнопок colors")
                    entry(s.modSlider.buttonsInChat, s.modSlider.buttonsInChatHelp, "кнопки в чате скрыть выключить спрятать мини-профиль buttons in chat hide")
                    if (context.input.pointer) entry(s.modButtonsOnHover, s.modButtonsOnHoverDesc, "при наведении hover")
                    entry(s.modAddTimeoutButton, keywords = "добавить таймаут кнопку custom timeout")
                }
            }
            if (context.desktop) {
                tab(ModerationTab.SLIDER) {
                    group(null) {
                        entry(slider.enableTitle, slider.enableHelp, "слайдер ручка включить выключить модерация slider handle enable")
                    }
                    group(slider.scaleTitle) {
                        entry(slider.twitchScale, slider.twitchScaleHelp, "шкала твич секунды минуты часы дни twitch scale")
                        entry(slider.stepsTitle, slider.stepsHelp, "свои сроки мута таймауты custom steps durations")
                    }
                    group(slider.travelTitle) {
                        entry(slider.deleteAtStart, slider.deleteAtStartHelp, "удаление delete")
                        entry(slider.banAtEnd, slider.banAtEndHelp, "бан ban")
                        entry(slider.travelTitle, slider.travelHelp, "ход длина процент travel percent")
                    }
                    group(slider.previewTitle) {
                        entry(slider.previewTitle, slider.previewHint, "попробовать пример preview demo")
                    }
                }
            }
            tab(ModerationTab.MACROS) {
                group(s.modMacros) {
                    entry(s.modMacros, s.modMacrosDesc, "макросы быстрая панель macros quick bar")
                    entry(s.modCreateMacro, keywords = "новый макрос new macro")
                }
            }
        }
        section(SettingsSection.AI) {
            group(s.aiConfigTitle) {
                entry(s.aiEnable, s.aiEnableDesc, "ии ассистент нейросеть ollama lm studio gpt llm модель ai assistant")
            }
            group(s.aiAutoModTitle) {
                entry(s.aiMonitorTitle, s.aiMonitorDesc, "ии автомод сканер мониторинг ai automod")
                entry(s.aiSensitivity, keywords = "чувствительность sensitivity")
                entry(s.aiActiveHours, keywords = "часы расписание hours schedule")
            }
        }
        section(SettingsSection.ACCOUNT) {
            group(s.accountsTitle) {
                entry(s.accountsTitle, keywords = "аккаунты несколько аккаунтов переключить основной accounts switch primary multi")
                entry(s.accountsAdd, keywords = "добавить аккаунт войти login add account")
                entry(s.accountsProxyTitle, s.accountsProxyDesc, "прокси socks http proxy")
            }
            group(s.settingsAccount) {
                entry(s.settingsLogout, s.settingsLogoutDesc, "выйти выход logout sign out")
                entry(s.settingsClearCache, s.settingsClearCacheDesc, "кеш данные очистить cache clear")
            }
            group(s.extRightsTitle) {
                entry(s.extRightsTitle, keywords = "токен веб клиента first party gql integrity расширенные права token")
            }
            group(s.blockedUsers) {
                entry(s.blockedUsers, s.blockedUsersDesc, "заблокированные блок лист игнор blocked ignore unblock")
                entry(s.blockedUsersShowMessages, keywords = "сообщения заблокированных blocked messages")
            }
            group(s.streamerModeTitle) {
                entry(s.streamerModeHideData, s.streamerModeObs, "стример режим obs приватность скрывать streamer mode privacy")
                entry(s.streamerMaskTokens, keywords = "токены маска tokens")
                entry(s.streamerHideMod, keywords = "мод действия mod actions")
                entry(s.streamerMaskChannels, keywords = "каналы названия channels")
                entry(s.streamerHideThumbs, keywords = "превью thumbnails")
                entry(s.streamerSuppressNotif, keywords = "уведомления notifications")
            }
        }
        section(SettingsSection.ABOUT) {
            group(s.settingsBackupTitle) {
                entry(s.settingsBackupTitle, s.settingsBackupDesc, "бекап экспорт импорт резервная копия backup export import restore")
            }
            group(s.settingsAppInfo) {
                entry(s.settingsVersion, keywords = "версия о программе version about changelog")
                entry(s.settingsTelegram, keywords = "телеграм поддержка telegram support")
            }
        }
    }
    return builder.entries
}

private class CatalogBuilder(private val strings: AppStrings, private val context: SettingsCatalogContext) {
    val entries = mutableListOf<CatalogItem>()
    private var section: SettingsSection = SettingsSection.APPEARANCE
    private var tab: ModerationTab? = null
    private var group: String? = null

    fun section(target: SettingsSection, block: CatalogBuilder.() -> Unit) {
        if (!target.isAvailable(context.input, context.desktop)) return
        section = target
        tab = null
        block()
    }

    fun tab(target: ModerationTab, block: CatalogBuilder.() -> Unit) {
        tab = target
        block()
        tab = null
    }

    fun group(title: String?, block: CatalogBuilder.() -> Unit) {
        group = title
        block()
        group = null
    }

    fun entry(title: String, description: String? = null, keywords: String = "") {
        entries += CatalogItem(
            section = section,
            tab = tab,
            groupTitle = group,
            text = CatalogText(
                title = title,
                group = group?.takeIf { it != title },
                description = description?.takeIf { it.isNotBlank() },
                sectionLabel = section.localizedLabel(strings)
            ),
            keywords = keywords
        )
    }
}
