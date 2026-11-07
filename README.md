<div align="center">

<img src="composeApp/src/commonMain/composeResources/drawable/icon.png" width="104" alt="Chatone">

# Chatone

A smart chat client for Twitch — chat, streams and moderation in one app.

[![Latest release](https://img.shields.io/github/v/release/Rudione/Chatone?label=download&color=7c3aed)](https://github.com/Rudione/Chatone/releases/latest)
[![Platforms](https://img.shields.io/badge/Windows%20%7C%20macOS%20%7C%20Linux-ready-2b7fff)](https://app.chatone.im/#download)
[![Android and iOS](https://img.shields.io/badge/Android%20%7C%20iOS-soon-555)](https://app.chatone.im/#download)

**[Website](https://app.chatone.im/)** · [Features](https://app.chatone.im/features) · [What’s new](https://app.chatone.im/changelog) · [Telegram](https://t.me/rudionee)

<img src="composeApp/src/commonMain/composeResources/drawable/icon.png" width="220" alt="Chatone logo">

</div>

## Features

- Several chats side by side, folders for channels, all mentions in one feed
- 7TV, BTTV and FFZ emotes, 7TV paints and badges
- Moderation that keeps up: hover buttons, a mod panel with macros, the AutoMod queue right in chat
- Your own automod: rules for caps, links, spam and new accounts, checked on your device
- Viewer dossier: account age, past names, every message and roles on other channels
- A low-delay player with a glow around the video and chat beside or over the stream
- Screen chat that floats over a game or a browser on desktop
- Image and link previews, translation, GIFs, polls and predictions without leaving chat
- Themes, wallpapers, fonts and sizes — make it yours
- An AI assistant that sums up chat and only ever suggests

## Download

Get the latest build on the [website](https://app.chatone.im/#download) or from [Releases](https://github.com/Rudione/Chatone/releases/latest): Windows, macOS and Linux. Android and iOS are on the way.

## Build from source

You need JDK 17+, plus the Android SDK for the Android app.

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageReleaseDistributionForCurrentOS
./gradlew :androidApp:assembleRelease
```

## Feedback

Bugs and ideas go to [Issues](https://github.com/Rudione/Chatone/issues) or [Telegram](https://t.me/rudionee).

## License

MIT. Chatone is an independent client and is not affiliated with Twitch Interactive.
