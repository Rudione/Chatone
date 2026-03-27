# Chatone - Architecture Documentation

## 📐 Architectural Overview

Chatone follows Clean Architecture principles with MVI (Model-View-Intent) pattern for state management.

### Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (ViewModels, UI Screens, Components)  │
├─────────────────────────────────────────┤
│          Domain Layer                   │
│     (Use Cases, Models, Interfaces)     │
├─────────────────────────────────────────┤
│           Data Layer                    │
│  (Repositories, API Clients, Database)  │
└─────────────────────────────────────────┘
```

## 🏗️ Project Structure

### Module Organization

```
Chatone/
├── composeApp/                  # Main application module
│   ├── src/
│   │   ├── commonMain/          # Shared code for all platforms
│   │   │   ├── kotlin/
│   │   │   │   └── io/rudione/chatone/
│   │   │   │       ├── base/            # Base classes (BaseViewModel)
│   │   │   │       ├── data/            # Data layer
│   │   │   │       │   ├── local/       # SQLDelight database
│   │   │   │       │   ├── remote/      # API clients
│   │   │   │       │   └── repository/  # Repository implementations
│   │   │   │       ├── domain/          # Domain layer
│   │   │   │       │   ├── model/       # Domain models
│   │   │   │       │   └── usecase/     # Use cases
│   │   │   │       ├── presentation/    # Presentation layer
│   │   │   │       │   ├── auth/        # Authentication screen
│   │   │   │       │   ├── home/        # Home screen
│   │   │   │       │   ├── chat/        # Chat screen
│   │   │   │       │   └── components/  # Reusable UI components
│   │   │   │       ├── di/              # Dependency Injection (Koin)
│   │   │   │       └── util/            # Utilities and helpers
│   │   │   └── sqldelight/              # SQL schema definitions
│   │   ├── androidMain/         # Android-specific code
│   │   ├── iosMain/            # iOS-specific code
│   │   └── jvmMain/            # Desktop-specific code
│   └── build.gradle.kts
└── gradle/                     # Gradle wrapper
```

## 🔄 MVI Pattern Implementation

### State Management Flow

```
User Action → Event → ViewModel → State Update → UI Recomposition
                 ↓
            Side Effects → Effect Handler
```

### BaseViewModel

All ViewModels extend `BaseViewModel<State, Event, Effect>`:

```kotlin
abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UIEffect>
```

**Components:**
- **State**: Immutable data class representing UI state
- **Event**: User actions and system events
- **Effect**: One-time side effects (navigation, toasts, etc.)

### Example: ChatViewModel

```kotlin
// State
data class ChatState(
    val channelLogin: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val isConnected: Boolean = false
) : UiState

// Events
sealed class ChatEvent : UiEvent {
    data class OnInit(val channelLogin: String) : ChatEvent()
    data class OnMessageInputChanged(val input: String) : ChatEvent()
    object OnSendMessage : ChatEvent()
}

// Effects
sealed class ChatEffect : UIEffect {
    data class ShowError(val message: String) : ChatEffect()
    object ScrollToBottom : ChatEffect()
}
```

## 📊 Data Flow

### Authentication Flow

```
1. User clicks "Login with Twitch"
   ↓
2. AuthViewModel.startAuthentication()
   ↓
3. Open browser with OAuth URL
   ↓
4. User authorizes → Redirect with code
   ↓
5. AuthViewModel.handleAuthCode(code)
   ↓
6. AuthRepository.authenticate(code)
   ↓
7. TwitchApiClient.getAccessToken(code)
   ↓
8. Save TwitchAccount to database
   ↓
9. Navigate to Home screen
```

### Chat Connection Flow

```
1. HomeViewModel loads accounts
   ↓
2. Auto-connect first account
   ↓
3. ChatRepository.connect(account)
   ↓
4. TwitchIrcClient establishes WebSocket
   ↓
5. Authenticate with IRC (PASS, NICK)
   ↓
6. User joins channel
   ↓
7. Listen for PRIVMSG events
   ↓
8. Parse IRC messages → ChatMessage
   ↓
9. Emit to messages Flow
   ↓
10. ChatViewModel collects and updates state
   ↓
11. UI displays messages
```

### Message Sending Flow

```
1. User types message and presses Send
   ↓
2. ChatViewModel.sendEvent(OnSendMessage)
   ↓
3. ChatViewModel.sendMessage()
   ↓
4. SendMessageUseCase.invoke()
   ↓
5. ChatRepository.sendMessage()
   ↓
6. TwitchIrcClient.sendMessage()
   ↓
7. Send "PRIVMSG #channel :message" via WebSocket
   ↓
8. Twitch IRC server broadcasts to all clients
   ↓
