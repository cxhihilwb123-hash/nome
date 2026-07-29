//
//  CallController.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 21/05/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/services/calls.md

import Foundation
import CallKit
import StoreKit
import PushKit
import AVFoundation
import SimpleXChat
import WebRTC

// Spec: spec/services/calls.md#CallController
class CallController: NSObject, CXProviderDelegate, PKPushRegistryDelegate, ObservableObject {
    static let shared = CallController()
    static let isInChina = SKStorefront().countryCode == "CHN"
    static func useCallKit() -> Bool {
#if DEBUG
        if ProcessInfo.processInfo.environment["NOME_DISABLE_CALLKIT_FOR_TESTS"] == "1" {
            return false
        }
#endif
        return !isInChina && callKitEnabledGroupDefault.get()
    }

    static func useCallKit(callUUID: String?) -> Bool {
        useCallKit() && callUUID.flatMap { UUID(uuidString: $0) } != nil
    }

    private let provider = CXProvider(configuration: {
        let configuration = CXProviderConfiguration()
        configuration.supportsVideo = true
        configuration.supportedHandleTypes = [.generic]
        configuration.includesCallsInRecents = UserDefaults.standard.bool(forKey: DEFAULT_CALL_KIT_CALLS_IN_RECENTS)
        configuration.maximumCallGroups = 1
        configuration.maximumCallsPerCallGroup = 1
        configuration.iconTemplateImageData = UIImage(named: "icon-transparent")?.pngData()
        return configuration
    }())
    private let controller = CXCallController()
    private let callManager = CallManager()
    @Published var activeCallInvitation: RcvCallInvitation?
    var shouldSuspendChat: Bool = false
    var fulfillOnConnect: CXAnswerCallAction? = nil
    private(set) var isAudioSessionActive = false

    // PKPushRegistry is used from notification service extension
    private let registry = PKPushRegistry(queue: nil)

    override init() {
        super.init()
        provider.setDelegate(self, queue: nil)
        registry.delegate = self
        registry.desiredPushTypes = [.voIP]
    }

    func providerDidReset(_ provider: CXProvider) {
        logger.debug("CallController.providerDidReset")
    }

    // Spec: spec/services/calls.md#CXStartCallAction
    func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        logger.debug("CallController.provider CXStartCallAction")
        guard NomeActivationGate.require(.call) else {
            action.fail()
            return
        }
        configureAudioSession(action.isVideo ? .video : .audio)
#if DEBUG
        CallAudioDiagnosticLog.write("provider-start video=\(action.isVideo) useCallKit=\(CallController.useCallKit()) china=\(CallController.isInChina) \(audioSessionState())")
#endif
        if callManager.startOutgoingCall(callUUID: action.callUUID.uuidString.lowercased()) {
            action.fulfill()
            provider.reportOutgoingCall(with: action.callUUID, startedConnectingAt: nil)
        } else {
            action.fail()
        }
    }

    // Spec: spec/services/calls.md#CXAnswerCallAction
    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        logger.debug("CallController.provider CXAnswerCallAction")
        guard NomeActivationGate.require(.call) else {
            action.fail()
            return
        }
        Task {
            let chatIsReady = await waitUntilChatStarted(timeoutMs: 30_000, stepMs: 500)
            logger.debug("CallController chat started \(chatIsReady) \(ChatModel.shared.chatInitialized) \(ChatModel.shared.chatRunning == true) \(String(describing: AppChatState.shared.value))")
            if !chatIsReady {
                action.fail()
                return
            }
            if !ChatModel.shared.callInvitations.values.contains(where: { inv in inv.callUUID == action.callUUID.uuidString.lowercased() }) {
                try? await justRefreshCallInvitations()
                logger.debug("CallController: updated call invitations chat")
            }
            await MainActor.run {
                logger.debug("CallController.provider will answer on call")

                let media = ChatModel.shared.callInvitations.values
                    .first(where: { $0.callUUID == action.callUUID.uuidString.lowercased() })?
                    .callType.media ?? .audio
                self.configureAudioSession(media)
                if callManager.answerIncomingCall(callUUID: action.callUUID.uuidString.lowercased()) {
                    logger.debug("CallController.provider answered on call")
                    // WebRTC call should be in connected state to fulfill.
                    // Otherwise no audio and mic working on lockscreen
                    fulfillOnConnect = action
                } else {
                    logger.debug("CallController.provider will fail the call")
                    action.fail()
                }
            }
        }
    }

    // Spec: spec/services/calls.md#CXEndCallAction
    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        logger.debug("CallController.provider CXEndCallAction")
        // Should be nil here if connection was in connected state
        fulfillOnConnect?.fail()
        fulfillOnConnect = nil
        callManager.endCall(callUUID: action.callUUID.uuidString.lowercased()) { ok in
            if ok {
                action.fulfill()
            } else {
                action.fail()
            }
            self.suspendOnEndCall()
        }
    }

    // Spec: spec/services/calls.md#CXSetMutedCallAction
    func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        if callManager.enableMedia(source: .mic, enable: !action.isMuted, callUUID:  action.callUUID.uuidString.lowercased()) {
            action.fulfill()
        } else {
            action.fail()
        }
    }

    func provider(_ provider: CXProvider, timedOutPerforming action: CXAction) {
        logger.debug("timed out: \(String(describing: action))")
        action.fulfill()
    }

    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        logger.debug("CallController: activating audioSession and audio in WebRTCClient")
        isAudioSessionActive = true
        RTCAudioSession.sharedInstance().audioSessionDidActivate(audioSession)
        RTCAudioSession.sharedInstance().isAudioEnabled = true
