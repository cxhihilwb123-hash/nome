package chat.simplex.common.views.newchat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.color.ColorFormat
import boofcv.android.ConvertCameraImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import chat.simplex.common.helpers.openAppSettingsInSystem
import chat.simplex.common.platform.TAG
import chat.simplex.common.ui.theme.DEFAULT_PADDING_HALF
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.ListenableFuture
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

// Adapted from learntodroid - https://gist.github.com/learntodroid/8f839be0b29d0378f843af70607bd7f5

@Composable
actual fun QRCodeScanner(
  showQRCodeScanner: MutableState<Boolean>,
  padding: PaddingValues,
  onBarcode: suspend (String) -> Boolean
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val preview = remember { mutableStateOf<Preview?>(null) }
  val contactLink = remember { AtomicReference("") }
  val checkingLink = remember { AtomicBoolean(false) }
  val disposed = remember(lifecycleOwner) {
    AtomicBoolean(false)
  }
  val cameraProvider = remember(lifecycleOwner) {
    AtomicReference<ProcessCameraProvider?>(null)
  }
  val cameraExecutor =
    remember(lifecycleOwner) {
      Executors.newSingleThreadExecutor()
    }

  val cameraProviderFuture by produceState<ListenableFuture<ProcessCameraProvider>?>(initialValue = null) {
    value = ProcessCameraProvider.getInstance(context)
  }

  DisposableEffect(lifecycleOwner) {
    disposed.set(false)
    onDispose {
      disposed.set(true)
      cameraProvider.getAndSet(null)?.unbindAll()
      cameraExecutor.shutdownNow()
    }
  }

  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val modifier = Modifier
      .padding(padding)
      .clipToBounds()
      .widthIn(max = 400.dp)
      .aspectRatio(1f)
    val showScanner = remember { showQRCodeScanner }
    if (showScanner.value && cameraPermissionState.status == PermissionStatus.Granted) {
      AndroidView(
        factory = { AndroidViewContext ->
          PreviewView(AndroidViewContext).apply {
            this.scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          }
        },
        modifier = modifier
      ) { previewView ->
        val providerFuture =
          cameraProviderFuture ?: return@AndroidView
        val cameraSelector: CameraSelector = CameraSelector.Builder()
          .requireLensFacing(CameraSelector.LENS_FACING_BACK)
          .build()
        providerFuture.addListener({
          if (disposed.get()) {
            return@addListener
          }
          val provider =
            try {
              providerFuture.get()
            } catch (e: Exception) {
              Log.d(
                TAG,
                "CameraProvider: ${e.localizedMessage}",
              )
              return@addListener
            }
          if (disposed.get()) {
            return@addListener
          }
          cameraProvider.set(provider)
          preview.value = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }
          val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)
          suspend fun getQR(imageProxy: ImageProxy) {
            detector.process(imageProxyToGrayU8(imageProxy))
            val found = detector.detections
            val qr = found.firstOrNull()
            if (qr != null) {
              if (qr.message != contactLink.get()) {
                // Make sure link is new and not a repeat if that link was handled successfully
                if (onBarcode(qr.message)) {
                  contactLink.set(qr.message)
                }
                // just some delay to not spam endlessly with alert in case the user scan something wrong, and it fails fast
                // (for example, scan user's address while verifying contact code - it prevents alert spam)
                delay(1000)
              }
            }
          }

          val imageAnalyzer = ImageAnalysis.Analyzer { proxy ->
            if (disposed.get()) {
              proxy.close()
              return@Analyzer
            }
            if (!checkingLink.compareAndSet(false, true)) {
              proxy.close()
              return@Analyzer
            }
            withApi {
              try {
                getQR(proxy)
              } finally {
                checkingLink.set(false)
                proxy.close()
              }
            }
          }
          val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(1)
            .build()
            .also { it.setAnalyzer(cameraExecutor, imageAnalyzer) }
          try {
            if (
              disposed.get() ||
                cameraExecutor.isShutdown
            ) {
              imageAnalysis.clearAnalyzer()
              return@addListener
            }
            provider.unbindAll()
            if (disposed.get()) {
              imageAnalysis.clearAnalyzer()
              return@addListener
            }
            provider.bindToLifecycle(
              lifecycleOwner,
              cameraSelector,
              preview.value,
              imageAnalysis,
            )
          } catch (e: Exception) {
            Log.d(TAG, "CameraPreview: ${e.localizedMessage}")
          }
        }, ContextCompat.getMainExecutor(context))
      }
    } else {
      val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.9f),
        contentColor = MaterialTheme.colors.primary,
        disabledBackgroundColor = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.9f),
        disabledContentColor = MaterialTheme.colors.primary,
      )
      var permissionRequested by rememberSaveable { mutableStateOf(false) }
      val hasCamera =
        context.packageManager.hasSystemFeature(
          PackageManager.FEATURE_CAMERA_ANY,
        )
      val denied =
        cameraPermissionState.status as? PermissionStatus.Denied
      when {
        !hasCamera -> {
          Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            colors = buttonColors,
          ) {
            Text(
              stringResource(
                MR.strings.camera_not_available,
              ),
            )
          }
        }
        denied != null && !permissionRequested && showScanner.value -> {
          LaunchedEffect(Unit) {
            permissionRequested = true
            cameraPermissionState.launchPermissionRequest()
          }
        }
        denied != null -> {
          val openSettings =
            permissionRequested &&
              !denied.shouldShowRationale
          Button(
            onClick = {
              if (openSettings) {
                context.openAppSettingsInSystem()
              } else {
                permissionRequested = true
                cameraPermissionState
                  .launchPermissionRequest()
              }
            },
            modifier = modifier,
            colors = buttonColors,
          ) {
            Icon(painterResource(MR.images.ic_camera_enhance), null)
            Spacer(Modifier.width(DEFAULT_PADDING_HALF))
            Text(
              stringResource(
                if (openSettings) {
                  MR.strings.permissions_open_settings
                } else {
                  MR.strings.enable_camera_access
                },
              ),
            )
          }
        }
        cameraPermissionState.status == PermissionStatus.Granted -> {
          Button({ showQRCodeScanner.value = true }, modifier = modifier, colors = buttonColors) {
            Icon(painterResource(MR.images.ic_qr_code), null)
            Spacer(Modifier.width(DEFAULT_PADDING_HALF))
            Text(stringResource(MR.strings.tap_to_scan))
          }
        }
      }
    }
  }
}

@SuppressLint("UnsafeOptInUsageError")
private fun imageProxyToGrayU8(img: ImageProxy) : GrayU8? {
  val image = img.image
  if (image != null) {
    val outImg = GrayU8()
    ConvertCameraImage.imageToBoof(image, ColorFormat.GRAY, outImg, null)
    return outImg
  }
  return null
}
