package chat.simplex.app.nome.connection

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPreviewRouteBoundaryTest {
  @Test
  fun onlyOpenUriCallerSelectsConnectionPreviewPolicy() {
    val commonMain = findCommonMain()
    val files = listOf(
      "views/newchat/NewChatSheet.kt",
      "views/newchat/NewChatView.kt",
      "views/chatlist/ChatListView.kt",
      "views/chatlist/ChatPreviewView.kt",
      "views/chat/item/FramedItemView.kt",
      "views/chat/group/GroupMemberInfoView.kt",
    ).map {
      File(
        commonMain,
        "kotlin/chat/simplex/common/$it",
      )
    }
    assertTrue(files.all(File::isFile))

    val sources = files.associateWith(File::readText)
    val callCount = sources.values.sumOf {
      Regex("""\bplanAndConnect\s*\(""").findAll(it).count()
    }
    val policySelectionFiles = sources.filterValues {
      it.contains("connectionPreviewEntryPolicy(")
    }.keys

    assertEquals(7, callCount)
    assertEquals(
      listOf("ChatListView.kt"),
      policySelectionFiles.map {
        it.name ?: error("Source file has no name: $it")
      },
    )
    assertEquals(
      1,
      Regex("""\bconnectionPreviewEntryPolicy\s*\(""")
        .findAll(sources.getValue(policySelectionFiles.single()))
        .count(),
    )
  }

  @Test
  fun ingressProvenanceIsCreatedAndForwardedAtEachBoundary() {
    val commonMain = findCommonMain()
    val commonModule = requireNotNull(
      commonMain.canonicalFile.parentFile?.parentFile,
    )
    val multiplatformRoot = requireNotNull(
      commonModule.parentFile,
    )
    val mainActivity = File(
      multiplatformRoot,
      "android/src/main/java/chat/simplex/app/MainActivity.kt",
    ).readText()
    val app = File(
      commonMain,
      "kotlin/chat/simplex/common/App.kt",
    ).readText()
    val utils = File(
      commonMain,
      "kotlin/chat/simplex/common/views/helpers/Utils.kt",
    ).readText()
    val chatList = File(
      commonMain,
      "kotlin/chat/simplex/common/views/chatlist/ChatListView.kt",
    ).readText()

    assertTrue(
      mainActivity.contains(
        "source = AppOpenUrlSource.ExternalActionView",
      ),
    )
    assertTrue(
      utils.contains(
        "source = AppOpenUrlSource.InternalVerified",
      ),
    )
    assertTrue(app.contains("source = pendingUrl.source"))
    assertTrue(
      chatList.contains(
        "connectionPreviewEntryPolicy(source, appPlatform.isAndroid)",
      ),
    )
  }

  private fun findCommonMain(): File {
    var current = File(
      requireNotNull(System.getProperty("user.dir")),
    ).absoluteFile
    repeat(8) {
      val candidate = File(current, "../common/src/commonMain")
        .canonicalFile
      if (candidate.isDirectory) return candidate
      current = current.parentFile
        ?: error("Unable to locate common/src/commonMain")
    }
    error("Unable to locate common/src/commonMain")
  }
}
