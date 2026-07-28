package chat.simplex.common.ui.theme

import chat.simplex.common.views.helpers.PresetWallpaper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NomeDefaultWallpaperTest {
  @Test
  fun desktopUsesNomeWallpaperWithoutRemovingSchoolPreset() {
    assertEquals(PresetWallpaper.NOME, defaultPresetWallpaper())
    assertEquals(PresetWallpaper.NOME, PresetWallpaper.from("nome"))
    assertEquals(PresetWallpaper.SCHOOL, PresetWallpaper.from("school"))
    assertNotEquals(PresetWallpaper.SCHOOL, defaultPresetWallpaper())
  }
}