9. Message appears in chat (from server)
```

## 🗄️ Database Schema

### SQLDelight Tables

#### TwitchAccountEntity
```sql
CREATE TABLE TwitchAccountEntity (
    userId TEXT NOT NULL PRIMARY KEY,
    login TEXT NOT NULL,
    displayName TEXT NOT NULL,
    profileImageUrl TEXT NOT NULL,
    accessToken TEXT NOT NULL,
    refreshToken TEXT NOT NULL,
    expiresAt INTEGER NOT NULL,
    scopes TEXT NOT NULL,  -- JSON array
    createdAt INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
```

#### ChannelEntity
```sql
CREATE TABLE ChannelEntity (
    id TEXT NOT NULL PRIMARY KEY,
    login TEXT NOT NULL,
    displayName TEXT NOT NULL,
    profileImageUrl TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    viewerCount INTEGER NOT NULL DEFAULT 0,
    isLive INTEGER NOT NULL DEFAULT 0,
    gameName TEXT NOT NULL DEFAULT '',
    title TEXT NOT NULL DEFAULT '',
    isFavorite INTEGER NOT NULL DEFAULT 0,
    lastJoinedAt INTEGER,
    createdAt INTEGER NOT NULL
);
```

#### MessageEntity
```sql
CREATE TABLE MessageEntity (
    id TEXT NOT NULL PRIMARY KEY,
    channelId TEXT NOT NULL,
    channelName TEXT NOT NULL,
    userId TEXT NOT NULL,
    username TEXT NOT NULL,
    displayName TEXT NOT NULL,
    message TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    color TEXT,
    badges TEXT NOT NULL DEFAULT '[]',  -- JSON array
    emotes TEXT NOT NULL DEFAULT '[]',  -- JSON array
    isModerator INTEGER NOT NULL DEFAULT 0,
    isSubscriber INTEGER NOT NULL DEFAULT 0,
    isVip INTEGER NOT NULL DEFAULT 0,
    isBroadcaster INTEGER NOT NULL DEFAULT 0,
    isMention INTEGER NOT NULL DEFAULT 0,
    isAction INTEGER NOT NULL DEFAULT 0
);
```

## 🌐 Network Layer

### Ktor HTTP Client Configuration

```kotlin
HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        })
    }
    install(Logging) {
        logger = NapierLogger
        level = LogLevel.INFO
    }
    install(WebSockets)
}
```

### API Endpoints

#### Twitch Helix API (REST)
- Base URL: `https://api.twitch.tv/helix`
- Authentication: Bearer token in header
- Client-ID required in header

**Endpoints:**
- `GET /users` - User information
- `GET /search/channels` - Search channels
- `GET /streams` - Stream information
- `GET /chat/settings` - Chat settings
- `PATCH /chat/settings` - Update chat settings (mod)
- `POST /moderation/bans` - Ban/timeout user
- `DELETE /moderation/bans` - Unban user
- `DELETE /moderation/chat` - Delete message
- `POST /moderation/moderators` - Add moderator
- `DELETE /moderation/moderators` - Remove moderator
- `POST /channels/vips` - Add VIP
- `DELETE /channels/vips` - Remove VIP
- `POST /whispers` - Send whisper

#### Twitch OAuth API
- Base URL: `https://id.twitch.tv/oauth2`

**Endpoints:**
- `POST /token` - Get/refresh access token
- `GET /validate` - Validate token
- `POST /revoke` - Revoke token

#### Twitch IRC WebSocket
- URL: `wss://irc-ws.chat.twitch.tv:443`
- Protocol: IRC (Internet Relay Chat)
- Authentication: OAuth token

**IRC Commands:**
```
CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership
PASS oauth:your_access_token
NICK your_username
JOIN #channel_name
PRIVMSG #channel :message_text
```

## 🔌 WebSocket Management

### Connection Strategy

```kotlin
class TwitchIrcClient {
    // Separate read/write connections (recommended by Twitch)
    private var session: WebSocketSession? = null
    
    // Auto-reconnect with exponential backoff
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 2000L
    
    // Keep-alive mechanism
    private fun startPingJob() {
        pingJob = scope.launch {
            while (isActive) {
                delay(60000) // Ping every 60 seconds
                session?.send(Frame.Text("PING"))
            }
        }
    }
}
```

### IRC Message Parsing

```kotlin
object IrcMessageParser {
    fun parseMessage(rawMessage: String): ChatMessage {
        // Example:
        // @badge-info=;badges=moderator/1;color=#FF0000;display-name=User
        // :user!user@user.tmi.twitch.tv PRIVMSG #channel :Hello!
        
        1. Extract tags (@key=value;...)
        2. Parse prefix (user!user@user.tmi.twitch.tv)
        3. Extract command (PRIVMSG)
        4. Extract channel and message
        5. Convert to ChatMessage model
    }
}
```

## 🎨 UI Components

### Material 3 Theme

