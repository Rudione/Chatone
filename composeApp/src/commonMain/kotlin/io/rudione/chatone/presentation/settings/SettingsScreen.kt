package io.rudione.chatone.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.bell_filled
import chatone.composeapp.generated.resources.bell_outlined
import chatone.composeapp.generated.resources.chatbubbles
import chatone.composeapp.generated.resources.chatbubbles_outline
import chatone.composeapp.generated.resources.icon
import chatone.composeapp.generated.resources.images
import chatone.composeapp.generated.resources.images_outline
import chatone.composeapp.generated.resources.key_outline
import chatone.composeapp.generated.resources.ic_sword
import chatone.composeapp.generated.resources.sparkle_filled
import chatone.composeapp.generated.resources.keyboard_24_filled
import chatone.composeapp.generated.resources.keyboard_24_regular
import chatone.composeapp.generated.resources.musical_notes_outline
import chatone.composeapp.generated.resources.palette_fill_16
import chatone.composeapp.generated.resources.palette_stroke_12
import chatone.composeapp.generated.resources.panel_left_key_16_regular
import chatone.composeapp.generated.resources.person_filled
import chatone.composeapp.generated.resources.shield_filled
import chatone.composeapp.generated.resources.shield_outlined
import chatone.composeapp.generated.resources.star_filled
import chatone.composeapp.generated.resources.star_outlined
import chatone.composeapp.generated.resources.unfold_more
import chatone.composeapp.generated.resources.wallpaper_filled
import chatone.composeapp.generated.resources.wallpaper_outlined
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.HotkeyRow
import io.rudione.chatone.presentation.settings.components.ModerationSettingsSection
import io.rudione.chatone.presentation.settings.components.NotificationGroupCard
import io.rudione.chatone.presentation.settings.components.CustomSoundCard
import io.rudione.chatone.presentation.settings.components.BackgroundCard
import io.rudione.chatone.presentation.settings.components.AboutCard
import io.rudione.chatone.presentation.settings.components.BackupCard
import io.rudione.chatone.presentation.settings.components.SettingsSurface
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.AccentColorPaletteRow
import io.rudione.chatone.presentation.settings.components.HighlightRuleCardFor
import io.rudione.chatone.presentation.settings.components.HightlightRuleCard
import io.rudione.chatone.presentation.settings.components.FontSettingsCard
import io.rudione.chatone.presentation.settings.sections.appearanceLazyItems
import io.rudione.chatone.presentation.settings.sections.chatLazyItems
import io.rudione.chatone.presentation.settings.sections.highlightLazyItems
import io.rudione.chatone.presentation.settings.sections.hotkeyLazyItems
import io.rudione.chatone.presentation.settings.sections.AppearanceContent
import io.rudione.chatone.presentation.settings.sections.ChatContent
import io.rudione.chatone.presentation.settings.sections.HighlightContent
import io.rudione.chatone.presentation.settings.sections.HotkeyContent
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import androidx.compose.runtime.CompositionLocalProvider
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.util.media.pickAudioFile
import io.rudione.chatone.util.media.pickImageFile
import io.rudione.chatone.util.font.pickFontFile
import io.rudione.chatone.util.font.resolveFontFamily
import io.rudione.chatone.util.font.listAvailableFontNames
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField

