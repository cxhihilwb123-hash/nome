package chat.simplex.common.platform

import chat.simplex.common.model.ChatController.appPrefs

actual object Log {
  actual fun d(tag: String, text: String) { if (shouldLogDesktop(LogLevel.DEBUG)) println("D: $text") }
  actual fun e(tag: String, text: String) { if (shouldLogDesktop(LogLevel.ERROR)) println("E: $text") }
  actual fun i(tag: String, text: String) { if (shouldLogDesktop(LogLevel.INFO)) println("I: $text") }
  actual fun w(tag: String, text: String) { if (shouldLogDesktop(LogLevel.WARNING)) println("W: $text") }
}

private fun shouldLogDesktop(level: LogLevel): Boolean =
  shouldLogDesktop(level, appPrefs.logLevel.get(), appPrefs.developerTools.get())

internal fun shouldLogDesktop(level: LogLevel, configuredLevel: LogLevel, developerTools: Boolean): Boolean =
  developerTools && configuredLevel <= level
