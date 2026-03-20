# Chatone - Twitch Chat Client

Современный кроссплатформенный клиент для Twitch чата, построенный на Kotlin Multiplatform и Compose Multiplatform.

## 🚀 Функции

- ✅ **OAuth 2.0 авторизация** - безопасная аутентификация через Twitch
- ✅ **Множественные аккаунты** - поддержка нескольких Twitch аккаунтов
- ✅ **Реальный чат в реальном времени** - WebSocket IRC соединение
- ✅ **Поиск каналов** - поиск стримеров через Twitch API
- ✅ **Отправка сообщений** - пишите в чат и видите сообщения других
- ✅ **Отображение бейджей** - модераторы, подписчики, VIP, стримеры
- ✅ **Цветные имена пользователей** - как в оригинальном Twitch
- ✅ **Material 3 дизайн** - современный UI с темной/светлой темой
- ✅ **Кроссплатформенность** - Android, Desktop (Windows, macOS, Linux), iOS

## 🏗️ Архитектура

### Tech Stack

- **Kotlin Multiplatform** - общий код для всех платформ
- **Compose Multiplatform** - декларативный UI
- **MVI Architecture** - однонаправленный поток данных
- **Ktor** - HTTP и WebSocket клиент
- **Koin** - dependency injection
- **SQLDelight** - кроссплатформенная база данных
- **Coroutines & Flow** - асинхронность

### Структура проекта

```
composeApp/src/
├── commonMain/          # Общий код
│   ├── kotlin/
│   │   └── io/rudione/chatone/
│   │       ├── base/            # BaseViewModel, MVI интерфейсы
│   │       ├── data/            # Слой данных
│   │       │   ├── local/       # SQLDelight схемы и драйверы
│   │       │   ├── remote/      # API клиенты (HTTP, WebSocket)
│   │       │   └── repository/  # Репозитории
│   │       ├── domain/          # Бизнес-логика
│   │       │   ├── model/       # Доменные модели
│   │       │   └── usecase/     # Use cases
│   │       ├── presentation/    # UI слой
│   │       │   ├── auth/        # Экран авторизации
│   │       │   ├── home/        # Главный экран
│   │       │   └── chat/        # Экран чата
│   │       ├── di/              # Dependency Injection
│   │       └── util/            # Утилиты
│   └── sqldelight/              # SQL схемы
├── androidMain/         # Android специфичный код
├── iosMain/            # iOS специфичный код
└── jvmMain/            # Desktop специфичный код
```

## 📋 Требования

- JDK 17+
- Android SDK (для Android)
- Xcode (для iOS)
- Аккаунт Twitch Developer

## 🔧 Настройка

### 1. Создайте Twitch Application

1. Перейдите на https://dev.twitch.tv/console/apps
2. Нажмите "Register Your Application"
3. Заполните данные:
   - **Name**: Chatone (или любое имя)
   - **OAuth Redirect URLs**: `http://localhost:8080/auth/callback`
   - **Category**: Chat Bot
4. Сохраните приложение и скопируйте:
   - **Client ID**
   - **Client Secret** (нажмите "New Secret")

### 2. Настройте конфигурацию

Откройте файл:
```
composeApp/src/commonMain/kotlin/io/rudione/chatone/util/AppConfig.kt
```

Замените значения:
```kotlin
const val TWITCH_CLIENT_ID = "ваш_client_id"
const val TWITCH_CLIENT_SECRET = "ваш_client_secret"
```

### 3. Сборка и запуск

#### Desktop (Windows, macOS, Linux)

```bash
./gradlew :composeApp:run
```

#### Android

```bash
./gradlew :composeApp:assembleDebug
# или откройте проект в Android Studio
```

#### iOS

```bash
./gradlew :composeApp:iosSimulatorArm64Test
# или откройте iosApp в Xcode
```

## 🎯 Использование

### 1. Авторизация

1. Запустите приложение
2. Нажмите "Login with Twitch"
3. Авторизуйтесь в браузере
4. Вернитесь в приложение (автоматически или вручную)

### 2. Добавление каналов

1. В главном экране введите имя канала в поиск
2. Нажмите на результат поиска
3. Канал откроется в режиме чата

### 3. Чат

- Просматривайте сообщения в реальном времени
- Пишите сообщения в текстовое поле внизу
- Отправляйте нажатием Enter или кнопки отправки

### 4. Множественные аккаунты

- Добавьте несколько аккаунтов через кнопку "+"
- Переключайтесь между аккаунтами в главном экране
- Каждый аккаунт подключается к IRC независимо

## 🔐 OAuth Scopes

Приложение запрашивает следующие разрешения:

- `chat:read` - чтение сообщений чата
- `chat:edit` - отправка сообщений
- `user:read:email` - получение информации о пользователе
- `channel:read:subscriptions` - чтение подписок
- `moderator:read:followers` - чтение фолловеров
- `moderator:manage:banned_users` - управление банами (для модераторов)
- `moderator:manage:chat_messages` - управление сообщениями (для модераторов)

## 🗄️ База данных

SQLDelight схемы:

- **TwitchAccount** - хранение аккаунтов и токенов
- **Channel** - сохраненные каналы и их информация
- **Message** - история сообщений чата

## 🌐 API Endpoints

### Twitch Helix API

- `GET /users` - информация о пользователях
- `GET /search/channels` - поиск каналов
- `GET /streams` - информация о стримах
- `GET /chat/settings` - настройки чата

### Twitch IRC WebSocket

- `wss://irc-ws.chat.twitch.tv:443` - IRC чат

### OAuth

- `POST /oauth2/token` - получение/обновление токенов
- `GET /oauth2/validate` - валидация токена
- `POST /oauth2/revoke` - отзыв токена

## 🎨 Дизайн

Приложение использует Material 3 Design с кастомной Twitch цветовой схемой:

- **Primary**: Twitch Purple (#9146FF)
- **Secondary**: Twitch Green (#00F593)
- **Темная тема** по умолчанию
- **Светлая тема** (переключение в настройках)

## 🐛 Troubleshooting

### Ошибка "Failed to authenticate"

- Проверьте Client ID и Client Secret
- Убедитесь, что Redirect URI совпадает
- Проверьте интернет соединение

### WebSocket не подключается

- Проверьте токен доступа
- Убедитесь в правильности scopes
- Проверьте firewall/прокси настройки

### База данных не создается

- Проверьте права доступа к файловой системе
- На Desktop: проверьте `~/.chatone/`
- На Android: проверьте app permissions

## 📝 Roadmap

- [ ] Эмоуты (Twitch, BTTV, FFZ, 7TV)
- [ ] Упоминания и хайлайты
- [ ] Модерация (тайм-ауты, баны)
- [ ] Папки каналов
- [ ] Экспорт/импорт настроек
- [ ] Уведомления
- [ ] Кастомные макросы
- [ ] Statist ics и аналитика

## 🤝 Contributing

Pull requests приветствуются! Для больших изменений сначала откройте issue для обсуждения.

## 📄 License

MIT License - см. LICENSE файл

## 👥 Авторы

- Разработано с ❤️ на Kotlin Multiplatform
- Twitch API integration
- IRC WebSocket chat

## 🔗 Ссылки

- [Twitch API Documentation](https://dev.twitch.tv/docs/api/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor](https://ktor.io/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)

---

**Примечание**: Это демонстрационное приложение. Для production использования рекомендуется добавить:
- Обработку ошибок
- Тесты (Unit, Integration, UI)
- CI/CD pipeline
- Crash reporting
- Analytics
- Rate limiting
- Token encryption
- И другие production-ready функции
# Chatone
