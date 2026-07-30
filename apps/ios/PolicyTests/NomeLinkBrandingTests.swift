import Darwin
import Foundation

@main
struct NomeLinkBrandingTests {
    private static var failures: [String] = []

    static func main() {
        expect("protocol contact link uses Nome", simplexChatLink("simplex:/contact#opaque") == "https://nome.im/contact#opaque")
        expect("protocol file link uses Nome", simplexChatLink("simplex:/file#opaque") == "https://nome.im/file#opaque")
        expect("protocol short invitation uses Nome resolver", simplexChatLink("simplex:/i#opaque") == "https://smp.nome.im/i#opaque")
        expect("protocol short channel uses Nome resolver", simplexChatLink("simplex:/c#opaque") == "https://smp.nome.im/c#opaque")
        expect("legacy public invitation is rebranded", simplexChatLink("https://simplex.chat/invitation#opaque") == "https://nome.im/invitation#opaque")
        expect("legacy hosted short link keeps its resolver", simplexChatLink("https://smp6.simplex.im/a#hosted") == "https://smp6.simplex.im/a#hosted")
        expect("Nome hosted short link keeps its resolver", simplexChatLink("https://smp.nome.im/i#hosted") == "https://smp.nome.im/i#hosted")
        expect("Nome hosted channel keeps its resolver", simplexChatLink("https://smp.nome.im/c#hosted") == "https://smp.nome.im/c#hosted")
        expect("legacy hosted channel keeps its resolver", simplexChatLink("https://smp6.simplex.im/c#hosted") == "https://smp6.simplex.im/c#hosted")
        expect("documentation URL is untouched", simplexChatLink("https://simplex.chat/docs/guide.html") == "https://simplex.chat/docs/guide.html")

        expect("Nome contact link normalizes for Core", normalizeNomeChatLink("https://nome.im/contact#opaque") == "simplex:/contact#opaque")
        expect("legacy HTTP file normalizes for Core", normalizeNomeChatLink("http://simplex.chat/file#legacy") == "simplex:/file#legacy")
        expect("Nome branded short link restores its resolver", normalizeNomeChatLink("https://nome.im/i#hosted") == "https://smp.nome.im/i#hosted")
        expect("Nome branded channel restores its resolver", normalizeNomeChatLink("https://nome.im/c#hosted") == "https://smp.nome.im/c#hosted")
        expect("Nome hosted short link keeps its resolver", normalizeNomeChatLink("https://smp.nome.im/i#hosted") == "https://smp.nome.im/i#hosted")
        expect("Nome hosted channel keeps its resolver for Core", normalizeNomeChatLink("https://smp.nome.im/c#hosted") == "https://smp.nome.im/c#hosted")
        expect("legacy hosted short link keeps its resolver", normalizeNomeChatLink("https://smp6.simplex.im/a#hosted") == "https://smp6.simplex.im/a#hosted")
        expect("legacy hosted channel keeps its resolver for Core", normalizeNomeChatLink("https://smp6.simplex.im/c#hosted") == "https://smp6.simplex.im/c#hosted")
        expect("Nome documentation URL is not intercepted", normalizeNomeChatLink("https://nome.im/docs/guide.html") == "https://nome.im/docs/guide.html")

        expect("known Nome contact path is recognized", isNomePublicChatLink("https://nome.im/contact#opaque"))
        expect("unknown Nome page is not recognized", !isNomePublicChatLink("https://nome.im/docs/guide.html"))
        expect("nested chat-like path is not recognized", !isRecognizedPublicChatLink("https://nome.im/contact/foo"))
        expect("Nome public file is recognized", isRecognizedPublicFileLink("https://nome.im/file#opaque"))
        expect("Nome hosted short link is recognized", isRecognizedPublicChatLink("https://smp.nome.im/i#hosted"))
        expect("Nome hosted channel is recognized", isRecognizedPublicChatLink("https://smp.nome.im/c#hosted"))
        expect("legacy hosted short link is recognized", isRecognizedPublicChatLink("https://smp6.simplex.im/a#hosted"))
        expect("legacy hosted channel is recognized", isRecognizedPublicChatLink("https://smp6.simplex.im/c#hosted"))
        expect("unknown hosted path is not recognized", !isRecognizedPublicChatLink("https://smp.nome.im/x#hosted"))
        expect("nested hosted channel path is not recognized", !isRecognizedPublicChatLink("https://smp.nome.im/c/extra#hosted"))
        expect("nested file page is not recognized", !isRecognizedPublicFileLink("https://nome.im/file/bar"))
        runCreatedConnectionLinkSourceRegressionTest()
        runIOSRoutingSourceRegressionTests()
        runAssociatedDomainsSourceRegressionTests()

        guard failures.isEmpty else {
            failures.forEach { fputs("FAIL: \($0)\n", stderr) }
            exit(1)
        }
        print("PASS: NomeLinkBranding (41 cases)")
    }

