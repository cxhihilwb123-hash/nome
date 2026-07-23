package chat.simplex.common.ui.nome.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import chat.simplex.common.R

private const val NomeBrandLockupAspectRatio = 700f / 285f

@Composable
fun NomeBrandLockup(
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
) {
  Image(
    painter =
      painterResource(
        if (MaterialTheme.colors.isLight) {
          R.drawable.nome_header_logo
        } else {
          R.drawable.nome_header_logo_dark
        },
      ),
    contentDescription = contentDescription,
    contentScale = ContentScale.Fit,
    modifier = modifier.aspectRatio(NomeBrandLockupAspectRatio),
  )
}
