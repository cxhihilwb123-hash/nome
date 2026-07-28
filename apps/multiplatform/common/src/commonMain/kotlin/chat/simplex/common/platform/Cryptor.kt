package chat.simplex.common.platform

interface CryptorInterface {
  fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String?
  fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray>
  fun deleteKey(alias: String)
}

/**
 * A credential was configured but its protected value cannot be recovered.
 * Callers must not silently substitute a default endpoint or empty secret.
 */
class CredentialUnavailable(
  credentialKind: String,
  cause: Throwable? = null,
) : IllegalStateException("$credentialKind credential is unavailable", cause)

expect val cryptor: CryptorInterface
