package com.pomelo.tkn_sdk.ui.composables

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.pomelo.sdk.pushprovisioning.ui.Brand
import com.pomelo.tkn_sdk.data.model.CardDto

private fun getImageUrlForBrand(brand: Brand): String =
    when (brand) {
      Brand.VISA -> "file:///android_asset/asset_card_1.png"
      Brand.MASTERCARD -> "file:///android_asset/asset_card_2.png"
    }

@Composable
fun PomeloCardComposable(card: CardDto) {
  val name = card.cardholderName
  val lastFour = card.lastFour
  val imageUrl = getImageUrlForBrand(card.brand)
  var bitmapState by remember { mutableStateOf<Bitmap?>(null) }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .then(if (imageUrl.isBlank()) Modifier.aspectRatio(1.586f) else Modifier),
      shape = RoundedCornerShape(8.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = if (imageUrl.isBlank()) Color.Black else Color.Transparent
          ),
  ) {
    var imageHeight by remember { mutableStateOf(200.dp) }
    val localDensity = LocalDensity.current

    Box(
        modifier =
            Modifier.onGloballyPositioned { coordinates ->
              imageHeight = with(localDensity) { coordinates.size.height.toDp() }
            }
    ) {
      Glide.with(LocalContext.current)
          .asBitmap()
          .load(imageUrl)
          .into(
              object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                  bitmapState = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
              }
          )

      bitmapState?.let {
        Image(
            contentDescription = "Card Background",
            bitmap = it.asImageBitmap(),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        )
      }

      Box(modifier = Modifier.height(imageHeight).padding(start = 20.dp, end = 20.dp)) {
        Column {
          Spacer(modifier = Modifier.height(72.dp))

          Text(
              text = name.uppercase(),
              color = Color.White,
              style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.25.sp),
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
              text = "•••• •••• •••• $lastFour",
              color = Color.White,
              style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.25.sp),
          )
        }
      }
    }
  }
}
