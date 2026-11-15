import AVFoundation
import ImageIO
import MediaPlayer
import UIKit
import ComposeApp

@MainActor
final class NowPlayingController {
    static let shared = NowPlayingController()
    private var details: [String: Any] = [:]
    private var artworkURL: String?
    private var artworkTask: URLSessionDataTask?
    private var artworkGeneration = UUID()
    private var playing = false
    private var commandTargets: [(MPRemoteCommand, Any)] = []
    private let images = NSCache<NSString, UIImage>()

    func install() {
        guard commandTargets.isEmpty else { return }
        images.countLimit = 8
        images.totalCostLimit = 4 * 1024 * 1024
        let bridge = IosNowPlayingBridge.shared
        bridge.onMetadata = { [weak self] title, channel, artwork in
            self?.show(title: title, channel: channel, artwork: artwork)
        }
        bridge.onPlayback = { [weak self] value in
            self?.playing = value.boolValue
            self?.publish()
        }
        bridge.onClear = { [weak self] in self?.clear() }
        let center = MPRemoteCommandCenter.shared()
        add(center.playCommand) { bridge.play() }
        add(center.pauseCommand) { bridge.pause() }
        add(center.togglePlayPauseCommand) { bridge.toggle() }
        [center.nextTrackCommand, center.previousTrackCommand, center.skipForwardCommand,
         center.skipBackwardCommand, center.seekForwardCommand, center.seekBackwardCommand,
         center.changePlaybackPositionCommand].forEach { $0.isEnabled = false }
        setCommandsEnabled(false)
    }

    private func add(_ command: MPRemoteCommand, action: @escaping () -> Void) {
        let target = command.addTarget { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, !self.details.isEmpty else { return }
                action()
            }
            return .success
        }
        commandTargets.append((command, target))
    }

    private func show(title: String, channel: String, artwork: String?) {
        let previousArtwork = details[MPMediaItemPropertyArtwork]
        details = [MPMediaItemPropertyTitle: title, MPMediaItemPropertyArtist: channel,
                   MPNowPlayingInfoPropertyIsLiveStream: true]
        setCommandsEnabled(true)
        if artwork == artworkURL {
            details[MPMediaItemPropertyArtwork] = previousArtwork
            publish()
            return
        }
        artworkURL = artwork
        artworkGeneration = UUID()
        let generation = artworkGeneration
        artworkTask?.cancel()
        artworkTask = nil
        publish()
        guard let artwork, let url = URL(string: artwork), url.scheme == "https", url.user == nil else { return }
        if let image = images.object(forKey: artwork as NSString) {
            setArtwork(image)
            return
        }
        var request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad, timeoutInterval: 12)
        request.httpShouldHandleCookies = false
        artworkTask = URLSession.shared.dataTask(with: request) { [weak self] data, response, _ in
            guard let data, data.count <= 2 * 1024 * 1024,
                  let response = response as? HTTPURLResponse, (200...299).contains(response.statusCode),
                  let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceThumbnailMaxPixelSize: 256,
                    kCGImageSourceCreateThumbnailWithTransform: true
                  ] as CFDictionary) else { return }
            let image = UIImage(cgImage: thumbnail)
            DispatchQueue.main.async {
                guard let self, self.artworkGeneration == generation else { return }
                self.images.setObject(image, forKey: artwork as NSString, cost: thumbnail.bytesPerRow * thumbnail.height)
                self.setArtwork(image)
                self.artworkTask = nil
            }
        }
        artworkTask?.resume()
    }

    private func setArtwork(_ image: UIImage) {
        details[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        publish()
    }

    private func publish() {
        guard !details.isEmpty else { return }
        var info = details
        info[MPNowPlayingInfoPropertyPlaybackRate] = playing ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
        MPNowPlayingInfoCenter.default().playbackState = playing ? .playing : .paused
    }

    private func setCommandsEnabled(_ enabled: Bool) {
        commandTargets.forEach { $0.0.isEnabled = enabled }
    }

    private func clear() {
        artworkGeneration = UUID()
        artworkTask?.cancel()
        artworkTask = nil
        artworkURL = nil
        details = [:]
        setCommandsEnabled(false)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        MPNowPlayingInfoCenter.default().playbackState = .stopped
    }
}
