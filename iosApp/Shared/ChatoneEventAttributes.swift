import ActivityKit
import Foundation

@available(iOS 16.2, *)
struct ChatoneEventAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var title: String
        var phase: String
        var deadline: Date?
        var options: [String]
        var totals: [Int]
        var updatedAt: Date
    }
    let eventKey: String
    let channel: String
    let kind: String
}
