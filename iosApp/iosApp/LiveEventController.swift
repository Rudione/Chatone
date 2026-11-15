import ActivityKit
import Foundation
import UIKit
import ComposeApp

private struct LiveEventPayload: Decodable {
    let id: String
    let kind: String
    let channel: String
    let title: String
    let phase: String
    let deadlineMs: Double?
    let options: [String]
    let totals: [Int]
    let updatedAtMs: Double
    var key: String { "\(kind):\(channel):\(id)" }
}

private struct LiveEventCommand: Decodable {
    let action: String
    let event: LiveEventPayload?
}

@MainActor
final class LiveEventController {
    static let shared = LiveEventController()
    private var pending: Task<Void, Never>?
    private var installed = false

    func install() {
        guard !installed else { return }
        installed = true
        refreshAvailability()
        IosLiveEventBridge.shared.onCommand = { [weak self] json in
            self?.receive(json)
        }
    }

    private func receive(_ json: String) {
        guard let data = json.data(using: .utf8),
              let command = try? JSONDecoder().decode(LiveEventCommand.self, from: data) else { return }
        let previous = pending
        pending = Task {
            await previous?.value
            if #available(iOS 16.2, *) { await process(command) }
        }
    }

    func refreshAvailability() {
        if #available(iOS 16.2, *) {
            IosLiveEventBridge.shared.setAvailable(available: ActivityAuthorizationInfo().areActivitiesEnabled)
        } else {
            IosLiveEventBridge.shared.setAvailable(available: false)
        }
    }

    func open(_ url: URL) {
        guard url.scheme == "chatone", url.host == "channel", url.pathComponents.count == 2 else { return }
        IosLiveEventBridge.shared.openChannel(login: url.lastPathComponent)
    }

    @available(iOS 16.2, *)
    private func process(_ command: LiveEventCommand) async {
        let activities = Activity<ChatoneEventAttributes>.activities
        guard command.action != "end" else {
            for activity in activities { await activity.end(nil, dismissalPolicy: .immediate) }
            return
        }
        guard let event = command.event, event.channel.range(of: "^[a-z0-9_]{1,25}$", options: .regularExpression) != nil else {
            IosLiveEventBridge.shared.reportFailure()
            return
        }
        let state = ChatoneEventAttributes.ContentState(
            title: String(event.title.prefix(140)), phase: event.phase,
            deadline: event.deadlineMs.map { Date(timeIntervalSince1970: $0 / 1000) },
            options: event.options.prefix(5).map { String($0.prefix(60)) },
            totals: Array(event.totals.prefix(5)), updatedAt: Date(timeIntervalSince1970: event.updatedAtMs / 1000)
        )
        let content = ActivityContent(state: state, staleDate: state.updatedAt.addingTimeInterval(90))
        let current = activities.first { $0.attributes.eventKey == event.key }
        if command.action == "start" {
            guard UIApplication.shared.applicationState == .active,
                  ActivityAuthorizationInfo().areActivitiesEnabled else {
                IosLiveEventBridge.shared.reportFailure()
                return
            }
            for activity in activities { await activity.end(nil, dismissalPolicy: .immediate) }
            do {
                let attributes = ChatoneEventAttributes(eventKey: event.key, channel: event.channel, kind: event.kind)
                let activity = try Activity.request(attributes: attributes, content: content, pushType: nil)
                watchDismissal(activity)
            } catch {
                IosLiveEventBridge.shared.reportFailure()
            }
        } else if let current {
            if command.action == "finish" {
                await current.end(ActivityContent(state: state, staleDate: nil), dismissalPolicy: .after(Date().addingTimeInterval(120)))
            } else {
                await current.update(content)
                if command.action == "restore" { watchDismissal(current) }
            }
        } else {
            IosLiveEventBridge.shared.dismissed(eventKey: event.key)
        }
    }

    @available(iOS 16.2, *)
    private func watchDismissal(_ activity: Activity<ChatoneEventAttributes>) {
        Task {
            for await state in activity.activityStateUpdates {
                if state == .dismissed {
                    IosLiveEventBridge.shared.dismissed(eventKey: activity.attributes.eventKey)
                    break
                }
                if state == .ended { break }
            }
        }
    }
}