#if DEBUG
        CallAudioDiagnosticLog.write("did-activate \(audioSessionState(audioSession))")
#endif
        if ChatModel.shared.activeCall?.hasVideo == true {
            let activeExternalOutput = audioSession.currentRoute.outputs.contains {
                $0.portType != .builtInReceiver && $0.portType != .builtInSpeaker
            }
            if !activeExternalOutput {
                try? audioSession.overrideOutputAudioPort(.speaker)
            }
        }
    }

    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        logger.debug("CallController: deactivating audioSession and audio in WebRTCClient")
        isAudioSessionActive = false
        RTCAudioSession.sharedInstance().audioSessionDidDeactivate(audioSession)
        RTCAudioSession.sharedInstance().isAudioEnabled = false
#if DEBUG
        CallAudioDiagnosticLog.write("did-deactivate \(audioSessionState(audioSession))")
#endif
        suspendOnEndCall()
    }

    func suspendOnEndCall() {
        if shouldSuspendChat {
            // The delay allows to accept the second call before suspending a chat
            // see `.onChange(of: scenePhase)` in SimpleXApp
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
                logger.debug("CallController: shouldSuspendChat \(String(describing: self?.shouldSuspendChat))")
                if ChatModel.shared.activeCall == nil && self?.shouldSuspendChat == true {
                    self?.shouldSuspendChat = false
                    suspendChat()
                    BGManager.shared.schedule()
                }
            }
        }
    }

    private func waitUntilChatStarted(timeoutMs: UInt64, stepMs: UInt64) async -> Bool {
        logger.debug("CallController waiting until chat started")
        var t: UInt64 = 0
        repeat {
            if ChatModel.shared.chatInitialized, ChatModel.shared.chatRunning == true, case .active = AppChatState.shared.value {
                return true
            }
            _ = try? await Task.sleep(nanoseconds: stepMs * 1000000)
            t += stepMs
        } while t < timeoutMs
        return false
    }

    @objc(pushRegistry:didUpdatePushCredentials:forType:)
    func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        logger.debug("CallController: didUpdate push credentials for type \(type.rawValue)")
    }

    // Spec: spec/services/calls.md#pushRegistryDidReceive
    func pushRegistry(_ registry: PKPushRegistry, didReceiveIncomingPushWith payload: PKPushPayload, for type: PKPushType, completion: @escaping () -> Void) {
        logger.debug("CallController: did receive push with type \(type.rawValue)")
        if type != .voIP {
            completion()
            return
        }
        // PushKit requires every VoIP push to be reported to CallKit. In local-only mode,
        // report it as expired so iOS receives the required acknowledgement without starting
        // chat networking or exposing an answer action that could bypass activation.
        guard NomeActivationGate.allowsNetworking else {
            self.reportExpiredCall(payload: payload, completion)
            return
        }
        if AppChatState.shared.value == .stopped {
            self.reportExpiredCall(payload: payload, completion)
            return
        }
        // Extract the call information from the push notification payload
        let m = ChatModel.shared
        if let contactId = payload.dictionaryPayload["contactId"] as? String,
           let displayName = payload.dictionaryPayload["displayName"] as? String,
           let callUUID = payload.dictionaryPayload["callUUID"] as? String,
           let uuid = UUID(uuidString: callUUID),
           let callTsInterval = payload.dictionaryPayload["callTs"] as? TimeInterval,
           let mediaStr = payload.dictionaryPayload["media"] as? String,
           let media = CallMediaType(rawValue: mediaStr) {
            let update = self.cxCallUpdate(contactId, displayName, media)
            let callTs = Date(timeIntervalSince1970: callTsInterval)
            if callTs.timeIntervalSinceNow >= -180 {
                logger.debug("CallController: report pushkit call via CallKit")
                self.provider.reportNewIncomingCall(with: uuid, update: update) { error in
                    if error != nil {
                        m.callInvitations.removeValue(forKey: contactId)
                    }
                    // Tell PushKit that the notification is handled.
                    completion()
                }
            } else {
                logger.debug("CallController will expire call 1")
                self.reportExpiredCall(update: update, completion)
            }
        } else {
            logger.debug("CallController will expire call 2")
            self.reportExpiredCall(payload: payload, completion)
        }

        //DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
        if (!ChatModel.shared.chatInitialized) {
            logger.debug("CallController: initializing chat")
            do {
                try initializeChat(start: true, refreshInvitations: false)
            } catch let error {
                logger.error("CallController: initializing chat error: \(error)")
                if let call = ChatModel.shared.activeCall {
                    self.endCall(call: call, completed: completion)
                }
                return
            }
        }
        logger.debug("CallController: initialized chat")
        startChatForCall()
        logger.debug("CallController: started chat")
        self.shouldSuspendChat = true
    }

    // This function fulfils the requirement to always report a call when PushKit notification is received,
    // even when there is no more active calls by the time PushKit payload is processed.
    // See the note in the bottom of this article:
    // https://developer.apple.com/documentation/pushkit/pkpushregistrydelegate/2875784-pushregistry
    private func reportExpiredCall(update: CXCallUpdate, _ completion: @escaping () -> Void) {
        logger.debug("CallController: report expired pushkit call via CallKit")
        let uuid = UUID()
        provider.reportNewIncomingCall(with: uuid, update: update) { error in
            if error == nil {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    self.provider.reportCall(with: uuid, endedAt: nil, reason: .remoteEnded)
                }
            }
            completion()
        }
    }

    private func reportExpiredCall(payload: PKPushPayload, _ completion: @escaping () -> Void) {
        let update = CXCallUpdate()
        let displayName = payload.dictionaryPayload["displayName"] as? String
        let media = payload.dictionaryPayload["media"] as? String
        update.localizedCallerName = displayName ?? NSLocalizedString("Unknown caller", comment: "callkit banner")
        update.hasVideo = media == CallMediaType.video.rawValue
        reportExpiredCall(update: update, completion)
    }

    // Spec: spec/services/calls.md#reportNewIncomingCall
    func reportNewIncomingCall(invitation: RcvCallInvitation, completion: @escaping (Error?) -> Void) {
        logger.debug("CallController.reportNewIncomingCall, UUID=\(String(describing: invitation.callUUID))")
        if CallController.useCallKit(callUUID: invitation.callUUID),
           let callUUID = invitation.callUUID,
           let uuid = UUID(uuidString: callUUID) {
            if invitation.callTs.timeIntervalSinceNow >= -180 {
                let update = cxCallUpdate(invitation: invitation)
                provider.reportNewIncomingCall(with: uuid, update: update, completion: completion)
            }
        } else {
            NtfManager.shared.notifyCallInvitation(invitation)
            if invitation.callTs.timeIntervalSinceNow >= -180 {
                activeCallInvitation = invitation
            }
        }
    }

    private func cxCallUpdate(invitation: RcvCallInvitation) -> CXCallUpdate {
        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: invitation.contact.id)
        update.hasVideo = invitation.callType.media == .video
        update.localizedCallerName = invitation.contact.displayName
        return update
    }

    private func cxCallUpdate(_ contactId: String, _ displayName: String, _ media: CallMediaType) -> CXCallUpdate {
        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: contactId)
        update.hasVideo = media == .video
        update.localizedCallerName = displayName
        return update
    }

    func reportIncomingCall(call: Call, connectedAt dateConnected: Date?) {
        logger.debug("CallController: reporting incoming call connected")
        if CallController.useCallKit(callUUID: call.callUUID) {
            // Fulfilling this action only after connect, otherwise there are no audio and mic on lockscreen
            fulfillOnConnect?.fulfill()
            fulfillOnConnect = nil
        }
    }

    // Spec: spec/services/calls.md#reportOutgoingCall
    func reportOutgoingCall(call: Call, connectedAt dateConnected: Date?) {
        logger.debug("CallController: reporting outgoing call connected")
        if CallController.useCallKit(callUUID: call.callUUID), let callUUID = call.callUUID, let uuid = UUID(uuidString: callUUID) {
            provider.reportOutgoingCall(with: uuid, connectedAt: dateConnected)
        }
    }

    func reportCallRemoteEnded(invitation: RcvCallInvitation) {
        logger.debug("CallController: reporting remote ended")
        if CallController.useCallKit(callUUID: invitation.callUUID), let callUUID = invitation.callUUID, let uuid = UUID(uuidString: callUUID) {
            provider.reportCall(with: uuid, endedAt: nil, reason: .remoteEnded)
        } else if invitation.contact.id == activeCallInvitation?.contact.id {
            activeCallInvitation = nil
        }
    }

    func reportCallRemoteEnded(call: Call) {
        logger.debug("CallController: reporting remote ended")
        fulfillOnConnect?.fail()
        fulfillOnConnect = nil
        if CallController.useCallKit(callUUID: call.callUUID), let callUUID = call.callUUID, let uuid = UUID(uuidString: callUUID) {
            provider.reportCall(with: uuid, endedAt: nil, reason: .remoteEnded)
        }
    }

    func startCall(_ contact: Contact, _ media: CallMediaType) {
        logger.debug("CallController.startCall")
        guard NomeActivationGate.require(.call) else { return }
#if DEBUG
        CallAudioDiagnosticLog.reset()
        CallAudioDiagnosticLog.write("start-call direction=outgoing media=\(media) useCallKit=\(CallController.useCallKit()) china=\(CallController.isInChina) micPermission=\(AVCaptureDevice.authorizationStatus(for: .audio).rawValue) \(audioSessionState())")
#endif
        let callUUID = callManager.newOutgoingCall(contact, media)
        guard let uuid = UUID(uuidString: callUUID) else {
            return
        }
        if CallController.useCallKit() {
            let handle = CXHandle(type: .generic, value: contact.id)
            let action = CXStartCallAction(call: uuid, handle: handle)
            action.isVideo = media == .video
            requestTransaction(with: action) {
                let update = CXCallUpdate()
                update.remoteHandle = CXHandle(type: .generic, value: contact.id)
                update.hasVideo = media == .video
                update.localizedCallerName = contact.displayName
                self.provider.reportCall(with: uuid, updated: update)
            }
        } else if callManager.startOutgoingCall(callUUID: callUUID) {
            logger.debug("CallController.startCall: call started")
        } else {
            logger.error("CallController.startCall: no active call")
        }
    }

    func answerCall(invitation: RcvCallInvitation) {
        logger.debug("CallController: answering a call")
        guard NomeActivationGate.require(.call) else { return }
#if DEBUG
        CallAudioDiagnosticLog.reset()
        CallAudioDiagnosticLog.write("start-call direction=incoming media=\(invitation.callType.media) useCallKit=\(CallController.useCallKit()) callKitUUID=\(CallController.useCallKit(callUUID: invitation.callUUID)) china=\(CallController.isInChina) micPermission=\(AVCaptureDevice.authorizationStatus(for: .audio).rawValue) \(audioSessionState())")
#endif
        if CallController.useCallKit(callUUID: invitation.callUUID),
           let callUUID = invitation.callUUID,
           let uuid = UUID(uuidString: callUUID) {
            requestTransaction(with: CXAnswerCallAction(call: uuid))
        } else {
            callManager.answerIncomingCall(invitation: invitation)
        }
        if invitation.contact.id == self.activeCallInvitation?.contact.id {
            self.activeCallInvitation = nil
        }
    }

    private func configureAudioSession(_ media: CallMediaType) {
        let audioSession = AVAudioSession.sharedInstance()
        do {
            let options: AVAudioSession.CategoryOptions = media == .video
                ? [.defaultToSpeaker, .allowBluetoothHFP]
                : [.allowBluetoothHFP]
            try audioSession.setCategory(
                .playAndRecord,
                mode: media == .video ? .videoChat : .voiceChat,
                options: options
            )
            logger.debug("CallController: audio session configured before CallKit activation")
#if DEBUG
            CallAudioDiagnosticLog.write("configured media=\(media) \(audioSessionState(audioSession))")
#endif
        } catch {
            logger.error("CallController: failed configuring audio session: \(error.localizedDescription)")
#if DEBUG
            CallAudioDiagnosticLog.write("configure-failed code=\((error as NSError).code)")
#endif
        }
    }

