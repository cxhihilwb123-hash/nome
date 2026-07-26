//
//  Tests_iOS.swift
//  Tests iOS
//
//  Created by Evgeny Poberezkin on 17/01/2022.
//

import XCTest

class Tests_iOS: XCTestCase {

    private struct DiagnosticIdentity {
        let sender: String
        let contact: String

        var remoteSender: String {
            sender == "main" ? "peer" : "main"
        }
    }

    private let diagnosticRunIDKey = "NOME_REAL_CORE_DIAGNOSTIC_RUN_ID"
    private let diagnosticRoundsKey = "NOME_REAL_CORE_DIAGNOSTIC_ROUNDS"
    private let callContactKey = "NOME_CALL_DIAGNOSTIC_CONTACT"
    private let callMediaKey = "NOME_CALL_DIAGNOSTIC_MEDIA"

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testExample() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()
        app.launch()

        // Use recording to get started writing UI tests.
        // Use XCTAssert and related functions to verify your tests produce the correct results.
    }

    func testSendRealCoreDiagnosticMessages() throws {
        let configuration = try diagnosticConfiguration()
        let app = diagnosticApplication(configuration: configuration)
        app.launch()
        skipQuiescenceWaits(in: app)

        let identity = try detectDiagnosticIdentity(in: app)
        try openDiagnosticContact(identity.contact, in: app)
        app.swipeUp()

        for round in 1...configuration.rounds {
            let message = diagnosticMessage(
                runID: configuration.runID,
                sender: identity.sender,
                round: round
            )
            try sendDiagnosticMessage(message, round: round, in: app)
        }
    }

    func testReceiveRealCoreDiagnosticMessages() throws {
        let configuration = try diagnosticConfiguration()
        let app = diagnosticApplication(configuration: configuration)
        app.launch()
        skipQuiescenceWaits(in: app)

        let identity = try detectDiagnosticIdentity(in: app)
        try openDiagnosticContact(identity.contact, in: app)
        app.swipeUp()

        for round in 1...configuration.rounds {
            let message = diagnosticMessage(
                runID: configuration.runID,
                sender: identity.remoteSender,
                round: round
            )
            print("[NOME_DIAG] remote-receive-wait sender=\(identity.remoteSender) round=\(round) message=\(message)")
            let startedAt = Date()
            let received = diagnosticMessageElement(message, in: app).waitForExistence(timeout: 45)
            let elapsed = Date().timeIntervalSince(startedAt)
            print("[NOME_DIAG] remote-receive sender=\(identity.remoteSender) round=\(round) elapsed=\(String(format: "%.3f", elapsed)) message=\(message)")
            if !received {
                attachDiagnosticScreenshot(app, name: "missing-remote-\(identity.remoteSender)-round-\(round)")
            }
            XCTAssertTrue(received, "Remote diagnostic message did not arrive: \(message)")
        }
    }

    func testStartNomeCallDiagnostic() throws {
        let environment = ProcessInfo.processInfo.environment
        guard let contact = environment[callContactKey], !contact.isEmpty,
              let media = environment[callMediaKey], ["audio", "video"].contains(media) else {
            throw XCTSkip("Set \(callContactKey) and \(callMediaKey)=audio|video to enable the call diagnostic")
        }

        let app = XCUIApplication()
        app.launchEnvironment[callContactKey] = contact
        app.launchEnvironment[callMediaKey] = media
        app.launchEnvironment["NOME_DISABLE_CALLKIT_FOR_TESTS"] = "1"
        app.launch()
        skipQuiescenceWaits(in: app)

        let contactLabel = app.staticTexts[contact].firstMatch
        XCTAssertTrue(contactLabel.waitForExistence(timeout: 30), "Call diagnostic contact is not visible: \(contact)")
        contactLabel.tap()
        XCTAssertTrue(app.textViews["chat-compose-editor"].firstMatch.waitForExistence(timeout: 20))

        let callMenu = app.buttons["chat-call-menu"].firstMatch
        XCTAssertTrue(callMenu.waitForExistence(timeout: 10), "Call menu is not accessible")
        callMenu.tap()

        let mediaButton = app.buttons[media == "audio" ? "chat-audio-call" : "chat-video-call"].firstMatch
        XCTAssertTrue(mediaButton.waitForExistence(timeout: 10), "Call media action is not accessible: \(media)")
        print("[NOME_CALL_DIAG] start media=\(media) contact=\(contact)")
        mediaButton.tap()

        // Keep the caller alive while the peer accepts and require the connected state.
        let connected = app.staticTexts["已连接"].firstMatch
        XCTAssertTrue(connected.waitForExistence(timeout: 40), "Call did not reach the connected state: \(media)")
        RunLoop.current.run(until: Date().addingTimeInterval(5))
        attachDiagnosticScreenshot(app, name: "call-\(media)-connected")
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }

    private func diagnosticConfiguration() throws -> (runID: String, rounds: Int) {
        let environment = ProcessInfo.processInfo.environment
        guard let rawRunID = environment[diagnosticRunIDKey], !rawRunID.isEmpty else {
            throw XCTSkip("Set \(diagnosticRunIDKey) to enable the real-core diagnostic UI tests")
        }
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-_"))
        let runID = rawRunID.unicodeScalars.filter(allowed.contains).map(String.init).joined()
        guard !runID.isEmpty else {
            XCTFail("\(diagnosticRunIDKey) must contain at least one safe character")
            throw DiagnosticFailure.invalidConfiguration
        }

        let rounds = environment[diagnosticRoundsKey].flatMap(Int.init) ?? 1
        guard (1...20).contains(rounds) else {
            XCTFail("\(diagnosticRoundsKey) must be between 1 and 20")
            throw DiagnosticFailure.invalidConfiguration
        }
        return (runID, rounds)
    }

    private func diagnosticApplication(configuration: (runID: String, rounds: Int)) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment[diagnosticRunIDKey] = configuration.runID
        app.launchEnvironment[diagnosticRoundsKey] = String(configuration.rounds)

        skipQuiescenceWaits(in: app)
        return app
    }

    private func skipQuiescenceWaits(in app: XCUIApplication) {
        // SimpleX has intentional, continuously updating SwiftUI animations. XCUITest otherwise
        // waits up to 60 seconds before and after every interaction even though the UI is usable.
        // These XCTest-only interaction flags skip those idle waits without changing app behavior.
        // XCUIApplication resets them when a new automation session is established, so callers
        // apply them both before and immediately after launch.
        let selector = NSSelectorFromString("setCurrentInteractionOptions:")
        guard app.responds(to: selector) else {
            XCTFail("This Xcode version does not expose XCUITest interaction options")
            return
        }
        typealias InteractionOptionsSetter = @convention(c) (AnyObject, Selector, UInt) -> Void
        let setter = unsafeBitCast(app.method(for: selector), to: InteractionOptionsSetter.self)
        setter(app, selector, 3)
    }

    private func detectDiagnosticIdentity(in app: XCUIApplication) throws -> DiagnosticIdentity {
        let mainContact = app.staticTexts["nomepeer"].firstMatch
        let peerContact = app.staticTexts["nometest"].firstMatch
        let deadline = Date().addingTimeInterval(30)

        while Date() < deadline {
            if mainContact.exists && mainContact.isHittable {
                print("[NOME_DIAG] identity sender=main contact=nomepeer")
                return DiagnosticIdentity(sender: "main", contact: "nomepeer")
            }
            if peerContact.exists && peerContact.isHittable {
                print("[NOME_DIAG] identity sender=peer contact=nometest")
                return DiagnosticIdentity(sender: "peer", contact: "nometest")
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }

        attachDiagnosticScreenshot(app, name: "identity-not-found")
        XCTFail("Expected the nometest or nomepeer diagnostic contact on the chat list")
        throw DiagnosticFailure.identityNotFound
    }

    private func openDiagnosticContact(_ contact: String, in app: XCUIApplication) throws {
        let contactLabel = app.staticTexts[contact].firstMatch
        XCTAssertTrue(contactLabel.exists, "Diagnostic contact is not visible: \(contact)")
        contactLabel.tap()

        let editor = app.textViews["chat-compose-editor"].firstMatch
        if !editor.waitForExistence(timeout: 20) {
            attachDiagnosticScreenshot(app, name: "composer-not-found-\(contact)")
            XCTFail("Chat composer did not appear for \(contact)")
            throw DiagnosticFailure.composerNotFound
        }
    }

    private func sendDiagnosticMessage(_ message: String, round: Int, in app: XCUIApplication) throws {
        let editor = app.textViews["chat-compose-editor"].firstMatch
        XCTAssertTrue(editor.exists, "Chat composer disappeared before round \(round)")
        editor.tap()
        editor.typeText(message)

        let sendButton = app.buttons["chat-send-button"].firstMatch
        guard sendButton.waitForExistence(timeout: 10), sendButton.isEnabled else {
            attachDiagnosticScreenshot(app, name: "send-button-unavailable-round-\(round)")
            XCTFail("Send button was unavailable for round \(round)")
            throw DiagnosticFailure.sendButtonUnavailable
        }

        print("[NOME_DIAG] local-send-start round=\(round) message=\(message)")
        let startedAt = Date()
        sendButton.tap()

        let deadline = startedAt.addingTimeInterval(35)
        var cleared = false
        while Date() < deadline {
            let value = editor.value as? String
            if value != message {
                cleared = true
                break
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }

        let elapsed = Date().timeIntervalSince(startedAt)
        print("[NOME_DIAG] local-send-complete round=\(round) elapsed=\(String(format: "%.3f", elapsed)) cleared=\(cleared) message=\(message)")
        if !cleared {
            attachDiagnosticScreenshot(app, name: "local-send-stalled-round-\(round)")
            XCTFail("Local send remained in progress for 35 seconds: \(message)")
            throw DiagnosticFailure.localSendStalled
        }

        app.swipeUp()
        let localMessage = diagnosticMessageElement(message, in: app)
        if !localMessage.waitForExistence(timeout: 10) {
            attachDiagnosticScreenshot(app, name: "local-message-missing-round-\(round)")
            XCTFail("Sent message did not appear in the local conversation: \(message)")
            throw DiagnosticFailure.localMessageMissing
        }
    }

    private func diagnosticMessage(runID: String, sender: String, round: Int) -> String {
        "nome-diag-\(runID)-\(sender)-r\(round)"
    }

    private func diagnosticMessageElement(_ message: String, in app: XCUIApplication) -> XCUIElement {
        return app.descendants(matching: .any)
            .matching(identifier: "chat-real-core-diagnostic-message")
            .matching(NSPredicate(format: "value == %@", message))
            .firstMatch
    }

    private func attachDiagnosticScreenshot(_ app: XCUIApplication, name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private enum DiagnosticFailure: Error {
        case invalidConfiguration
        case identityNotFound
        case composerNotFound
        case sendButtonUnavailable
        case localSendStalled
        case localMessageMissing
    }
}
