package chat.simplex.common.views.database

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformDatabaseRootRoute(
  facts: NomeDatabaseRootFacts,
  allowSensitiveContent: Boolean,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}

actual fun platformDatabaseKeyReadState(): AndroidDatabaseKeyReadState? = null

actual fun clearPlatformDatabaseKeyReadState() {}