private enum class SettingsSection(
    val label: String,
    val icon: DrawableResource,
    val iconOutlined: DrawableResource? = null
) {
    APPEARANCE("Appearance", Res.drawable.palette_fill_16, Res.drawable.palette_stroke_12),
    CHAT("Chat", Res.drawable.chatbubbles, Res.drawable.chatbubbles_outline),
    NOTIFICATIONS("Notifications", Res.drawable.bell_filled, Res.drawable.bell_outlined),
    HIGHLIGHTS("Highlights", Res.drawable.star_filled, Res.drawable.star_outlined),
    BACKGROUND("Background", Res.drawable.wallpaper_filled, Res.drawable.wallpaper_outlined),
    HOTKEYS("Hotkeys", Res.drawable.keyboard_24_filled, Res.drawable.keyboard_24_regular),
    COMMANDS("Commands", Res.drawable.key_outline, Res.drawable.key_outline),
    ACTIONS("Actions", Res.drawable.ic_sword, Res.drawable.ic_sword),
    MODERATION("Moderation", Res.drawable.shield_filled, Res.drawable.shield_outlined),
    AI("AI Assistant", Res.drawable.sparkle_filled, null),
    ACCOUNT("Account", Res.drawable.person_filled, Res.drawable.person_filled),
    ABOUT("About", Res.drawable.panel_left_key_16_regular, null);

    fun localizedLabel(s: AppStrings): String = when (this) {
        APPEARANCE -> s.settingsAppearance
        CHAT -> s.sectionChat
        NOTIFICATIONS -> s.settingsNotifications
        HIGHLIGHTS -> s.sectionHighlights
        BACKGROUND -> s.sectionBackground
        HOTKEYS -> s.sectionHotkeys
        COMMANDS -> s.sectionCommands
        ACTIONS -> s.sectionActions
        MODERATION -> s.settingsModeration
        AI -> s.aiAssistantTitle
        ACCOUNT -> s.settingsAccount
        ABOUT -> s.settingsAbout
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    isWideScreen: Boolean = false,
    isDetached: Boolean = false,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    wallpaperLoader: WallpaperLoader = koinInject(),
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
    onLogoutSuccess: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val customThemeManager: CustomThemeManager = LocalCustomThemeManager.current
    val wallpaperController = LocalWallpaperController.current
    val s = LocalStrings.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is SettingsEffect.NavigateToAuth) {
                onNavigateBack()
            }
        }
    }

    if (isWideScreen && embedded) {
        SettingsDialogContent(
            state = state,
            onNavigateBack = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            onThemeChanged = onThemeChanged,
            viewModel = viewModel,
            isDetached = true,
            onOpenThemeCreator = { seedColor ->
                viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
            }
        )
    } else if (isWideScreen) {
        Dialog(
            onDismissRequest = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            SettingsDialogContent(
                state = state,
                onNavigateBack = {
                    onNavigateBack()
                    if (state.showThemeCreator) {
                        viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                    }
                },
                onThemeChanged = onThemeChanged,
                viewModel = viewModel,
                isDetached = isDetached,
                onOpenThemeCreator = { seedColor ->
                    viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
                }
            )
        }
    } else {
        SettingsFullScreen(
            state = state,
            onNavigateBack = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            onThemeChanged = onThemeChanged,
            modifier = modifier,
            viewModel = viewModel,
            onOpenThemeCreator = { seedColor ->
                viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
            }
        )
    }

    if (state.showThemeCreator) {
        Dialog(
            onDismissRequest = {
                viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            CompositionLocalProvider(
                LocalWallpaperController provides wallpaperController,
                LocalCustomThemeManager provides customThemeManager,
                LocalStrings provides s
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 16.dp,
                    shadowElevation = 32.dp
                ) {
                    ThemeSettingsScreen(
                        onNavigateBack = {
                            viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                        },
                        onThemeApplied = {
                            val active = customThemeManager.currentTheme.value
                            viewModel.sendEvent(SettingsEvent.OnCustomThemeApplied(active))
                        },
                        settingsViewModel = viewModel,
                        customThemeManager = customThemeManager,
                        initialSeedColor = state.themeCreatorSeedColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDialogContent(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    isDetached: Boolean = false,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var selectedSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    var searchQuery by remember { mutableStateOf("") }
    val extra = ChatoneTheme.extraColors

    val sectionKeywords: Map<SettingsSection, List<String>> = remember {
        mapOf(
            SettingsSection.APPEARANCE to listOf(
                "appearance", "theme", "color", "dark", "light", "font", "size", "scale", "ui",
                "ui scale", "font size", "font family", "font style", "title bar", "titlebar",
                "language", "locale", "compact", "density", "expressive", "palette", "accent",
                "corner radius", "rounding", "elevation", "transparency", "opacity", "glass", "blur",
                "sidebar", "menu", "animation", "ripple", "high contrast",
                "внешний вид", "тема", "цвет", "темная", "светлая", "шрифт", "размер", "масштаб",
                "масштаб интерфейса", "размер шрифта", "семейство шрифтов", "стиль шрифта",
                "заголовок окна", "язык", "локаль", "компактный", "плотность", "палитра",
                "акцент", "скругление", "тень", "прозрачность", "стекло", "размытие",
                "боковая панель", "анимация", "контраст"
            ),
            SettingsSection.CHAT to listOf(
                "chat", "message", "timestamp", "badge", "emote", "scroll", "deleted",
                "message density", "line spacing", "username color", "readable colors",
                "compact mode", "alternate background", "stripes", "inline images", "link preview",
                "image height", "image preview", "blur images", "emoji", "autocomplete",
                "command suggestions", "translate", "mentions", "pause", "pause hotkey", "auto scroll",
                "channel points", "warning", "raid", "raid countdown", "show timestamps", "12h", "24h",
                "deleted messages", "removed messages", "moderator actions", "show moderator",
                "spoof", "send as web", "platform badge",
                "чат", "сообщение", "время", "бейдж", "эмоут", "скролл", "удаленные",
                "плотность сообщений", "межстрочный интервал", "цвет имени", "читаемые цвета",
                "компактный режим", "чередование фона", "встроенные изображения", "превью ссылок",
                "высота изображения", "размытие изображений", "эмодзи", "автозаполнение",
                "подсказки команд", "перевод", "упоминания", "пауза", "автоскролл",
                "поинты канала", "предупреждения", "рейд", "обратный отсчет рейда",
                "показывать время", "удаленные сообщения", "действия модератора"
            ),
            SettingsSection.NOTIFICATIONS to listOf(
                "notification", "sound", "mention", "alert", "mute", "ping",
                "volume", "notification sound", "custom sound", "mention sound",
                "system notifications", "tray", "tray notifications", "live notifications",
                "follow notifications", "do not disturb", "dnd", "quiet hours",
                "уведомление", "звук", "упоминание", "тишина", "пинг",
                "громкость", "звук уведомлений", "свой звук", "звук упоминаний",
                "системные уведомления", "трей", "уведомления в трее", "лайв уведомления",
                "уведомления подписки", "не беспокоить", "тихие часы"
            ),
            SettingsSection.HIGHLIGHTS to listOf(
                "highlight", "keyword", "rule", "color", "regex",
                "highlight color", "highlight rule", "add highlight", "case sensitive",
                "whole word", "background color", "blink", "sound on highlight",
                "хайлайт", "ключевое слово", "правило", "цвет", "регулярка",
                "цвет хайлайта", "правило хайлайта", "добавить хайлайт", "регистр",
                "целое слово", "фоновый цвет", "мигание", "звук при хайлайте"
            ),
            SettingsSection.BACKGROUND to listOf(
                "background", "wallpaper", "image", "blur", "wallpaper opacity",
                "blur amount", "background color", "tint", "noise", "video wallpaper",
                "gradient", "fit", "cover", "stretch",
                "фон", "обои", "картинка", "размытие", "прозрачность обоев",
                "степень размытия", "цвет фона", "оттенок", "шум", "видео обои",
                "градиент", "вписать", "растянуть"
            ),
            SettingsSection.HOTKEYS to listOf(
                "hotkey", "shortcut", "keyboard", "keybind", "binding",
                "pause hotkey", "send message", "open settings", "switch panel",
                "next panel", "previous panel", "reset hotkey",
                "горячие клавиши", "сочетание", "клавиатура", "клавиша", "привязка",
                "клавиша паузы", "отправка сообщения", "открыть настройки", "переключение панели",
                "следующая панель", "предыдущая панель", "сбросить клавишу"
            ),
            SettingsSection.COMMANDS to listOf(
                "command", "commands", "trigger", "alias", "shortcut", "replace", "expand",
                "auto reply", "phrase", "abbreviation", "expander",
                "команда", "команды", "триггер", "алиас", "сокращение", "подмена", "замена",
                "автоответ", "фраза", "расширение"
            ),
            SettingsSection.ACTIONS to listOf(
                "action", "actions", "automation", "timer", "timed message", "auto reply",
                "keyword", "sound alert", "auto points", "bonus", "claim", "mute", "ignore",
                "действия", "автоматизация", "таймер", "автоответчик", "ключевое слово",
                "звук", "баллы", "бонус", "мьют", "игнор", "фраза"
            ),
            SettingsSection.MODERATION to listOf(
                "moderation", "mod", "ban", "timeout", "automod", "macro",
                "default timeout", "timeout duration", "ban reason", "custom reason",
                "mod actions", "mod buttons", "moderator buttons", "macros", "custom macros",
                "local automod", "chat rules", "rule trigger", "saved reasons",
                "repeated message", "nuke", "blocked term", "blockterm", "bot badge",
                "модерация", "мод", "бан", "таймаут", "автомод", "макрос",
                "стандартный таймаут", "длительность таймаута", "причина бана", "своя причина",
                "действия модератора", "кнопки мода", "макросы", "локальный автомод",
                "правила чата", "сохраненные причины", "повторные сообщения", "ньюк",
                "блок слова", "бот бейдж"
            ),
            SettingsSection.AI to listOf(
                "ai", "assistant", "gpt", "llm", "model", "neural", "ollama", "g4f",
                "lm studio", "auto mod scan", "ai automod", "endpoint", "base url",
                "ии", "ассистент", "нейросеть", "модель", "нейро", "искусственный интеллект",
                "ии автомод", "сканер"
            ),
            SettingsSection.ACCOUNT to listOf(
                "account", "login", "token", "auth", "profile", "proxy",
                "add account", "remove account", "primary account", "logout", "clear cache",
                "switch account", "multi account", "account proxy", "blocked users",
                "blocked", "block list", "ignore", "ignored", "unblock",
                "first party", "first-party", "device auth", "gql token", "integrity",
                "аккаунт", "логин", "токен", "авторизация", "профиль", "прокси",
                "добавить аккаунт", "удалить аккаунт", "основной аккаунт", "выйти", "очистить кеш",
                "переключить аккаунт", "несколько аккаунтов", "прокси аккаунта",
                "заблокированные", "блок лист", "игнор", "игнорированные", "разблокировать"
            ),
            SettingsSection.ABOUT to listOf(
                "about", "version", "info", "backup", "export", "import", "restore",
                "changelog", "license", "credits", "donate", "support", "github",
                "settings backup", "settings export", "settings import",
                "о программе", "версия", "информация", "бекап", "экспорт", "импорт",
                "восстановить", "история изменений", "лицензия", "поддержка",
                "бекап настроек", "экспорт настроек", "импорт настроек"
            )
        )
    }

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) SettingsSection.entries.toList()
        else {
            val q = searchQuery.trim().lowercase()
            SettingsSection.entries.filter { section ->
                section.label.lowercase().contains(q) ||
                        section.localizedLabel(s).lowercase().contains(q) ||
                        (sectionKeywords[section]?.any { it.contains(q) } == true)
            }
        }
    }

    LaunchedEffect(filteredSections) {
        if (filteredSections.isNotEmpty() && selectedSection !in filteredSections) {
            selectedSection = filteredSections.first()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(if (isDetached) 1f else 0.88f)
            .fillMaxHeight(if (isDetached) 1f else 0.86f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 32.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(216.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(extra.sidebarSurface, extra.sidebarSurface.copy(alpha = 0.95f))
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = extra.cardBorder,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height)
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        s.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Chatone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = extra.cardBorder)
                Spacer(Modifier.height(8.dp))

                ChatoneTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 52.dp),
                    placeholder = s.settingsSearchPlaceholder,
                    leading = {
                        Icon(
                            Icons.Filled.Search, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailing = if (searchQuery.isNotEmpty()) {
                        {
                            ChatoneIconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )

                Spacer(Modifier.height(6.dp))

                val navScrollState = rememberScrollState()
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(navScrollState)
                            .padding(horizontal = 8.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (filteredSections.isEmpty()) {
                            Text(
                                s.settingsSearchNoResults,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        } else {
                            filteredSections.forEach { section ->
                                SidebarNavItem(
                                    section = section,
                                    isSelected = selectedSection == section,
                                    onClick = { selectedSection = section },
                                    highlightQuery = searchQuery
                                )
                            }
                        }
                    }
                    SettingsThinScrollbarScroll(
                        scrollState = navScrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = extra.cardBorder)
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.close)
                }
            }

            CompositionLocalProvider(LocalSettingsSearch provides searchQuery) {
                SectionContentLazy(
                    section = selectedSection,
                    state = state,
                    onThemeChanged = onThemeChanged,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onOpenThemeCreator = onOpenThemeCreator
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFullScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var expandedSections by remember { mutableStateOf(setOf<SettingsSection>()) }
    val extra = ChatoneTheme.extraColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle) },
                navigationIcon = {
                    ChatoneIconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                extra.sidebarSurface,
                                extra.sidebarSurface.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                SettingsSection.entries.forEach { section ->
                    val isExpanded = section in expandedSections

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSections = if (isExpanded)
                                    expandedSections - section
                                else
                                    expandedSections + section
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(
                                if (isExpanded) section.icon
                                else (section.iconOutlined ?: section.icon)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isExpanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            section.localizedLabel(s),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isExpanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowDown
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = if (isExpanded) 0f else -90f },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                    ) {
                        SectionContentColumn(
                            section = section,
                            state = state,
                            onThemeChanged = onThemeChanged,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth(),
                            onOpenThemeCreator = onOpenThemeCreator
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            SettingsThinScrollbarScroll(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp)
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    section: SettingsSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    highlightQuery: String = ""
) {
    val s = LocalStrings.current
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        tween(150), label = "nav_bg"
    )
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painterResource(
                if (isSelected) section.icon
                else (section.iconOutlined ?: section.icon)
            ),
            null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        val label = section.localizedLabel(s)
        val q = highlightQuery.trim()
        val annotated = remember(label, q) {
            buildAnnotatedString {
                if (q.isEmpty()) {
                    append(label)
                } else {
                    val lowerLabel = label.lowercase()
                    val lowerQ = q.lowercase()
                    var i = 0
                    while (i < label.length) {
                        val idx = lowerLabel.indexOf(lowerQ, i)
                        if (idx < 0) {
                            append(label.substring(i))
                            break
                        }
                        if (idx > i) append(label.substring(i, idx))
                        withStyle(
                            SpanStyle(
                                background = androidx.compose.ui.graphics.Color(0xFFFFEE58).copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold
                            )
                        ) { append(label.substring(idx, idx + q.length)) }
                        i = idx + q.length
                    }
                }
            }
        }
        Text(
            annotated,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier.size(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SectionContentLazy(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    section.localizedLabel(s),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            when (section) {
                SettingsSection.APPEARANCE -> appearanceLazyItems(
                    state,
                    onThemeChanged,
                    viewModel,
                    onOpenThemeCreator
                )

                SettingsSection.ACCOUNT -> accountLazyItems(viewModel)
                SettingsSection.CHAT -> chatLazyItems(state, viewModel)
                SettingsSection.NOTIFICATIONS -> notificationLazyItems(state, viewModel)
                SettingsSection.HIGHLIGHTS -> highlightLazyItems(state, viewModel)
                SettingsSection.BACKGROUND -> backgroundLazyItems(state, viewModel)
                SettingsSection.HOTKEYS -> hotkeyLazyItems(state, viewModel)
                SettingsSection.COMMANDS -> commandsLazyItems(state, viewModel)
                SettingsSection.ACTIONS -> actionsLazyItems(state, viewModel)
                SettingsSection.MODERATION -> moderationLazyItems(state, viewModel)
                SettingsSection.AI -> aiLazyItems()
                SettingsSection.ABOUT -> aboutLazyItems(viewModel)
            }
        }
        SettingsThinScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp),
            coroutineScope = coroutineScope
        )
    }
}

@Composable
private fun SectionContentColumn(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (section) {
                SettingsSection.APPEARANCE -> AppearanceContent(
                    state,
                    onThemeChanged,
                    viewModel,
                    onOpenThemeCreator
                )

                SettingsSection.CHAT -> ChatContent(state, viewModel)
                SettingsSection.NOTIFICATIONS -> NotificationContent(state, viewModel)
                SettingsSection.HIGHLIGHTS -> HighlightContent(state, viewModel)
                SettingsSection.BACKGROUND -> BackgroundContent(state, viewModel)
                SettingsSection.HOTKEYS -> HotkeyContent(state, viewModel)
                SettingsSection.ACTIONS -> io.rudione.chatone.presentation.settings.components.ActionsSection(
                    state = state,
                    onEvent = { viewModel.sendEvent(it) }
                )
                SettingsSection.COMMANDS -> io.rudione.chatone.presentation.settings.components.ChatCommandsSection(
                    state = state,
                    onEvent = { viewModel.sendEvent(it) }
                )
                SettingsSection.MODERATION -> ModerationContent(state, viewModel)
                SettingsSection.AI -> {
                    io.rudione.chatone.presentation.settings.components.AiAssistantConfigCard()
                    io.rudione.chatone.presentation.settings.components.AiAutoModCard()
                }
                SettingsSection.ACCOUNT -> AccountContent(viewModel)
                SettingsSection.ABOUT -> AboutContent(viewModel)
            }
        }
    }
}

private fun LazyListScope.notificationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { NotificationGroupCard(state, vm) }
    if (state.mentionSoundEnabled) {
        item { CustomSoundCard(state, vm) }
    }
}

private fun LazyListScope.backgroundLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { BackgroundCard(state, vm) }
}

private fun LazyListScope.moderationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ModerationSettingsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
    item { io.rudione.chatone.presentation.settings.components.StreamerModeCard() }
}

private fun LazyListScope.aiLazyItems() {
    item { io.rudione.chatone.presentation.settings.components.AiAssistantConfigCard() }
    item { io.rudione.chatone.presentation.settings.components.AiAutoModCard() }
}

private fun LazyListScope.actionsLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        io.rudione.chatone.presentation.settings.components.ActionsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.commandsLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        io.rudione.chatone.presentation.settings.components.ChatCommandsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.aboutLazyItems(vm: SettingsViewModel) {
    item { BackupCard(vm) }
    item { AboutCard() }
}

private fun LazyListScope.accountLazyItems(vm: SettingsViewModel) {
    item {
        AccountContent(vm)
    }
}

@Composable
private fun AccountSectionBody(
    onLogout: () -> Unit,
    onClearCache: () -> Unit
) {
    val s = LocalStrings.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            s.settingsLogoutDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsLogout)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            s.settingsClearCacheDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsClearCache)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(s.settingsClearCache) },
            text = { Text(s.settingsConfirmClearCache) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCache()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(s.settingsClearCache) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(s.settingsCancel)
                }
            }
        )
    }
}

