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
    private let activationBaseURLKey = "NOME_ACTIVATION_BASE_URL"
    private let activationInviteCodeKey = "NOME_ACTIVATION_INVITE_CODE"
    private let callContactKey = "NOME_CALL_DIAGNOSTIC_CONTACT"
    private let callMediaKey = "NOME_CALL_DIAGNOSTIC_MEDIA"
    private let callAnswerKey = "NOME_CALL_DIAGNOSTIC_ANSWER"
    private let callRoundsKey = "NOME_CALL_DIAGNOSTIC_ROUNDS"

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

    func testNomeActivationFormFailsClosedWithoutEndpoint() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-NomeActivationPreview")
        // Override any build-time production URL so this test proves the explicit no-endpoint path.
        app.launchEnvironment[activationBaseURLKey] = ""
        app.launch()

        let codeField = app.textFields["nome.activation.inviteCode"]
        let redeemButton = app.buttons["nome.activation.redeem"]
        XCTAssertTrue(codeField.waitForExistence(timeout: 10), "Invitation-code field is missing")
        XCTAssertTrue(redeemButton.waitForExistence(timeout: 5), "Activation action is missing")
        XCTAssertFalse(redeemButton.isEnabled, "An empty invitation code must not be submitted")

        codeField.tap()
        codeField.typeText("TEST-CODE")
        XCTAssertTrue(redeemButton.isEnabled, "A non-empty invitation code should enable activation")
        redeemButton.tap()

        let activationError = app.descendants(matching: .any)
            .matching(identifier: "nome.activation.error")
            .firstMatch
        XCTAssertTrue(
            activationError.waitForExistence(timeout: 10),
            "A build without an activation endpoint must fail closed with an actionable error"
        )
    }

    func testNomeActivationBlocksRedeemsAndPersistsAcrossRelaunch() throws {
        let environment = ProcessInfo.processInfo.environment
        let baseURL = environment[activationBaseURLKey] ?? "http://127.0.0.1:33073"
        let inviteCode = environment[activationInviteCodeKey] ?? "NOME-2345-6789-ABCD-EFGH-JKMN"
        try requireActivationE2EServer(baseURL: baseURL, expectedMode: "enforced")

        let app = XCUIApplication()
        app.launchArguments.append("-NomeConversationPreview")
        app.launchEnvironment[activationBaseURLKey] = baseURL
        app.launch()
        skipQuiescenceWaits(in: app)

        let access = app.staticTexts["nome.activation.previewAccess"].firstMatch
        XCTAssertTrue(access.waitForExistence(timeout: 10), "Activation preview access marker is missing")
        let policyEnforced = expectation(
            for: NSPredicate(format: "label == %@", "local_only"),
            evaluatedWith: access
        )
        wait(for: [policyEnforced], timeout: 10)

        let editor = app.textViews["chat-compose-editor"].firstMatch
        XCTAssertTrue(editor.waitForExistence(timeout: 15), "Conversation preview composer is missing")
        editor.tap()
        editor.typeText("activation-gate-probe")

        let sendButton = app.buttons["chat-send-button"].firstMatch
        XCTAssertTrue(sendButton.waitForExistence(timeout: 10), "Send button is missing")
        XCTAssertTrue(sendButton.isEnabled, "Send button must be enabled before the gate handles the action")
        sendButton.tap()

        let codeField = app.textFields["nome.activation.inviteCode"].firstMatch
        XCTAssertTrue(codeField.waitForExistence(timeout: 10), "Unactivated send must open the invitation-code sheet")
        attachDiagnosticScreenshot(app, name: "activation-e2e-blocked")

        codeField.tap()
        codeField.typeText(inviteCode)
        let redeemButton = app.buttons["nome.activation.redeem"].firstMatch
        XCTAssertTrue(redeemButton.waitForExistence(timeout: 5), "Activation action is missing")
        XCTAssertTrue(redeemButton.isEnabled, "A valid invitation code must enable activation")
        redeemButton.tap()

        let sheetDismissed = expectation(
            for: NSPredicate(format: "exists == false"),
            evaluatedWith: codeField
        )
        wait(for: [sheetDismissed], timeout: 20)
        XCTAssertFalse(app.descendants(matching: .any)["nome.activation.error"].firstMatch.exists)
        attachDiagnosticScreenshot(app, name: "activation-e2e-redeemed")

        XCTAssertTrue(sendButton.waitForExistence(timeout: 10), "Send button did not return after activation")
        sendButton.tap()
        XCTAssertFalse(
            app.textFields["nome.activation.inviteCode"].firstMatch.waitForExistence(timeout: 3),
            "The same send action must not be blocked after activation"
        )

        app.terminate()
        app.launch()
        skipQuiescenceWaits(in: app)

        let relaunchedEditor = app.textViews["chat-compose-editor"].firstMatch
        XCTAssertTrue(relaunchedEditor.waitForExistence(timeout: 15), "Composer is missing after relaunch")
        relaunchedEditor.tap()
        relaunchedEditor.typeText("activation-relaunch-probe")
        let relaunchedSend = app.buttons["chat-send-button"].firstMatch
        XCTAssertTrue(relaunchedSend.waitForExistence(timeout: 10), "Send button is missing after relaunch")
        relaunchedSend.tap()
        XCTAssertFalse(
            app.textFields["nome.activation.inviteCode"].firstMatch.waitForExistence(timeout: 3),
            "Activation must persist across an application relaunch"
        )
        attachDiagnosticScreenshot(app, name: "activation-e2e-relaunch-persisted")
    }

    func testNomeActivationDisabledPolicyAllowsMessageWithoutInvite() throws {
        let baseURL = ProcessInfo.processInfo.environment[activationBaseURLKey] ?? "http://127.0.0.1:33073"
        try requireActivationE2EServer(baseURL: baseURL, expectedMode: "disabled")

        let app = XCUIApplication()
        app.launchArguments.append("-NomeConversationPreview")
        app.launchEnvironment[activationBaseURLKey] = baseURL
        app.launch()
        skipQuiescenceWaits(in: app)

        let access = app.staticTexts["nome.activation.previewAccess"].firstMatch
        XCTAssertTrue(access.waitForExistence(timeout: 10), "Activation preview access marker is missing")
        let policyDisabled = expectation(
            for: NSPredicate(format: "label == %@", "full"),
            evaluatedWith: access
        )
        wait(for: [policyDisabled], timeout: 10)

        let editor = app.textViews["chat-compose-editor"].firstMatch
        XCTAssertTrue(editor.waitForExistence(timeout: 10), "Conversation preview composer is missing")
        editor.tap()
        editor.typeText("activation-disabled-policy-probe")
        let sendButton = app.buttons["chat-send-button"].firstMatch
        XCTAssertTrue(sendButton.waitForExistence(timeout: 10), "Send button is missing")
        sendButton.tap()
        XCTAssertFalse(
            app.textFields["nome.activation.inviteCode"].firstMatch.waitForExistence(timeout: 3),
            "A disabled activation policy must not block message sending"
        )
        attachDiagnosticScreenshot(app, name: "activation-e2e-policy-disabled")
    }

    private func requireActivationE2EServer(baseURL: String, expectedMode: String) throws {
        guard let url = URL(string: "\(baseURL)/__e2e/summary") else {
            XCTFail("Invalid activation E2E base URL: \(baseURL)")
            throw DiagnosticFailure.invalidConfiguration
        }

        let ready = expectation(description: "activation E2E server readiness")
        var available = false
        var observedMode: String?
        var request = URLRequest(url: url)
        request.timeoutInterval = 2
        URLSession.shared.dataTask(with: request) { data, response, _ in
            available = (response as? HTTPURLResponse)?.statusCode == 200
            if let data,
               let body = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let policy = body["policy"] as? [String: Any] {
                observedMode = policy["mode"] as? String
            }
            ready.fulfill()
        }.resume()
        wait(for: [ready], timeout: 3)
        guard available, observedMode == expectedMode else {
            throw XCTSkip("Start the local activation E2E server in \(expectedMode) mode at \(baseURL) to enable this test")
        }
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
        guard let media = environment[callMediaKey], ["audio", "video"].contains(media) else {
            throw XCTSkip("Set \(callMediaKey)=audio|video to enable the call diagnostic")
        }

        let app = XCUIApplication()
        if let contact = environment[callContactKey], !contact.isEmpty {
            app.launchEnvironment[callContactKey] = contact
        }
        app.launchEnvironment[callMediaKey] = media
        app.launchEnvironment["NOME_DISABLE_CALLKIT_FOR_TESTS"] = "1"
        app.launch()
        skipQuiescenceWaits(in: app)

        let rounds = max(1, Int(environment[callRoundsKey] ?? "1") ?? 1)
        let contact = try environment[callContactKey].flatMap { $0.isEmpty ? nil : $0 }
            ?? detectDiagnosticIdentity(in: app).contact
        let contactLabel = app.staticTexts[contact].firstMatch
        XCTAssertTrue(contactLabel.waitForExistence(timeout: 30), "Call diagnostic contact is not visible: \(contact)")
        contactLabel.tap()
        XCTAssertTrue(app.textViews["chat-compose-editor"].firstMatch.waitForExistence(timeout: 20))

        for round in 1 ... rounds {
            let callMenu = app.buttons["chat-call-menu"].firstMatch
            XCTAssertTrue(callMenu.waitForExistence(timeout: 10), "Call menu is not accessible in round \(round)")
            callMenu.tap()

            let mediaButton = app.buttons[media == "audio" ? "chat-audio-call" : "chat-video-call"].firstMatch
            XCTAssertTrue(mediaButton.waitForExistence(timeout: 10), "Call media action is not accessible: \(media), round \(round)")
            print("[NOME_CALL_DIAG] start media=\(media) contact=\(contact) round=\(round)")
            mediaButton.tap()

            let connected = app.staticTexts["已连接"].firstMatch
            XCTAssertTrue(connected.waitForExistence(timeout: 40), "Call did not reach the connected state: \(media), round \(round)")
            RunLoop.current.run(until: Date().addingTimeInterval(5))
            attachDiagnosticScreenshot(app, name: "call-\(media)-connected-round-\(round)")

            let end = app.buttons["active-call-end"].firstMatch
            XCTAssertTrue(end.waitForExistence(timeout: 10), "End call button is not accessible in round \(round)")
            end.tap()
            XCTAssertTrue(app.textViews["chat-compose-editor"].firstMatch.waitForExistence(timeout: 20), "Chat did not return after round \(round)")
            RunLoop.current.run(until: Date().addingTimeInterval(2))
        }
    }

    func testAnswerNomeCallDiagnostic() throws {
        guard ProcessInfo.processInfo.environment[callAnswerKey] == "1" else {
            throw XCTSkip("Set \(callAnswerKey)=1 to enable the incoming call diagnostic")
        }

        let app = XCUIApplication()
        app.launchEnvironment["NOME_DISABLE_CALLKIT_FOR_TESTS"] = "1"
        app.launch()
        skipQuiescenceWaits(in: app)

        let rounds = max(1, Int(ProcessInfo.processInfo.environment[callRoundsKey] ?? "1") ?? 1)
        for round in 1 ... rounds {
            let accept = app.buttons["incoming-call-accept"].firstMatch
            XCTAssertTrue(accept.waitForExistence(timeout: 60), "Incoming call did not arrive in round \(round)")
            accept.tap()

            let connected = app.staticTexts["已连接"].firstMatch
            XCTAssertTrue(connected.waitForExistence(timeout: 40), "Incoming call did not reach the connected state in round \(round)")
            RunLoop.current.run(until: Date().addingTimeInterval(5))
            attachDiagnosticScreenshot(app, name: "call-incoming-connected-round-\(round)")
            let end = app.buttons["active-call-end"].firstMatch
            XCTAssertTrue(
                waitForDisappearance(end, timeout: 20),
                "Incoming call UI did not close after the caller ended round \(round)"
            )
            RunLoop.current.run(until: Date().addingTimeInterval(2))
        }
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
        if let baseURL = ProcessInfo.processInfo.environment[activationBaseURLKey] {
            app.launchEnvironment[activationBaseURLKey] = baseURL
        }

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

    private func waitForDisappearance(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if !element.exists {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
        return !element.exists
    }

    private func detectDiagnosticIdentity(in app: XCUIApplication) throws -> DiagnosticIdentity {
        let identities = [
            (element: app.staticTexts["nomepeer"].firstMatch, identity: DiagnosticIdentity(sender: "main", contact: "nomepeer")),
            (element: app.staticTexts["nometest"].firstMatch, identity: DiagnosticIdentity(sender: "peer", contact: "nometest")),
            (element: app.staticTexts["NomeTokyoB"].firstMatch, identity: DiagnosticIdentity(sender: "main", contact: "NomeTokyoB")),
            (element: app.staticTexts["NomeTokyoA"].firstMatch, identity: DiagnosticIdentity(sender: "peer", contact: "NomeTokyoA")),
        ]
        let deadline = Date().addingTimeInterval(30)

        while Date() < deadline {
            for candidate in identities where candidate.element.exists && candidate.element.isHittable {
                print("[NOME_DIAG] identity sender=\(candidate.identity.sender) contact=\(candidate.identity.contact)")
                return candidate.identity
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
