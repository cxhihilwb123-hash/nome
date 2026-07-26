//
//  KeyChain.swift
//  SimpleXChat
//
//  Created by Evgeny on 04/09/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//

import Foundation
import Security

private let ACCESS_POLICY: CFString = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
private let ACCESS_GROUP: String = "5NN7GUYB6T.chat.simplex.app"
private let NOME_ACTIVATION_ACCESS_GROUP: String = "5NN7GUYB6T.chat.simplex.nome.activation"
private let DATABASE_PASSWORD_ITEM: String = "databasePassword"
private let APP_PASSWORD_ITEM: String = "appPassword"
private let SELF_DESTRUCT_PASSWORD_ITEM: String = "selfDestructPassword"
private let NOME_ACTIVATION_TOKEN_ITEM: String = "nomeActivationToken"
private let NOME_ACTIVATION_INSTALLATION_ID_ITEM: String = "nomeActivationInstallationId"
private let NOME_ACTIVATION_IDEMPOTENCY_KEY_ITEM: String = "nomeActivationIdempotencyKey"

public let kcDatabasePassword = KeyChainItem(forKey: DATABASE_PASSWORD_ITEM)

public let kcAppPassword = KeyChainItem(forKey: APP_PASSWORD_ITEM)

public let kcSelfDestructPassword = KeyChainItem(forKey: SELF_DESTRUCT_PASSWORD_ITEM)

/// Nome activation credentials are device-local and intentionally excluded from
/// App Group preferences so extensions cannot read the bearer token.
public let kcNomeActivationToken = KeyChainItem(
    forKey: NOME_ACTIVATION_TOKEN_ITEM,
    accessGroup: NOME_ACTIVATION_ACCESS_GROUP
)

public let kcNomeActivationInstallationId = KeyChainItem(
    forKey: NOME_ACTIVATION_INSTALLATION_ID_ITEM,
    accessGroup: NOME_ACTIVATION_ACCESS_GROUP
)

public let kcNomeActivationIdempotencyKey = KeyChainItem(
    forKey: NOME_ACTIVATION_IDEMPOTENCY_KEY_ITEM,
    accessGroup: NOME_ACTIVATION_ACCESS_GROUP
)

public struct KeyChainItem {
    var forKey: String
    private var accessGroup: String = ACCESS_GROUP

    fileprivate init(forKey: String, accessGroup: String = ACCESS_GROUP) {
        self.forKey = forKey
        self.accessGroup = accessGroup
    }

    public func get() -> String? {
        getItemString(forKey: forKey, accessGroup: accessGroup)
    }

    public func set(_ value: String) -> Bool {
        setItemString(value, forKey: forKey, accessGroup: accessGroup)
    }

    public func remove() -> Bool {
        deleteItem(forKey: forKey, accessGroup: accessGroup)
    }
}

func randomDatabasePassword() -> String {
    var keyData = Data(count: 32)
    let status = keyData.withUnsafeMutableBytes {
        SecRandomCopyBytes(kSecRandomDefault, 32, $0.baseAddress!)
    }
    if status == errSecSuccess {
        return keyData.base64EncodedString()
    } else {
        logger.error("randomDatabasePassword: error \(status)")
        return ""
    }
}

private func getItemData(forKey key: String, accessGroup: String) -> Data? {
    var query = baseItemQuery(forKey: key, accessGroup: accessGroup)
    query[kSecMatchLimit] = kSecMatchLimitOne
    query[kSecReturnData] = true as AnyObject?

    var dataRef: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &dataRef)
    if status != errSecSuccess && status != errSecItemNotFound {
        logger.error("getItemData: error getting data for key '\(key)', error: \(status)")
    }
    return dataRef as? Data
}

private func getItemString(forKey key: String, accessGroup: String) -> String? {
    if let data = getItemData(forKey: key, accessGroup: accessGroup) {
        return NSString(data: data, encoding: String.Encoding.utf8.rawValue) as? String
    }
    return nil
}

private func setItemData(_ data: Data, forKey key: String, accessGroup: String) -> Bool {
    var query = baseItemQuery(forKey: key, accessGroup: accessGroup)
    var update = [NSString : AnyObject]()
    update[kSecValueData] = data as AnyObject?
    update[kSecAttrAccessible] = ACCESS_POLICY
    var status: OSStatus
    if getItemData(forKey: key, accessGroup: accessGroup) == nil {
        for (key, value) in update { query[key] = value }
        status = SecItemAdd(query as CFDictionary, nil)
    } else {
        status = SecItemUpdate(query as CFDictionary, update as CFDictionary)
    }
    if status != errSecSuccess {
        logger.error("setItemData: error setting data for key '\(key)', error: \(status)")
        return false
    }
    return true
}

private func setItemString(_ s: String, forKey key: String, accessGroup: String) -> Bool {
    if let data = s.data(using: .utf8) {
        return setItemData(data, forKey: key, accessGroup: accessGroup)
    }
    return false
}

private func deleteItem(forKey key: String, accessGroup: String) -> Bool {
    let query = baseItemQuery(forKey: key, accessGroup: accessGroup)
    if getItemData(forKey: key, accessGroup: accessGroup) != nil {
        let status = SecItemDelete(query as CFDictionary)
        if status != errSecSuccess {
            logger.error("deleteItem: error deleting data for key '\(key)', error: \(status)")
            return false
        }
    }
    return true
}

private func baseItemQuery(forKey key: String, accessGroup: String) -> [NSString : AnyObject] {
    var query = [NSString : AnyObject]()
    query[kSecClass] = kSecClassGenericPassword
    query[kSecAttrAccount] = key as AnyObject?
    #if os(iOS) && !targetEnvironment(simulator)
        query[kSecAttrAccessGroup] = accessGroup
    #endif
    return query
}
