import XCTest

final class ChatoneSmokeUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
    }

    func testGuestOpensLiveStreamFromBrowse() throws {
        capture("01-launch", after: 8)
        try tap(label: "Guest", timeout: 40)
        capture("02-home", after: 4)
        try tap(label: "Browse", timeout: 20)
        capture("03-browse", after: 8)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.32)).tap()
        capture("04-player-start", after: 6)
        capture("05-player-live", after: 20)
        XCUIDevice.shared.orientation = .landscapeLeft
        capture("06-player-landscape", after: 6)
        XCUIDevice.shared.orientation = .portrait
        capture("07-chat", after: 4)
    }

    private func tap(label: String, timeout: TimeInterval) throws {
        let element = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
        guard element.waitForExistence(timeout: timeout) else {
            capture("missing-\(label)", after: 0)
            XCTFail("No element labelled \(label). Tree: \(app.debugDescription)")
            return
        }
        element.tap()
    }

    private func capture(_ name: String, after seconds: TimeInterval) {
        if seconds > 0 { Thread.sleep(forTimeInterval: seconds) }
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
