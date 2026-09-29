package io.rudione.chatone.presentation.theme.i18n

interface StreamPlayerStrings {
    val watch: String
    val close: String
    val play: String
    val pause: String
    val mute: String
    val unmute: String
    val fullscreen: String
    val exitFullscreen: String
    val pictureInPicture: String
    val settings: String
    val latency: String
    val latencyCompact: String
    val live: String
    val quality: String
    val qualityAuto: String
    val qualityAutoActive: String
    val audioOnly: String
    val source: String
    val lowLatency: String
    val lowLatencyDescription: String
    val video: String
    val scaleFit: String
    val scaleFill: String
    val landscapeChat: String
    val chatSide: String
    val chatOverlay: String
    val chatHidden: String
    val overlayLock: String
    val overlayUnlock: String
    val chatBackground: String
    val chatMessagesOpacity: String
    val autoPictureInPicture: String
    val autoPictureInPictureDescription: String
    val videoStats: String
    val offline: String
    val otherChannelNotice: String
    val retry: String
    val errorNetwork: String
    val errorGeoblocked: String
    val errorSubscribersOnly: String
    val errorCodec: String
    val errorForbidden: String
    val errorNotFound: String
    val errorUnknown: String
    val statDownloadResolution: String
    val statRenderResolution: String
    val statViewportResolution: String
    val statDownloadBitrate: String
    val statBandwidth: String
    val statFps: String
    val statSkippedFrames: String
    val statBuffer: String
    val statLatency: String
    val statCodecs: String
    val statProtocol: String
    val statLatencyMode: String
    val statLatencyLow: String
    val statLatencyNormal: String
    val statRenderSurface: String
    val statBackend: String
    val statPlaySession: String
    val statServingId: String
    val unitSeconds: String
    val unitKbps: String
    val decimalSeparator: Char
}

object StreamPlayerStringsEn : StreamPlayerStrings {
    override val watch = "Watch stream"
    override val close = "Close player"
    override val play = "Play"
    override val pause = "Pause"
    override val mute = "Mute"
    override val unmute = "Unmute"
    override val fullscreen = "Fullscreen"
    override val exitFullscreen = "Exit fullscreen"
    override val pictureInPicture = "Picture-in-picture"
    override val settings = "Player settings"
    override val latency = "Latency {0}s"
    override val latencyCompact = "{0}s"
    override val live = "LIVE"
    override val quality = "Quality"
    override val qualityAuto = "Auto"
    override val qualityAutoActive = "Auto · {0}"
    override val audioOnly = "Audio only"
    override val source = "Source"
    override val lowLatency = "Low latency"
    override val lowLatencyDescription = "Closer to live. Weak networks may buffer more often"
    override val video = "Video"
    override val scaleFit = "Fit"
    override val scaleFill = "Fill"
    override val landscapeChat = "Chat in landscape"
    override val chatSide = "Side"
    override val chatOverlay = "Overlay"
    override val chatHidden = "Off"
    override val overlayLock = "Lock chat in place"
    override val overlayUnlock = "Unlock to move and resize"
    override val chatBackground = "Overlay background"
    override val chatMessagesOpacity = "Message opacity"
    override val autoPictureInPicture = "Picture-in-picture"
    override val autoPictureInPictureDescription = "Keep watching in a small window when you leave the app"
    override val videoStats = "Video stats"
    override val offline = "{0} is offline"
    override val otherChannelNotice = "Chat: #{0} · Stream: #{1}"
    override val retry = "Retry"
    override val errorNetwork = "Connection problem"
    override val errorGeoblocked = "This stream is not available in your region"
    override val errorSubscribersOnly = "This stream is for subscribers only"
    override val errorCodec = "Your device can't decode this quality"
    override val errorForbidden = "Playback is restricted"
    override val errorNotFound = "Channel not found"
    override val errorUnknown = "Couldn't start playback"
    override val statDownloadResolution = "Download Resolution"
    override val statRenderResolution = "Render Resolution"
    override val statViewportResolution = "Viewport Resolution"
    override val statDownloadBitrate = "Download Bitrate"
    override val statBandwidth = "Bandwidth Estimate"
    override val statFps = "FPS"
    override val statSkippedFrames = "Skipped Frames"
    override val statBuffer = "Buffer Size"
    override val statLatency = "Latency To Broadcaster"
    override val statCodecs = "Codecs"
    override val statProtocol = "Protocol"
    override val statLatencyMode = "Latency Mode"
    override val statLatencyLow = "Low Latency"
    override val statLatencyNormal = "Normal Latency"
    override val statRenderSurface = "Render Surface"
    override val statBackend = "Backend Version"
    override val statPlaySession = "Play Session ID"
    override val statServingId = "Serving ID"
    override val unitSeconds = "sec."
    override val unitKbps = "Kbps"
    override val decimalSeparator = '.'
}