@Composable
private fun AccountContent(viewModel: SettingsViewModel) {
    val s = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val accountManager = org.koin.compose.koinInject<io.rudione.chatone.presentation.account.AccountManager>()
    val accountActions = org.koin.compose.koinInject<io.rudione.chatone.presentation.account.AccountActions>()
    val accountLoader = org.koin.compose.koinInject<io.rudione.chatone.presentation.account.AccountListLoader>()
    val accountUi = io.rudione.chatone.presentation.account.rememberAccountUiState(accountLoader, accountManager)
    val oauthHandler = org.koin.compose.koinInject<io.rudione.chatone.presentation.account.oauth.AddAccountOAuthHandler>()
    var showTokenDialog by remember { mutableStateOf(false) }

    SettingsGroup(s.accountsTitle) {
        io.rudione.chatone.presentation.account.AccountsSettingsSectionCompact(
            accounts = accountUi.accounts,
            accountManager = accountManager,
            onAddAccount = { showTokenDialog = true },
            onAddAccountBrowser = { oauthHandler.launchBrowserAuth { _ -> } },
            onRemoveAccount = { accountActions.remove(it) },
            onSetPrimary = { accountActions.setPrimary(it) }
        )
    }
    SettingsGroup(s.settingsAccount) {
        AccountSectionBody(
            onLogout = { viewModel.sendEvent(SettingsEvent.OnLogoutClicked) },
            onClearCache = { viewModel.sendEvent(SettingsEvent.OnClearCacheClicked) }
        )
    }
    io.rudione.chatone.presentation.settings.components.FirstPartyTokenCard()
    LaunchedEffect(Unit) {
        if (state.blockedUsernames.isEmpty() && !state.isLoadingBlockedUsers) {
            viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers)
        }
    }
    io.rudione.chatone.presentation.settings.components.BlockedUsersSection(
        blockedUsernames = state.blockedUsernames,
        showBlockedMode = state.showBlockedMode,
        isLoadingBlockedUsers = state.isLoadingBlockedUsers,
        loadError = state.blockedLoadError,
        onShowBlockedModeChange = { viewModel.sendEvent(SettingsEvent.OnShowBlockedModeChanged(it)) },
        onUnblockUser = { username ->
            viewModel.sendEvent(SettingsEvent.OnUnblockUserFromSettings(username, ""))
        },
        onRefresh = { viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers) }
    )

    if (showTokenDialog) {
        io.rudione.chatone.presentation.account.AccountAddDialog(
            onDismiss = { showTokenDialog = false },
            onLaunchOAuth = {
                oauthHandler.launchBrowserAuth { _ -> }
                showTokenDialog = false
            },
            onSubmitToken = { token ->
                oauthHandler.completeWithToken(token) { _ -> }
                showTokenDialog = false
            }
        )
    }
}