#if DEBUG
    private func audioSessionState(_ session: AVAudioSession = .sharedInstance()) -> String {
        let inputs = session.currentRoute.inputs.map { $0.portType.rawValue }.joined(separator: ",")
        let outputs = session.currentRoute.outputs.map { $0.portType.rawValue }.joined(separator: ",")
        return "activeFlag=\(isAudioSessionActive) category=\(session.category.rawValue) mode=\(session.mode.rawValue) inputs=[\(inputs)] outputs=[\(outputs)]"
    }
#endif

    func endCall(callUUID: String) {
        let uuid = UUID(uuidString: callUUID)
        logger.debug("CallController: ending the call with UUID \(callUUID)")
        if CallController.useCallKit(callUUID: callUUID), let uuid {
            requestTransaction(with: CXEndCallAction(call: uuid))
        } else {
            callManager.endCall(callUUID: callUUID) { ok in
                if ok {
                    logger.debug("CallController.endCall: call ended")
                } else {
                    logger.error("CallController.endCall: no active call pr call invitation to end")
                }
            }
        }
    }

    func endCall(invitation: RcvCallInvitation) {
        logger.debug("CallController: ending the call with invitation")
        callManager.endCall(invitation: invitation) {
            if invitation.contact.id == self.activeCallInvitation?.contact.id {
                DispatchQueue.main.async {
                    self.activeCallInvitation = nil
                }
            }
        }
    }

    func endCall(call: Call, completed: @escaping () -> Void) {
        logger.debug("CallController: ending the call with call instance")
        callManager.endCall(call: call, completed: completed)
    }

    func callAction(invitation: RcvCallInvitation, action: NtfCallAction) {
        switch action {
        case .accept: answerCall(invitation: invitation)
        case .reject: endCall(invitation: invitation)
        }
    }

    func showInRecents(_ show: Bool) {
        let conf = provider.configuration
        conf.includesCallsInRecents = show
        provider.configuration = conf
    }

    // Spec: spec/services/calls.md#hasActiveCalls
    func hasActiveCalls() -> Bool {
        controller.callObserver.calls.count > 0
    }

    private func requestTransaction(with action: CXAction, onSuccess: @escaping () -> Void = {}) {
        controller.request(CXTransaction(action: action)) { error in
            if let error = error {
                logger.error("CallController.requestTransaction error requesting transaction: \(error.localizedDescription)")
            } else {
                logger.debug("CallController.requestTransaction requested transaction successfully")
                onSuccess()
            }
        }
    }
}