```kotlin
// Twitch Purple color scheme
private val TwitchPurple = Color(0xFF9146FF)

private val DarkColorScheme = darkColorScheme(
    primary = TwitchPurple,
    surface = Color(0xFF18181B),
    background = Color(0xFF0E0E10)
)
```

### Screen Composition

```
App (Root)
├── AuthScreen
│   └── Login form + Twitch OAuth flow (WebView)
└── MainScreen
    ├── BoxWithConstraints (responsive: >=900dp persistent sidebar, else overlay)
    ├── ChannelSidebar (280dp)
    │   ├── Account indicator (avatar + connection status)
    │   ├── Channel Folders (collapsible, drag-target)
    │   │   └── ChannelItem (avatar, live dot, unread badge, context menu)
    │   ├── Unfoldered Channels
    │   └── Bottom actions (Add Channel, Create Folder, Settings)
    ├── MiniRail (compact horizontal, top-left overlay)
    │   └── Channel icons with red mention badges
    ├── ChannelTabBar (FlowRow, wraps to new lines)
    │   └── Tab chips with unread count badges
    ├── ChatScreen
    │   ├── ChatTopBar (channel name, room state chips, mod tools)
    │   ├── MessageList (LazyColumn)
    │   │   ├── PrivMsgItem (badges, paint, highlight color, animated emotes)
    │   │   ├── SystemMsg / UserNoticeMsg / ModerationMsg
    │   │   └── EmotePickerSheet (bottom sheet, animated)
    │   ├── EmoteAutocomplete (suggestions dropdown)
    │   ├── ModerationPanel (glassmorphism, chat modes, slow mode)
    │   ├── UserProfilePopup (badges, mod/vip actions)
    │   └── MessageInput (TextField + Send + Emote picker toggle)
    └── SettingsScreen (overlay)
        ├── Appearance (theme, font, emote size, navigation mode)
        ├── Chat (timestamps, badges, deleted messages, scrollback)
        ├── Notifications & Highlights (rules, sound, patterns)
        ├── Moderation (confirm actions, default timeout)
        └── About
```

### Platform-Specific Components (expect/actual)

```
NotificationSoundPlayer
├── Desktop: javax.sound.sampled (880Hz synth tone)
├── Android: ToneGenerator (TONE_PROP_BEEP)
└── iOS: AudioServicesPlaySystemSound(1007)

AnimatedEmoteImage
├── Desktop: Skia Codec frame decoding + LaunchedEffect animation
├── Android: Coil + coil-gif (AnimatedImageDecoder)
└── iOS: Static AsyncImage fallback

ImageLoaderFactory
├── Desktop: Default Coil ImageLoader
├── Android: Coil + AnimatedImageDecoder.Factory()
└── iOS: Default Coil ImageLoader
```

## 🧪 Testing Strategy

### Unit Tests
- ViewModels (state reducers)
- Use Cases (business logic)
- Repositories (data operations)
- Parsers (IRC message parsing)

### Integration Tests
- API client integration
- Database operations
- Repository implementations

### UI Tests
- Screen navigation
- User interactions
- State updates

## 🔐 Security Considerations

### Token Storage
- Platform-specific secure storage
- Android: EncryptedSharedPreferences (planned)
- iOS: Keychain (planned)
- Desktop: OS keyring (planned)

### Network Security
- HTTPS/WSS only
- Certificate pinning (planned)
- Rate limiting awareness

## 📱 Platform-Specific Implementation

### Android
- `DatabaseDriverFactory`: AndroidSqliteDriver
- UI: Compose for Android
- OAuth: Custom Tabs for browser

### Desktop (JVM)
- `DatabaseDriverFactory`: JdbcSqliteDriver
- UI: Compose for Desktop
- OAuth: System browser

### iOS
- `DatabaseDriverFactory`: NativeSqliteDriver
- UI: Compose for iOS
- OAuth: SafariServices (planned)

## 🚀 Performance Optimizations

### Message List
- LazyColumn for virtualization
- Message limit (500 per channel)
- Efficient recomposition with keys

### Image Loading
- Coil for async image loading
- Memory caching
- Disk caching

### Database
- Indexed queries
- Batch operations
- Background threading

## 📊 Monitoring & Logging

### Napier Logging
```kotlin
Napier.d("Debug message", tag = "TAG")
Napier.e("Error message", throwable, tag = "TAG")
Napier.i("Info message", tag = "TAG")
```

### Log Levels
- DEBUG: Detailed information
- INFO: General information
- ERROR: Error events

## 🔄 State Restoration

- ViewModel state survives configuration changes
- Database persists data across app restarts
- OAuth tokens saved securely

## 🌍 Internationalization (Future)

- String resources in `composeResources`
- Locale-aware formatting
- RTL support

---

This architecture provides:
✅ Clean separation of concerns
✅ Testable components
✅ Type-safe state management
✅ Platform-specific optimizations
✅ Scalable structure
✅ Maintainable codebase
