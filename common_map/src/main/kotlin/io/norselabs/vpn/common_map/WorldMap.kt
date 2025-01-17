package io.norselabs.vpn.common_map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.ln
import kotlin.math.tan

@Composable
fun WorldMap(
  lat: Double,
  long: Double,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val coordinates = remember { mutableStateOf(0f to 0f) }
  LaunchedEffect(lat, long) {
    coordinates.value = getCoordinates(latitude = lat, longitude = long)
  }

  val radiusPx = with(LocalDensity.current) { 3.dp.toPx() }
  val imgSize = remember { mutableStateOf(Size.Zero) }
  val viewSize = remember { mutableStateOf(Size.Zero) }
  val scale = remember {
    derivedStateOf {
      val scale = viewSize.value.height / imgSize.value.height
      scale.takeIf { !it.isNaN() && it.isFinite() } ?: 1f
    }
  }
  val pointOffset = remember {
    derivedStateOf {
      if (coordinates.value.first == 0f || coordinates.value.second == 0f) {
        Offset.Zero
      } else {
        Offset(
          x = viewSize.value.width * coordinates.value.first,
          y = imgSize.value.height * coordinates.value.second + (viewSize.value.height - imgSize.value.height) / 2,
        )
      }
    }
  }
  val widthExcessHalf = remember {
    derivedStateOf {
      (imgSize.value.width * scale.value - viewSize.value.width) / 2
    }
  }
  val transX = rememberSaveable(
    saver = screenAnimationSaver(),
  ) { Animatable(0f) }

  LaunchedEffect(pointOffset.value) {
    if (pointOffset.value == Offset.Zero) return@LaunchedEffect
    val x = pointOffset.value.x
    val zone = viewSize.value.width / 3
    val trans = when {
      x < zone -> widthExcessHalf.value
      x > 2 * zone -> widthExcessHalf.value * -1
      else -> 0f
    }
    transX.animateTo(
      targetValue = trans,
      animationSpec = tween(500),
    )
  }

  Image(
    colorFilter = ColorFilter.tint(color.copy(alpha = 0.15f)),
    painter = painterResource(R.drawable.img_map),
    contentDescription = null,
    contentScale = object : ContentScale {
      override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val scaleValue = dstSize.width / srcSize.width
        viewSize.value = dstSize
        imgSize.value = Size(srcSize.width * scaleValue, srcSize.height * scaleValue)
        return ScaleFactor(scaleValue, scaleValue)
      }
    },
    modifier = modifier
      .graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        translationX = transX.value
      }
      .drawWithContent {
        drawContent()
        val offset = pointOffset.value
        if (offset != Offset.Zero) {
          drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = radiusPx * 3,
            center = offset,
          )
          drawCircle(
            color = color,
            radius = radiusPx,
            center = offset,
          )
        }
      },
  )
}

private fun getCoordinates(latitude: Double, longitude: Double): Pair<Float, Float> {

  val mapWidthPx = 1068
  val mapHeightPx = 704
  val zeroX = 0.4981273408
  val zeroY = 0.6960227273
  val latMin = -58.55
  val latMax = 83.62
  val lonMin = -180.0
  val lonMax = 180.0

  val y = when {
    latitude > 0 -> {
      (mapHeightPx * zeroY) * (1 - (normalizeLatitude(latitude) / normalizeLatitude(latMax)))
    }

    latitude < 0 -> {
      (mapHeightPx * zeroY) + (
        (mapHeightPx - (mapHeightPx * zeroY)) * (normalizeLatitude(latitude) / normalizeLatitude(latMin))
        )
    }

    else -> 0.0
  }

  val x = when {
    longitude > 0 -> (mapWidthPx * zeroX) + ((mapWidthPx - (mapWidthPx * zeroX)) * (longitude / lonMax))
    longitude < 0 -> (mapWidthPx * zeroX) - ((mapWidthPx * zeroX) * (longitude / lonMin))
    else -> 0.0
  }

  return (x.toFloat() / mapWidthPx) to (y.toFloat() / mapHeightPx)
}

private fun normalizeLatitude(lat: Double): Double {
  return ln(tan(Math.PI / 4 + (lat * Math.PI / 180) / 2))
}

private fun screenAnimationSaver(): Saver<Animatable<Float, *>, Float> {
  return Saver(
    save = { it.value },
    restore = { Animatable(it) },
  )
}
