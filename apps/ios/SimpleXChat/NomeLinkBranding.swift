import Foundation

private let nomePublicLinkPrefix = "https://nome.im/"
private let legacySimpleXPublicLinkPrefix = "https://simplex.chat/"
private let legacySimpleXPublicHTTPLinkPrefix = "http://simplex.chat/"
private let simplexInternalLinkPrefix = "simplex:/"

private let nomeHostedShortLinkPrefix = "https://smp.nome.im/"

private let hostedShortLinkPathSegments: Set<String> = ["a", "c", "g", "i", "r"]

private let nomeChatLinkPathSegments: Set<String> = [
    "a",
    "c",
    "call",
    "channel",
    "chat",
    "contact",
    "file",
    "g",
    "i",
    "invitation",
    "r",
]

private let legacyHostedShortLink = try! NSRegularExpression(
    pattern: #"(?i)^https://(?:smp(?:\d+)?\.simplex\.im|smp\.nome\.im)/((?:a|c|g|i|r)#.*)$"#
)

private func publicChatLinkSuffix(_ uri: String, prefix: String) -> String? {
    guard uri.range(of: prefix, options: [.anchored, .caseInsensitive]) != nil else { return nil }
    let suffix = String(uri.dropFirst(prefix.count))
    let segmentEnd = suffix.firstIndex(where: { $0 == "/" || $0 == "?" || $0 == "#" }) ?? suffix.endIndex
    let segment = String(suffix[..<segmentEnd])
    let delimiter = segmentEnd == suffix.endIndex ? nil : suffix[segmentEnd]
    guard nomeChatLinkPathSegments.contains(segment), delimiter == nil || delimiter == "?" || delimiter == "#" else {
        return nil
    }
    return suffix
}

private func recognizedPublicChatLinkSuffix(_ uri: String) -> String? {
    if let suffix = publicChatLinkSuffix(uri, prefix: nomePublicLinkPrefix)
        ?? publicChatLinkSuffix(uri, prefix: legacySimpleXPublicLinkPrefix)
        ?? publicChatLinkSuffix(uri, prefix: legacySimpleXPublicHTTPLinkPrefix) {
        return suffix
    }

    let fullRange = NSRange(uri.startIndex..<uri.endIndex, in: uri)
    guard let match = legacyHostedShortLink.firstMatch(in: uri, range: fullRange),
          match.range == fullRange,
          let suffixRange = Range(match.range(at: 1), in: uri) else {
        return nil
    }
    return String(uri[suffixRange])
}

private func hostedShortLinkSuffix(_ uri: String) -> String? {
    let fullRange = NSRange(uri.startIndex..<uri.endIndex, in: uri)
    guard let match = legacyHostedShortLink.firstMatch(in: uri, range: fullRange),
          match.range == fullRange,
          let suffixRange = Range(match.range(at: 1), in: uri) else {
        return nil
    }
    return String(uri[suffixRange])
}

private func firstPathSegment(_ suffix: String) -> String {
    let segmentEnd = suffix.firstIndex(where: { $0 == "/" || $0 == "?" || $0 == "#" }) ?? suffix.endIndex
    return String(suffix[..<segmentEnd])
}

private func nomeShareableLink(_ suffix: String) -> String {
    hostedShortLinkPathSegments.contains(firstPathSegment(suffix))
        ? nomeHostedShortLinkPrefix + suffix
        : nomePublicLinkPrefix + suffix
}

/// Presents all user-shareable connection links under the Nome brand.
public func simplexChatLink(_ uri: String) -> String {
    if hostedShortLinkSuffix(uri) != nil {
        return uri
    }
    if uri.hasPrefix(simplexInternalLinkPrefix) {
        return nomeShareableLink(String(uri.dropFirst(simplexInternalLinkPrefix.count)))
    }
    if let suffix = publicChatLinkSuffix(uri, prefix: nomePublicLinkPrefix)
        ?? publicChatLinkSuffix(uri, prefix: legacySimpleXPublicLinkPrefix)
        ?? publicChatLinkSuffix(uri, prefix: legacySimpleXPublicHTTPLinkPrefix) {
        return nomeShareableLink(suffix)
    }
    return uri
}

/// Converts branded links back to the exact transport form understood by the native Core.
/// Hosted short links retain a resolver host; reducing them to `simplex:/<path>#…` drops resolver data.
public func normalizeNomeChatLink(_ uri: String) -> String {
    if hostedShortLinkSuffix(uri) != nil {
        return uri
    }
    if let suffix = publicChatLinkSuffix(uri, prefix: nomePublicLinkPrefix) {
        return hostedShortLinkPathSegments.contains(firstPathSegment(suffix))
            ? nomeHostedShortLinkPrefix + suffix
            : simplexInternalLinkPrefix + suffix
    }
    if let suffix = publicChatLinkSuffix(uri, prefix: legacySimpleXPublicLinkPrefix)
        ?? publicChatLinkSuffix(uri, prefix: legacySimpleXPublicHTTPLinkPrefix) {
        return simplexInternalLinkPrefix + suffix
    }
    return uri
}

public func isNomePublicChatLink(_ uri: String) -> Bool {
    publicChatLinkSuffix(uri, prefix: nomePublicLinkPrefix) != nil
}

public func isRecognizedPublicChatLink(_ uri: String) -> Bool {
    recognizedPublicChatLinkSuffix(uri) != nil
}

public func isRecognizedPublicFileLink(_ uri: String) -> Bool {
    let normalized = normalizeNomeChatLink(uri)
    return normalized == "\(simplexInternalLinkPrefix)file"
        || normalized.hasPrefix("\(simplexInternalLinkPrefix)file#")
        || normalized.hasPrefix("\(simplexInternalLinkPrefix)file?")
}
