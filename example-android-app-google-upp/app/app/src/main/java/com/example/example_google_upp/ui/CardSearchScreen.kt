package com.example.example_google_upp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.example_google_upp.components.PomeloCardComposable
import com.example.example_google_upp.ui.theme.PomeloBlack
import com.example.example_google_upp.ui.theme.PomeloMagenta
import com.example.example_google_upp.ui.theme.PomeloMagentaDeep
import com.example.example_google_upp.ui.theme.PomeloOutline
import com.example.example_google_upp.ui.theme.PomeloTextMuted
import com.example.example_google_upp.ui.theme.PomeloViolet
import com.example.example_google_upp.ui.theme.PomeloWhite
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CardSearchScreen(
    uiState: CardSearchUiState,
    isSearchSheetVisible: Boolean,
    onSearch: suspend (String) -> Result<Unit>,
    onDismissSearch: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var searchErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    if (uiState.selectedCard != null) {
        PomeloCardComposable(card = uiState.selectedCard)
    }

    if (isSearchSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissSearch,
            containerColor = PomeloWhite,
            contentColor = PomeloBlack,
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Search by card ID",
                    style = MaterialTheme.typography.titleLarge,
                    color = PomeloBlack,
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        searchErrorMessage = null
                    },
                    label = { Text("Card ID") },
                    singleLine = true,
                    isError = searchErrorMessage != null,
                    supportingText = searchErrorMessage?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PomeloViolet,
                            unfocusedBorderColor = PomeloOutline,
                            focusedLabelColor = PomeloViolet,
                            unfocusedLabelColor = PomeloTextMuted,
                            cursorColor = PomeloMagenta,
                            focusedTextColor = PomeloBlack,
                            unfocusedTextColor = PomeloBlack,
                            focusedContainerColor = PomeloWhite,
                            unfocusedContainerColor = PomeloWhite,
                        ),
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            searchErrorMessage = null
                            onSearch(query).onFailure { error ->
                                searchErrorMessage = error.message ?: "Unable to load card"
                            }
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = PomeloMagenta,
                            contentColor = PomeloWhite,
                            disabledContainerColor = PomeloMagentaDeep,
                            disabledContentColor = PomeloWhite,
                        ),
                ) {
                    Text("Run search")
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
