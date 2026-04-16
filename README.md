# Chatone - Twitch Chat Client

A modern cross-platform Twitch chat client built with Kotlin Multiplatform and Compose Multiplatform.

## 🎯 How to Use

### 1. Login
1. Launch the app
2. Tap **"Login with Twitch"**
3. Authorize in your browser
4. Return to the app (automatic or manual)

### 2. Add Channels
1. On the home screen, type a channel name in the search bar
2. Tap a result to open its chat

### 3. Chat
- Watch messages appear in real time
- Type in the text field at the bottom
- Send with **Enter** or the send button

### 4. Manage Accounts
- Add multiple accounts via the **+** button
- Switch between accounts on the home screen
- Each account connects independently to Twitch chat

## 🔐 Permissions

Chatone requests only the permissions it needs:

| Scope | Purpose |
|-------|---------|
| `chat:read` | Read chat messages |
| `chat:edit` | Send messages |
| `user:read:email` | Load your profile info |
| `channel:read:subscriptions` | Show subscription status |
| `moderator:*` | Moderate chat (if you're a mod) |

Your tokens are stored securely and never shared.

## 🗄️ Data Storage

Chatone locally stores:
- Your connected accounts (tokens encrypted)
- Saved favorite channels
- Optional chat history (configurable)

All data stays on your device.

## 🤝 Contributing

Found a bug or have an idea? Open an issue!  
Pull requests are welcome — please discuss major changes first.

## 📄 License

MIT License — see the [LICENSE](LICENSE) file for details.

> **Note**: Chatone is a community-built client. For the best experience, keep the app updated and report any issues via GitHub. Not affiliated with Twitch Interactive.

**Chatone** - Chat smarter. Stay connected. 🎮💬
