# Chatone - Twitch Chat Client

A modern cross-platform Twitch chat client built with Kotlin Multiplatform and Compose Multiplatform — running on **Windows, macOS, Linux, Android and iOS** from a single codebase, with a Material 3 Expressive interface.

## ✨ Features

### 💬 Chat & messaging
- Real-time Twitch chat with multi-channel support
- **Multi-chat panels** — view several channels side by side with drag & drop layout and cross-channel mentions
- Reply threads and message pinning (for moderators)
- Recent message history loaded when you join a channel
- Inline image previews (on / off / blur) and link hover previews
- Slash-command suggestions, custom **chat commands** and **macros** (with pinned quick-macros)
- Repeated-message counter
- **Message translation** — right-click / long-press any message to translate it into your language (no API key required)

### 😀 Emotes & cosmetics
- **7TV, BTTV and FFZ** emotes, including animated emotes
- 7TV cosmetics: badges & paints, with live 7TV emote-set updates
- Emote picker with search and tooltips

### 🛡️ Moderation
- Mod actions: timeout (presets), ban / unban, delete, mod / unmod, VIP / un-VIP
- **Customizable, reorderable mod-action buttons**
- Moderation panel for live chat settings
- Built-in **Automod**:
  - Word filters (with alternates, regex, whole-word, case-sensitivity)
  - Behavioral rules: spam rate, all-caps, links, emote spam, new accounts, duplicate messages, consecutive numbers
  - Event triggers: stream online/offline greeting, first-message greeting, raid welcome
  - Global or per-channel scope, exemptions for mods/VIPs/subs
  - Import / export as JSON, CSV or XLSX

### 👤 User mini-profile
- User card with avatar, badges, roles, account age and follow date
- Per-user notes and mention muting (globally or per channel)
- Message history shown Chatterino-style: timestamp + colored nickname + message
- Compact, borderless quick actions (whisper, block, ban/unban, mod, VIP)

### 🎨 Customization & themes
- Material 3 Expressive UI with glassmorphism / liquid-glass surfaces
- Custom theme creator (seed colour → full scheme) and accent palettes
- **Fully configurable interface colours** — every chat-highlight, moderation and automod colour can be edited
- Wallpaper backgrounds with blur, per-panel colours and glow effects
- Custom fonts (load your own), italic / underline / strikethrough, font and UI scaling
- Adjustable message spacing / density, light & dark, configurable title bar

### 👥 Accounts & connectivity
- Multiple accounts with independent connections and quick switching
- Per-account settings profiles
- Proxy support

### 🔔 Highlights & notifications
- Highlight rules (username, keywords, subscriptions, whispers, first message, channel points)
- Custom mention sound, a mentions feed and a whispers panel

### 🖥️ Platform & extras
- Cross-platform: Windows, macOS, Linux, Android, iOS
- Localization: English & Russian
- Detached windows (profile, settings, automod) on desktop
- Image uploader (drag & drop to a custom host)
- Polls & predictions banners, channel-point redemptions
- Blocked-users management

## ⬇️ Download

[![Latest Release](https://img.shields.io/github/v/release/Rudione/Chatone?label=latest&style=for-the-badge)](https://github.com/Rudione/Chatone/releases/latest)

Pick the file for your platform from the [latest release](https://github.com/Rudione/Chatone/releases/latest):

| Platform | File to download |
|----------|------------------|
| 🪟 **Windows** | `Chatone-<version>.msi` |
| 🍎 **macOS** | `Chatone-<version>.dmg` |
| 🐧 **Linux (Debian/Ubuntu)** | `chatone_<version>-1_amd64.deb` |
| 📦 **Portable (any OS)** | `Chatone-<version>-portable.zip` |

> 👉 All downloads live on the [Releases page](https://github.com/Rudione/Chatone/releases/latest). The **Actions** tab is for CI artifacts and requires a GitHub account — always use Releases for installers.

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