object StreamPlayerStringsRu : StreamPlayerStrings {
    override val watch = "Смотреть стрим"
    override val close = "Закрыть плеер"
    override val play = "Смотреть"
    override val pause = "Пауза"
    override val mute = "Выключить звук"
    override val unmute = "Включить звук"
    override val fullscreen = "Полный экран"
    override val exitFullscreen = "Выйти из полноэкранного режима"
    override val pictureInPicture = "Картинка в картинке"
    override val settings = "Настройки плеера"
    override val latency = "Задержка {0} с"
    override val latencyCompact = "{0} с"
    override val live = "В ЭФИРЕ"
    override val quality = "Качество"
    override val qualityAuto = "Авто"
    override val qualityAutoActive = "Авто · {0}"
    override val audioOnly = "Только звук"
    override val source = "Исходное"
    override val lowLatency = "Низкая задержка"
    override val lowLatencyDescription = "Ближе к прямому эфиру. На слабой сети чаще подгружается"
    override val video = "Видео"
    override val scaleFit = "Вписать"
    override val scaleFill = "Заполнить"
    override val landscapeChat = "Чат в горизонтали"
    override val chatSide = "Сбоку"
    override val chatOverlay = "Поверх"
    override val chatHidden = "Выкл"
    override val overlayLock = "Закрепить чат"
    override val overlayUnlock = "Открепить, чтобы двигать"
    override val chatBackground = "Фон окна чата"
    override val chatMessagesOpacity = "Видимость сообщений"
    override val autoPictureInPicture = "Картинка в картинке"
    override val autoPictureInPictureDescription = "Продолжать просмотр в мини-окне, когда выходишь из приложения"
    override val videoStats = "Статистика видео"
    override val offline = "{0} сейчас не в эфире"
    override val otherChannelNotice = "Чат: #{0} · Стрим: #{1}"
    override val retry = "Повторить"
    override val errorNetwork = "Проблема с соединением"
    override val errorGeoblocked = "Стрим недоступен в вашем регионе"
    override val errorSubscribersOnly = "Стрим только для подписчиков"
    override val errorCodec = "Устройство не может воспроизвести это качество"
    override val errorForbidden = "Просмотр ограничен"
    override val errorNotFound = "Канал не найден"
    override val errorUnknown = "Не удалось запустить видео"
    override val statDownloadResolution = "Разрешение загрузки"
    override val statRenderResolution = "Разрешение рендера"
    override val statViewportResolution = "Разрешение окна"
    override val statDownloadBitrate = "Битрейт загрузки"
    override val statBandwidth = "Оценка канала"
    override val statFps = "FPS"
    override val statSkippedFrames = "Пропущено кадров"
    override val statBuffer = "Буфер"
    override val statLatency = "Задержка до стримера"
    override val statCodecs = "Кодеки"
    override val statProtocol = "Протокол"
    override val statLatencyMode = "Режим задержки"
    override val statLatencyLow = "Низкая"
    override val statLatencyNormal = "Обычная"
    override val statRenderSurface = "Поверхность"
    override val statBackend = "Движок"
    override val statPlaySession = "ID сессии"
    override val statServingId = "Serving ID"
    override val unitSeconds = "с"
    override val unitKbps = "кбит/с"
    override val decimalSeparator = ','
}
