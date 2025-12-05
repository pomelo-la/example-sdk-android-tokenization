package com.pomelo.tkn_sdk.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pomelo.tkn_sdk.data.model.CardDto

@Composable
fun CardCarousel(cards: List<CardDto>, pagerState: PagerState, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), pageSpacing = 16.dp) {
        page ->
      PomeloCardComposable(card = cards[page])
    }

    Spacer(modifier = Modifier.height(16.dp))

    PageIndicator(pageCount = cards.size, currentPage = pagerState.currentPage)
  }
}
