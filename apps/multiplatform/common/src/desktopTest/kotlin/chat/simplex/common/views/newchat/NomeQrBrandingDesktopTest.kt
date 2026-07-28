package chat.simplex.common.views.newchat

import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.fiducial.qrcode.QrCode
import boofcv.alg.fiducial.qrcode.QrCodeEncoder
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayU8
import chat.simplex.common.platform.addDesktopQrLogo
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NomeQrBrandingDesktopTest {
  @Test
  fun nomeLogoKeepsDesktopQrCodesDecodable() {
    assertBrandedQr(
      content = "nome:short-invitation",
      errorLevel = QrCode.ErrorLevel.M,
      logoSize = 0.21f,
    )
    assertBrandedQr(
      content = "nome:long-invitation:" + "0123456789abcdef".repeat(24),
      errorLevel = QrCode.ErrorLevel.L,
      logoSize = 0.16f,
    )
  }

  private fun assertBrandedQr(
    content: String,
    errorLevel: QrCode.ErrorLevel,
    logoSize: Float,
  ) {
    val image = addDesktopQrLogo(qrCodeImage(content, errorLevel), logoSize)
    val centerStart = image.width * 3 / 8
    val centerEnd = image.width * 5 / 8
    var nomeGreenPixels = 0
    for (y in centerStart until centerEnd) {
      for (x in centerStart until centerEnd) {
        val pixel = Color(image.getRGB(x, y), true)
        if (pixel.green > 110 && pixel.green > pixel.red * 2 && pixel.green > pixel.blue * 1.3) {
          nomeGreenPixels++
        }
      }
    }
    assertTrue(nomeGreenPixels > 500, "QR center should contain the Nome green mark")

    val scanImage = BufferedImage(1152, 1152, BufferedImage.TYPE_INT_RGB)
    val scanGraphics = scanImage.createGraphics()
    scanGraphics.color = Color.WHITE
    scanGraphics.fillRect(0, 0, scanImage.width, scanImage.height)
    scanGraphics.drawImage(image, 64, 64, null)
    scanGraphics.dispose()
    val gray = ConvertBufferedImage.convertFromSingle(scanImage, null, GrayU8::class.java)
    val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)
    detector.process(gray)
    assertEquals(content, detector.detections.singleOrNull()?.message)
  }

  private fun qrCodeImage(content: String, errorLevel: QrCode.ErrorLevel): BufferedImage {
    val qrCode = QrCodeEncoder().addAutomatic(content).setError(errorLevel).fixate()
    val numModules = QrCode.totalModules(qrCode.version)
    val renderer = QrCodeGeneratorImage(1024 / numModules + 1)
    renderer.borderModule = 0
    renderer.render(qrCode)
    val source = ConvertBufferedImage.extractBuffered(renderer.gray)
    val scaled = BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB)
    val graphics = scaled.createGraphics()
    graphics.setRenderingHint(
      RenderingHints.KEY_INTERPOLATION,
      RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    )
    graphics.drawImage(source, 0, 0, scaled.width, scaled.height, null)
    graphics.dispose()
    return scaled
  }
}