    private static func runCreatedConnectionLinkSourceRegressionTest() {
        let apiTypesURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("SimpleXChat/APITypes.swift")
        let source = try? String(contentsOf: apiTypesURL, encoding: .utf8)
        expect(
            "Core short links pass through Nome branding",
            source?.contains("connShortLink.map(simplexChatLink) ?? simplexChatLink(connFullLink)") == true
        )
    }

    private static func runIOSRoutingSourceRegressionTests() {
        let iosRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let messageSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Views/Chat/ChatItem/MsgContentView.swift"),
            encoding: .utf8
        )
        let shareSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Views/Helpers/ShareSheet.swift"),
            encoding: .utf8
        )
        let composeSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Views/Chat/ComposeMessage/ComposeView.swift"),
            encoding: .utf8
        )
        let newChatSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Views/NewChat/NewChatView.swift"),
            encoding: .utf8
        )
        let apiSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("SimpleXChat/API.swift"),
            encoding: .utf8
        )
        let contentSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/ContentView.swift"),
            encoding: .utf8
        )
        expect(
            "Nome chat links route in-app before browser handling",
            messageSource?.contains("simplex || isRecognizedPublicChatLink(uri)") == true
        )
        expect(
            "shared Nome chat links route in-app",
            shareSource?.contains("isRecognizedPublicChatLink(s)") == true
        )
        expect(
            "Nome chat links do not request web previews",
            composeSource?.contains("isRecognizedPublicChatLink(link)") == true
        )
        expect(
            "pasted links restore the correct Core transport before target parsing",
            newChatSource?.contains("parseSimpleXMarkdown(normalizeNomeChatLink(str))") == true
        )
        expect(
            "paste and QR target parsing use the same normalized-link path",
            newChatSource?.components(separatedBy: "parseSimpleXMarkdown(normalizeNomeChatLink(str))").count == 3
        )
        expect(
            "sanitized Core links are presented under the Nome brand",
            apiSource?.contains("parsedUri.uriInfo?.sanitized = simplexChatLink(sanitized)") == true
        )
        expect(
            "hosted channel links reach the in-app connect flow",
            contentSource?.contains("path == \"/c\"") == true
        )
    }

    private static func runAssociatedDomainsSourceRegressionTests() {
        let entitlementsURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("SimpleX (iOS).entitlements")
        let source = try? String(contentsOf: entitlementsURL, encoding: .utf8)
        expect(
            "Nome hosted links have an explicit associated domain",
            source?.contains("<string>applinks:smp.nome.im</string>") == true
        )
        expect(
            "Nome hosted links support developer-mode association",
            source?.contains("<string>applinks:smp.nome.im?mode=developer</string>") == true
        )
        expect(
            "Nome associated domains do not use an unnecessary wildcard",
            source?.contains("applinks:*.nome.im") == false
        )
    }

    private static func expect(_ name: String, _ condition: @autoclosure () -> Bool) {
        if !condition() { failures.append(name) }
    }
}
