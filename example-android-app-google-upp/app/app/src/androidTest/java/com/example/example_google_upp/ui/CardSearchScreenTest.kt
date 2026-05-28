package com.example.example_google_upp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardSearchScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyStateIsVisibleWithoutInlineSearchForm() {
        composeRule.setContent {
            CardSearchScreen(
                uiState = CardSearchUiState(),
                isSearchSheetVisible = false,
                onSearch = { Result.success(Unit) },
                onDismissSearch = {},
            )
        }

        composeRule
            .onNodeWithText("Search for a card by ID to display it here.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Card ID").assertCountEquals(0)
    }

    @Test
    fun searchSheetShowsFormWhenVisible() {
        composeRule.setContent {
            CardSearchScreen(
                uiState = CardSearchUiState(),
                isSearchSheetVisible = true,
                onSearch = { Result.success(Unit) },
                onDismissSearch = {},
            )
        }

        composeRule.onNodeWithText("Card ID").assertIsDisplayed()
        composeRule.onNodeWithText("Run search").assertIsDisplayed()
    }

    @Test
    fun searchActionReceivesTypedQuery() {
        var searchedQuery: String? = null

        composeRule.setContent {
            CardSearchScreen(
                uiState = CardSearchUiState(),
                isSearchSheetVisible = true,
                onSearch = {
                    searchedQuery = it
                    Result.success(Unit)
                },
                onDismissSearch = {},
            )
        }

        composeRule.onNodeWithText("Card ID").performTextInput("crd-123")
        composeRule.onNodeWithText("Run search").performClick()

        composeRule.runOnIdle { assertEquals("crd-123", searchedQuery) }
    }

    @Test
    fun searchFailureShowsErrorInSheet() {
        composeRule.setContent {
            CardSearchScreen(
                uiState = CardSearchUiState(),
                isSearchSheetVisible = true,
                onSearch = { Result.failure(IllegalStateException("Card not found")) },
                onDismissSearch = {},
            )
        }

        composeRule.onNodeWithText("Card ID").performTextInput("crd-missing")
        composeRule.onNodeWithText("Run search").performClick()

        composeRule.onNodeWithText("Card not found").assertIsDisplayed()
    }

    @Test
    fun replacingCardStateWithSamePagerSizeUpdatesCardAndWalletCta() {
        val initialCard =
            Card(
                cardId = "crd-123",
                userId = "usr-123",
                lastFour = "1573",
                cardholderName = "Dieguito",
                brand = Brand.MASTERCARD,
            )
        val replacementCard =
            Card(
                cardId = "crd-456",
                userId = "usr-456",
                lastFour = "4242",
                cardholderName = "Julieta",
                brand = Brand.VISA,
            )
        var uiState by
            mutableStateOf(
                CardSearchUiState(
                    selectedCard = initialCard,
                    walletButtonState = WalletButtonState.READY_TO_ADD,
                )
            )

        composeRule.setContent {
            CardSearchScreen(
                uiState = uiState,
                isSearchSheetVisible = false,
                onSearch = { Result.success(Unit) },
                onDismissSearch = {},
            )
        }

        composeRule.onNodeWithText("DIEGUITO").assertIsDisplayed()

        composeRule.runOnIdle {
            uiState =
                CardSearchUiState(
                    selectedCard = replacementCard,
                    walletButtonState = WalletButtonState.ALREADY_ADDED,
                )
        }

        composeRule.onNodeWithText("JULIETA").assertIsDisplayed()
        composeRule.onAllNodesWithText("DIEGUITO").assertCountEquals(0)
    }
}
