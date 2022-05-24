package dev.vdbroek.nekos.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.vdbroek.nekos.api.NekosRequestState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.capitalize

object SortingDropdownState {
    var expanded by mutableStateOf(false)
    var selected by mutableStateOf(App.defaultSort)
}

@Composable
fun SortingDropdown(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val items = listOf(
        "newest",
        "likes",
        "oldest",
        "relevance"
    )

    DropdownMenu(
        modifier = modifier,
        expanded = SortingDropdownState.expanded,
        offset = DpOffset((screenWidth - 150.dp), -(50.dp)),
        onDismissRequest = {
            SortingDropdownState.expanded = false
        }
    ) {
        items.forEach { text ->
            DropdownMenuItem(
                text = {
                    Text(text = text.capitalize())
                },
                onClick = {
                    if (SortingDropdownState.selected != text) {
                        NekosRequestState.apply {
                            end = false
                            skip = 0
                            sort = text
                            tags = App.defaultTags.toMutableStateList()
                        }

                        SortingDropdownState.selected = text
                        SortingDropdownState.expanded = false
                    }
                },
                trailingIcon = {
                    if (SortingDropdownState.selected == text) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected sort option"
                        )
                    }
                }
            )
        }
    }
}
