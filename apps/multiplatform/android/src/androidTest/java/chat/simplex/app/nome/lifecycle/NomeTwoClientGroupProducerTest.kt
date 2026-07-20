package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.APIJoinGroupResult
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatPagination
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.GroupMemberStatus
import chat.simplex.common.model.GroupPreference
import chat.simplex.common.model.GroupPreferences
import chat.simplex.common.model.GroupProfile
import chat.simplex.common.model.GroupFeatureEnabled
import chat.simplex.common.model.MsgContent
import chat.simplex.common.views.chatlist.acceptGroupInvitationAlertDialog
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientGroupProducerTest {
  @Test
  fun realInvitationJoinAndGroupMessageReachTheControlledPeer() {
    val arguments = InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(arguments)
        ?: return
    val groupName =
      requireGroupNameArgument(arguments)
    val marker =
      requireMarkerArgument(arguments)
    val port =
      bridgePortArgument(arguments)
    val captureInvitation =
      strictBooleanArgument(
        arguments = arguments,
        argumentName = CAPTURE_INVITATION_ARGUMENT,
      )
    val reuseExistingGroup =
      strictBooleanArgument(
        arguments = arguments,
        argumentName = REUSE_EXISTING_GROUP_ARGUMENT,
      )
    val invitationOnly =
      strictBooleanArgument(
        arguments = arguments,
        argumentName = INVITATION_ONLY_ARGUMENT,
      )

    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          InstrumentationRegistry.getInstrumentation().targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          readyDirectChats().isNotEmpty()
      }
      when (role) {
        ProducerRole.Source -> {
          if (!reuseExistingGroup) {
            val direct =
              requireNotNull(
                newestReadyDirectChat(),
              )
            val contact =
              (direct.chatInfo as ChatInfo.Direct).contact
            val group =
              runBlocking {
                ChatModel.controller.apiNewGroup(
                  rh = direct.remoteHostId,
                  incognito = false,
                  groupProfile =
                    GroupProfile(
                      displayName = groupName,
                      fullName = "",
                      shortDescr = null,
                      groupPreferences =
                        GroupPreferences(
                          history =
                            GroupPreference(
                              GroupFeatureEnabled.ON,
                            ),
                        ),
                    ),
                )
              }
            assertNotNull(
              "The official group producer must create the controlled private group",
              group,
            )
            val member =
              runBlocking {
                ChatModel.controller.apiAddMember(
                  rh = direct.remoteHostId,
                  groupId = requireNotNull(group).groupId,
                  contactId = contact.apiId,
                  memberRole = GroupMemberRole.Member,
                )
              }
            assertNotNull(
              "The official group producer must create the peer invitation",
              member,
            )
          } else {
            waitUntil(READY_TIMEOUT_MILLIS) {
              readyGroup(groupName) != null
            }
          }
          if (!invitationOnly) {
            waitUntil(GROUP_TIMEOUT_MILLIS) {
              groupContainsMarker(
                groupName = groupName,
                marker = marker,
              )
            }
          }
        }

        ProducerRole.Target -> {
          waitUntil(GROUP_TIMEOUT_MILLIS) {
            invitedGroup(groupName) != null
          }
          val invitation =
            requireNotNull(
              invitedGroup(groupName),
            )
          if (captureInvitation) {
            captureProductionInvitationPage(
              scenario = scenario,
              invitation = invitation,
            )
          }
          if (!invitationOnly) {
            val result =
              runBlocking {
                ChatModel.controller.apiJoinGroupResult(
                  rh = invitation.remoteHostId,
                  groupId =
                    (invitation.chatInfo as ChatInfo.Group)
                      .groupInfo
                      .groupId,
                )
              }
            assertTrue(
              "The official group join must accept the controlled invitation",
              result is APIJoinGroupResult.Accepted,
            )
            waitUntil(GROUP_TIMEOUT_MILLIS) {
              readyGroup(groupName) != null
            }
            val joined =
              requireNotNull(
                readyGroup(groupName),
              )
            val info =
              joined.chatInfo as ChatInfo.Group
            val sent =
              runBlocking {
                ChatModel.controller.apiSendMessages(
                  rh = joined.remoteHostId,
                  type = info.chatType,
                  id = info.apiId,
                  scope = info.groupChatScope(),
                  sendAsGroup = info.sendAsGroup,
                  composedMessages =
                    listOf(
                      ComposedMessage(
                        fileSource = null,
                        quotedItemId = null,
                        msgContent = MsgContent.MCText(marker),
                        mentions = emptyMap(),
                      ),
                    ),
                )
              }
            assertTrue(
              "The joined controlled member must create a sender-side group item",
              !sent.isNullOrEmpty(),
            )
          }
        }
      }

      coordinateWithPeer(
        port = port,
        role = role,
      )
      Log.i(
        TAG,
        if (invitationOnly) {
          "role=${role.argument} peerObservedGroupInvitation=true"
        } else {
          "role=${role.argument} groupInvitationJoinAndPeerMessage=true"
        },
      )
    } finally {
      scenario.close()
    }
  }

  private fun readyDirectChats(): List<Chat> =
    ChatModel.chats.value.filter {
      (it.chatInfo as? ChatInfo.Direct)
        ?.ready == true
    }

  private fun newestReadyDirectChat(): Chat? =
    readyDirectChats()
      .maxByOrNull {
        it.chatInfo.createdAt
      }

  private fun invitedGroup(
    groupName: String,
  ): Chat? =
    ChatModel.chats.value.firstOrNull {
      val info =
        it.chatInfo as? ChatInfo.Group
      info?.groupInfo?.groupProfile?.displayName == groupName &&
        info.groupInfo.membership.memberStatus == GroupMemberStatus.MemInvited
    }

  private fun readyGroup(
    groupName: String,
  ): Chat? =
    ChatModel.chats.value.firstOrNull {
      val info =
        it.chatInfo as? ChatInfo.Group
      info?.groupInfo?.groupProfile?.displayName == groupName &&
        info.ready
    }

  private fun groupContainsMarker(
    groupName: String,
    marker: String,
  ): Boolean {
    val group =
      readyGroup(groupName)
        ?: return false
    val info =
      group.chatInfo as ChatInfo.Group
    return runBlocking {
      ChatModel.controller.apiGetChat(
        rh = group.remoteHostId,
        type = info.chatType,
        id = info.apiId,
        scope = info.groupChatScope(),
        pagination = ChatPagination.Last(CHAT_ITEM_LIMIT),
      )?.first?.chatItems?.any {
        it.text == marker
      } == true
    }
  }

  private fun captureProductionInvitationPage(
    scenario: ActivityScenario<MainActivity>,
    invitation: Chat,
  ) {
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val info =
      invitation.chatInfo as ChatInfo.Group
    scenario.onActivity {
      acceptGroupInvitationAlertDialog(
        rhId = invitation.remoteHostId,
        groupInfo = info.groupInfo,
        chatModel = ChatModel,
      )
    }
    waitUntil(UI_TIMEOUT_MILLIS) {
      instrumentation.uiAutomation.rootInActiveWindow
        ?.packageName
        ?.toString() ==
        instrumentation.targetContext.packageName
    }
    scenario.onActivity {
      it.window.clearFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
      )
    }
    try {
      instrumentation.waitForIdleSync()
      SystemClock.sleep(STABLE_FRAME_MILLIS)
      val screenshot =
        requireNotNull(
          instrumentation.uiAutomation.takeScreenshot(),
        )
      try {
        val directory =
          requireNotNull(
            instrumentation.targetContext.getExternalFilesDir(
              EVIDENCE_DIRECTORY,
            ),
          )
        assertTrue(
          "The production capture directory must be available",
          directory.isDirectory ||
            directory.mkdirs(),
        )
        val destination =
          File(
            directory,
            INVITATION_CAPTURE_FILE,
          )
        FileOutputStream(
          destination,
          false,
        ).use { output ->
          assertTrue(
            "The production invitation screenshot must be encoded",
            screenshot.compress(
              android.graphics.Bitmap.CompressFormat.PNG,
              PNG_QUALITY,
              output,
            ),
          )
        }
        assertTrue(
          "The production invitation screenshot must be non-empty",
          destination.isFile &&
            destination.length() > 0L,
        )
      } finally {
        screenshot.recycle()
      }
    } finally {
      scenario.onActivity {
        it.window.addFlags(
          WindowManager.LayoutParams.FLAG_SECURE,
        )
      }
    }
  }

  private fun coordinateWithPeer(
    port: Int,
    role: ProducerRole,
  ) {
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(role.wireValue)
      output.flush()
      assertTrue(
        "Both controlled clients must finish the same group producer attempt",
        socket.getInputStream().read() == BRIDGE_ACK,
      )
    }
  }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = GROUP_TIMEOUT_MILLIS.toInt()
      connect(
        InetSocketAddress(
          BRIDGE_HOST,
          port,
        ),
        BRIDGE_CONNECT_TIMEOUT_MILLIS,
      )
    }

  private fun waitUntil(
    timeoutMillis: Long,
    predicate: () -> Boolean,
  ) {
    val deadline =
      SystemClock.elapsedRealtime() +
        timeoutMillis
    while (
      !predicate() &&
      SystemClock.elapsedRealtime() < deadline
    ) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue(
      "Timed out waiting for the controlled two-client group state",
      predicate(),
    )
  }

  private enum class ProducerRole(
    val argument: String,
    val wireValue: Int,
  ) {
    Source("source", 1),
    Target("target", 2),
  }

  private fun controlledProducerRoleOrSkip(
    arguments: android.os.Bundle,
  ): ProducerRole? {
    val roleArgument =
      arguments.getString(
        PRODUCER_ROLE_ARGUMENT,
      )
    if (roleArgument == null) {
      if (
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ) == "true"
      ) {
        return null
      }
      error(
        "Missing $PRODUCER_ROLE_ARGUMENT; " +
          "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true to bypass this controlled harness",
      )
    }
    return ProducerRole.entries.firstOrNull {
      it.argument == roleArgument
    } ?: error(
      "Invalid $PRODUCER_ROLE_ARGUMENT=$roleArgument",
    )
  }

  private fun requireGroupNameArgument(
    arguments: android.os.Bundle,
  ): String {
    val groupName =
      arguments.getString(
        GROUP_NAME_ARGUMENT,
      ) ?: error(
        "Missing $GROUP_NAME_ARGUMENT",
      )
    if (
      groupName.startsWith(GROUP_NAME_PREFIX) &&
      groupName.length <= MAX_GROUP_NAME_LENGTH
    ) {
      return groupName
    }
    error(
      "Invalid $GROUP_NAME_ARGUMENT=$groupName",
    )
  }

  private fun requireMarkerArgument(
    arguments: android.os.Bundle,
  ): String {
    val marker =
      arguments.getString(
        MESSAGE_MARKER_ARGUMENT,
      ) ?: error(
        "Missing $MESSAGE_MARKER_ARGUMENT",
      )
    if (
      marker.startsWith(
        MESSAGE_MARKER_PREFIX,
      ) &&
      marker.length <= MAX_MARKER_LENGTH
    ) {
      return marker
    }
    error(
      "Invalid $MESSAGE_MARKER_ARGUMENT=$marker",
    )
  }

  private fun bridgePortArgument(
    arguments: android.os.Bundle,
  ): Int {
    val rawPort =
      arguments.getString(
        PRODUCER_PORT_ARGUMENT,
      ) ?: return DEFAULT_BRIDGE_PORT
    return rawPort.toIntOrNull()
      ?: error(
        "Invalid $PRODUCER_PORT_ARGUMENT=$rawPort",
      )
  }

  private fun strictBooleanArgument(
    arguments: android.os.Bundle,
    argumentName: String,
  ): Boolean {
    val rawValue =
      arguments.getString(argumentName)
        ?: return false
    return rawValue.toBooleanStrictOrNull()
      ?: error(
        "Invalid $argumentName=$rawValue",
      )
  }

  private companion object {
    const val TAG = "NomeTwoClientGroup"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val GROUP_NAME_ARGUMENT =
      "nomeGroupName"
    const val MESSAGE_MARKER_ARGUMENT =
      "nomeMessageMarker"
    const val CAPTURE_INVITATION_ARGUMENT =
      "nomeCaptureInvitation"
    const val REUSE_EXISTING_GROUP_ARGUMENT =
      "nomeReuseExistingGroup"
    const val INVITATION_ONLY_ARGUMENT =
      "nomeInvitationOnly"
    const val GROUP_NAME_PREFIX =
      "NomeGroup"
    const val MESSAGE_MARKER_PREFIX =
      "NomeGroupMessage-"
    const val BRIDGE_HOST = "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT = 27192
    const val BRIDGE_ACK = 1
    const val MAX_GROUP_NAME_LENGTH = 48
    const val MAX_MARKER_LENGTH = 96
    const val CHAT_ITEM_LIMIT = 100
    const val EVIDENCE_DIRECTORY =
      "nome-evidence"
    const val INVITATION_CAPTURE_FILE =
      "P16-production-invited-api35-zh-light-raw.png"
    const val PNG_QUALITY = 100
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS = 15_000
    const val UI_TIMEOUT_MILLIS = 30_000L
    const val STABLE_FRAME_MILLIS = 1_000L
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val GROUP_TIMEOUT_MILLIS = 300_000L
    const val POLL_MILLIS = 1_000L
  }
}