@Composable
private fun NotificationContent(state: SettingsState, vm: SettingsViewModel) {
    NotificationGroupCard(state, vm)
    if (state.mentionSoundEnabled) {
        Spacer(Modifier.height(8.dp))
        CustomSoundCard(state, vm)
    }
}

@Composable
private fun BackgroundContent(state: SettingsState, vm: SettingsViewModel) {
    BackgroundCard(state, vm)
}

@Composable
private fun ModerationContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsModeration) {
        Column(modifier = Modifier.padding(12.dp)) {
            ModerationSettingsSection(
                state = state,
                onEvent = { vm.sendEvent(it) }
            )
        }
    }
}

@Composable
private fun AboutContent(vm: SettingsViewModel) {
    BackupCard(vm)
    AboutCard()
}

@Composable
internal fun MentionTabsSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMentionTabs) {
        SwitchRow(s.settingsMentionTabs, s.settingsMentionTabsDesc, state.mentionTabsEnabled) {
            vm.sendEvent(SettingsEvent.OnMentionTabsChanged(it))
        }
    }
}

@Composable
internal fun TranslationSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    val langs = io.rudione.chatone.presentation.chat.TranslationLanguages
    val currentName = langs.firstOrNull { it.first == state.translationTargetLang }?.second
        ?: state.translationTargetLang
    SettingsGroup(s.settingsTranslationLang) {
        ListRow(
            s.settingsTranslationLangDesc,
            currentName,
            langs.map { it.second }
        ) { idx -> vm.sendEvent(SettingsEvent.OnTranslationLangChanged(langs[idx].first)) }
        SwitchRow(s.settingsAutoTranslateInput, s.settingsAutoTranslateInputDesc, state.autoTranslateInput) {
            vm.sendEvent(SettingsEvent.OnAutoTranslateInputChanged(it))
        }
    }
}

@Composable
internal fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(LocalStrings.current.settingsUiScale, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${(currentScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        ChatoneSlider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.7f..2.0f,
            steps = 12,
            modifier = Modifier.width(180.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds < 3600 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
    seconds < 86400 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
    else -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
}

@Composable
private fun SettingsThinScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) {
    io.rudione.chatone.presentation.components.ChatoneLazyScrollbar(
        listState = listState,
        itemCount = listState.layoutInfo.totalItemsCount,
        modifier = modifier
    )
}

@Composable
private fun SettingsThinScrollbarScroll(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    io.rudione.chatone.presentation.components.ChatoneScrollbar(
        scrollState = scrollState,
        modifier = modifier
    )
}
