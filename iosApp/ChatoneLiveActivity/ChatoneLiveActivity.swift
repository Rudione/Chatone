import ActivityKit
import SwiftUI
import WidgetKit

@main
struct ChatoneWidgets: WidgetBundle {
    var body: some Widget { ChatoneLiveActivity() }
}

struct ChatoneLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: ChatoneEventAttributes.self) { context in
            EventCard(context: context)
                .padding(14)
                .activityBackgroundTint(Color(.secondarySystemBackground))
                .activitySystemActionForegroundColor(.primary)
                .widgetURL(channelURL(context.attributes.channel))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.attributes.channel, systemImage: eventSymbol(context.attributes.kind))
                        .font(.caption.weight(.semibold)).lineLimit(1)
                }
                DynamicIslandExpandedRegion(.trailing) { EventTimer(state: context.state) }
                DynamicIslandExpandedRegion(.bottom) { EventDetails(context: context) }
            } compactLeading: {
                Image(systemName: eventSymbol(context.attributes.kind)).foregroundStyle(.mint)
            } compactTrailing: {
                EventTimer(state: context.state).frame(maxWidth: 56)
            } minimal: {
                Image(systemName: eventSymbol(context.attributes.kind)).foregroundStyle(.mint)
            }
            .widgetURL(channelURL(context.attributes.channel))
            .keylineTint(.mint)
        }
    }
}

private func channelURL(_ channel: String) -> URL? { URL(string: "chatone://channel/\(channel)") }
private func eventSymbol(_ kind: String) -> String { kind == "prediction" ? "chart.bar.xaxis" : "checklist" }
private func isTerminal(_ phase: String) -> Bool {
    ["RESOLVED", "CANCELED", "COMPLETED", "TERMINATED", "ARCHIVED", "MODERATED", "INVALID"].contains(phase)
}

private struct EventCard: View {
    let context: ActivityViewContext<ChatoneEventAttributes>
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Label(context.attributes.channel, systemImage: eventSymbol(context.attributes.kind))
                    .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Spacer()
                EventTimer(state: context.state)
            }
            EventDetails(context: context)
        }
    }
}

private struct EventDetails: View {
    let context: ActivityViewContext<ChatoneEventAttributes>
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(context.state.title).font(.headline).lineLimit(2)
            HStack(spacing: 8) {
                ForEach(Array(context.state.options.prefix(2).enumerated()), id: \.offset) { index, option in
                    HStack(spacing: 4) {
                        Text(option).lineLimit(1)
                        Spacer(minLength: 0)
                        Text(index < context.state.totals.count ? context.state.totals[index].formatted(.number.notation(.compactName)) : "0")
                            .monospacedDigit().fontWeight(.semibold)
                    }
                    .font(.caption)
                    .padding(.horizontal, 9).padding(.vertical, 6)
                    .background((index == 0 ? Color.blue : Color.pink).opacity(0.18), in: RoundedRectangle(cornerRadius: 9))
                }
            }
            if context.isStale && !isTerminal(context.state.phase) {
                Text("Open chat for the latest result").font(.caption2).foregroundStyle(.secondary)
            } else if context.state.phase == "LOCKED" {
                Text("Entries closed · waiting for result").font(.caption2).foregroundStyle(.secondary)
            } else if isTerminal(context.state.phase) {
                Text(context.state.phase == "CANCELED" ? "Canceled" : "Finished").font(.caption2).foregroundStyle(.secondary)
            }
        }
    }
}

private struct EventTimer: View {
    let state: ChatoneEventAttributes.ContentState
    var body: some View {
        Group {
            if isTerminal(state.phase) {
                Image(systemName: "checkmark.circle")
            } else if state.phase == "LOCKED" {
                Image(systemName: "lock.fill")
            } else if let deadline = state.deadline, deadline > Date() {
                Text(timerInterval: Date()...deadline, countsDown: true).monospacedDigit()
            } else {
                Text("00:00").monospacedDigit()
            }
        }
        .font(.caption.weight(.semibold))
        .foregroundStyle(.mint)
        .fixedSize(horizontal: true, vertical: false)
    }
}
