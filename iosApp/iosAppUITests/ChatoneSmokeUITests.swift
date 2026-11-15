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

    func testSettingsOpenAndSaveMentionPreferences() throws {
        try openGuestSettings()
        XCTAssertTrue(app.textViews.firstMatch.waitForExistence(timeout: 10), app.debugDescription)
        capture("settings-open", after: 1)
        try tap(label: "Notifications", timeout: 10)
        XCTAssertTrue(labelled("Notifications for mentions while Chatone is running. iOS can pause the chat in the background; delivery while the app is suspended or closed is currently unavailable.").waitForExistence(timeout: 5), app.debugDescription)
        let toggle = app.switches.firstMatch
        XCTAssertTrue(toggle.waitForExistence(timeout: 5), app.debugDescription)
        let original = toggle.value as? String
        toggle.tap()
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = springboard.buttons["Allow"]
        if allow.waitForExistence(timeout: 3) { allow.tap() }
        let changed = toggle.value as? String
        XCTAssertNotEqual(original, changed)
        capture("settings-notifications", after: 1)
        XCUIDevice.shared.press(.home)
        app.activate()
        XCTAssertEqual(changed, app.switches.firstMatch.value as? String)
        app.terminate()
        app.launch()
        try openGuestSettings()
        try tap(label: "Notifications", timeout: 10)
        let restored = app.switches.firstMatch
        XCTAssertTrue(restored.waitForExistence(timeout: 5), app.debugDescription)
        XCTAssertEqual(changed, restored.value as? String)
        restored.tap()
        capture("settings-reopened", after: 1)
    }

    func testNativeMessageContextMenuCopiesMessage() throws {
        let guest = labelled("Guest")
        if guest.waitForExistence(timeout: 10) { guest.tap() }
        let messages = app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "(?s).*\\d{2}:\\d{2}.*"))
        if !messages.firstMatch.waitForExistence(timeout: 5) {
            let menu = labelled("Menu")
            if menu.exists { menu.tap() }
            try tap(label: "Browse", timeout: 20)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.32)).tap()
        }
        XCTAssertTrue(messages.firstMatch.waitForExistence(timeout: 40), app.debugDescription)
        for message in messages.allElementsBoundByIndex where message.isHittable && message.frame.width > 100 {
            message.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.7)).press(forDuration: 1)
            let copy = labelled("Copy message")
            if copy.waitForExistence(timeout: 2) {
                let preview = app.descendants(matching: .any).matching(identifier: "chat_message_preview").firstMatch
                XCTAssertTrue(preview.waitForExistence(timeout: 5), "Native preview is missing")
                capture("native-message-menu", after: 0)
                copy.tap()
                XCTAssertTrue(copy.waitForNonExistence(timeout: 5))
                return
            }
        }
        XCTFail("Native message menu did not open: " + app.debugDescription)
    }

    private func openGuestSettings() throws {
        let guest = labelled("Guest")
        if guest.waitForExistence(timeout: 10) { guest.tap() }
        let sidebar = labelled("Open sidebar")
        if sidebar.waitForExistence(timeout: 2) { sidebar.tap() }
        else if labelled("Menu").exists { labelled("Menu").tap() }
        try tap(label: "Settings", timeout: 15)
    }

    private func labelled(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
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
