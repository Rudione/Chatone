import AVKit
import AmazonIVSPlayer
import ComposeApp
import UIKit

final class IvsLowLatencyPlayerProvider: NSObject, LowLatencyPlayerProvider {
    func createPlayer() -> LowLatencyPlayer {
        IvsLowLatencyPlayer()
    }
}

final class IvsLowLatencyPlayer: NSObject, LowLatencyPlayer, IVSPlayer.Delegate {
    private let player = IVSPlayer()
    private let playerView = IVSPlayerView()
    private var listener: LowLatencyPlayerListener?
    private var pictureInPicture: AVPictureInPictureController?
    private var decodeFailures: NSObjectProtocol?
    private var failedFrames: Int64 = 0

    override init() {
        super.init()
        player.delegate = self
        player.setNetworkRecoveryMode(.resume)
        playerView.backgroundColor = .black
        playerView.isUserInteractionEnabled = false
        playerView.videoGravity = .resizeAspect
        playerView.player = player
        decodeFailures = NotificationCenter.default.addObserver(
            forName: .AVSampleBufferDisplayLayerFailedToDecode,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.countFailedFrame(in: notification.object as? CALayer)
        }
    }

    var view: UIView { playerView }

    var version: String { player.version }

    func setListener(listener: LowLatencyPlayerListener?) {
        self.listener = listener
    }

    func load(url: String, lowLatency: Bool) {
        guard let source = URL(string: url) else { return }
        player.setLiveLowLatencyEnabled(lowLatency)
        player.setRebufferToLive(lowLatency)
        failedFrames = 0
        player.load(source)
    }

    func play() {
        player.play()
    }

    func pause() {
        player.pause()
    }

    func setMuted(muted: Bool) {
        player.muted = muted
    }

    func setPlaybackRate(rate: Float) {
        player.playbackRate = rate
    }

    func selectQuality(name: String?, seamless: Bool) {
        guard let name, let quality = player.qualities.first(where: { $0.name == name }) else {
            if !player.autoQualityMode { player.autoQualityMode = true }
            return
        }
        if !player.autoQualityMode, let current = player.quality, current.isEqual(to: quality) { return }
        player.setQuality(quality, adaptive: seamless)
    }

    func limitVideoSize(width: Int32, height: Int32) {
        guard width > 0, height > 0 else { return }
        player.setMaxVideoSize(CGSize(width: CGFloat(width), height: CGFloat(height)))
    }

    func stats() -> LowLatencyPlayerStats {
        let quality = player.quality
        let size = player.videoSize
        return LowLatencyPlayerStats(
            state: Self.state(player.state),
            latencyMs: Self.milliseconds(player.liveLatency),
            bufferedMs: max(0, Self.milliseconds(player.buffered) - Self.milliseconds(player.position)),
            videoWidth: Int32(size.width),
            videoHeight: Int32(size.height),
            videoBitrate: Int64(player.videoBitrate),
            bandwidthEstimate: Int64(player.bandwidthEstimate),
            decodedFrames: Int64(player.videoFramesDecoded),
            droppedFrames: Int64(player.videoFramesDropped),
            failedFrames: failedFrames,
            qualityName: quality?.name,
            codecs: quality?.codecs,
            lowLatency: player.isLiveLowLatency
        )
    }

    func pictureInPictureController() -> AVPictureInPictureController? {
        if let pictureInPicture { return pictureInPicture }
        let created = AVPictureInPictureController(ivsPlayerLayer: playerView.playerLayer)
        pictureInPicture = created
        return created
    }

    func sampleFrame(width: Int32, height: Int32) -> KotlinIntArray? {
        guard width > 0, height > 0, let frame = playerView.playerLayer.displayedPixelBuffer() else { return nil }
        return PixelBufferSampler.sample(frame, width: Int(width), height: Int(height))
    }

    func close() {
        if let decodeFailures { NotificationCenter.default.removeObserver(decodeFailures) }
        decodeFailures = nil
        pictureInPicture?.stopPictureInPicture()
        pictureInPicture = nil
        listener = nil
        player.delegate = nil
        player.pause()
        player.load(nil)
        playerView.removeFromSuperview()
    }

    func player(_ player: IVSPlayer, didChangeState state: IVSPlayer.State) {
        listener?.onStateChanged(state: Self.state(state))
    }

    func player(_ player: IVSPlayer, didFailWithError error: Error) {
        let details = error as NSError
        listener?.onError(
            error: LowLatencyPlayerError(
                status: Int32(clamping: details.code),
                reason: details.userInfo[IVSResultDescriptionErrorKey] as? String ?? "",
                source: details.userInfo[IVSSourceDescriptionErrorKey] as? String ?? ""
            )
        )
    }

    func player(_ player: IVSPlayer, didChangeQuality quality: IVSQuality?) {
        listener?.onPlaybackChanged()
    }

    func player(_ player: IVSPlayer, didChangeVideoSize videoSize: CGSize) {
        listener?.onPlaybackChanged()
    }

    func playerWillRebuffer(_ player: IVSPlayer) {
        listener?.onRebuffer()
    }

    func playerNetworkDidBecomeUnavailable(_ player: IVSPlayer) {
        listener?.onNetworkUnavailable()
    }

    private func countFailedFrame(in layer: CALayer?) {
        var current = layer
        while let candidate = current {
            if candidate === playerView.playerLayer {
                failedFrames += 1
                return
            }
            current = candidate.superlayer
        }
    }

    private static func state(_ state: IVSPlayer.State) -> LowLatencyPlayerState {
        switch state {
        case .ready: return .ready
        case .buffering: return .buffering
        case .playing: return .playing
        case .ended: return .ended
        default: return .idle
        }
    }

    private static func milliseconds(_ time: CMTime) -> Int64 {
        let seconds = CMTimeGetSeconds(time)
        return seconds.isFinite ? Int64(seconds * 1000) : 0
    }
}
