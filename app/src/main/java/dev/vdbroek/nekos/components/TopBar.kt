package dev.vdbroek.nekos.components

import androidx.compose.material.ScaffoldState
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import dev.vdbroek.nekos.R
import kotlinx.coroutines.launch

@Composable
fun TopBar(
    scaffoldState: ScaffoldState,
    title: String? = stringResource(id = R.string.app_name)
) {
    val coroutine = rememberCoroutineScope()

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.primary,
        navigationIcon = {
            IconButton(onClick = { coroutine.launch { scaffoldState.drawerState.open() } }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Drawer Menu",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {
            Text(
                text = title ?: stringResource(id = R.string.app_name),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    )
}
