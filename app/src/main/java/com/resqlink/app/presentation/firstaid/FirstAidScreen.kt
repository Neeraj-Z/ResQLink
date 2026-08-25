package com.resqlink.app.presentation.firstaid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.components.RefreshRow
import com.resqlink.app.presentation.components.RoundedInfoCard
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Spacing

/**
 * First Aid module: content bundled/cached fully offline (CPR, bleeding
 * control, burns, fractures, choking, etc.) via Room, refreshed
 * opportunistically from Firestore (see [FirstAidViewModel], README section 8)
 * -- no network round trip ever sits on the critical path of reading a guide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstAidScreen(viewModel: FirstAidViewModel = hiltViewModel()) {
    val guides by viewModel.guides.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var query by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GradientHeader(
                eyebrow = "Module",
                title = "First Aid Guide",
                subtitle = "Available fully offline",
                icon = Icons.Filled.LocalHospital
            )
        }
        item {
            RefreshRow(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                label = "Synced from Firestore"
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChanged(it)
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Search procedures (CPR, burns, bleeding...)") },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding, vertical = Spacing.md)
            )
        }
        items(guides, key = { it.id }) { guide ->
            RoundedInfoCard(
                title = guide.title,
                subtitle = guide.summary,
                icon = Icons.Filled.LocalHospital,
                accentColor = Moss,
                modifier = Modifier.padding(horizontal = Spacing.screenPadding, vertical = Spacing.xxs)
            )
        }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}
