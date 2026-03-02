package com.example.hellocompose.presentation.collapsing

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingToolbarScreen(
    onBackClick: () -> Unit = {}
) {
    val density = LocalDensity.current

    var expandedHeightPx by remember { mutableFloatStateOf(0f) }
    var currentHeightPx by remember { mutableFloatStateOf(0f) }
    var imageLoaded by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (expandedHeightPx <= 0f) return Offset.Zero
                val previousHeight = currentHeightPx
                currentHeightPx = (currentHeightPx + available.y).coerceIn(0f, expandedHeightPx)
                return Offset(0f, currentHeightPx - previousHeight)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (expandedHeightPx <= 0f) return Velocity.Zero
                if (available.y < 0 && currentHeightPx > 0f) {
                    animateDecay(
                        initialValue = currentHeightPx,
                        initialVelocity = available.y,
                        animationSpec = FloatExponentialDecaySpec()
                    ) { value, _ ->
                        currentHeightPx = value.coerceIn(0f, expandedHeightPx)
                    }
                    return available
                }
                return super.onPreFling(available)
            }
        }
    }

    val collapseProgress = if (expandedHeightPx > 0f) {
        1f - currentHeightPx / expandedHeightPx
    } else 0f

    val currentHeightDp = with(density) { currentHeightPx.toDp() }

    val toolbarBgColor = lerp(Color.Transparent, MaterialTheme.colorScheme.surface, collapseProgress)
    val toolbarContentColor = lerp(Color.White, MaterialTheme.colorScheme.onSurface, collapseProgress)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Collapsing Toolbar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarBgColor,
                    titleContentColor = toolbarContentColor,
                    navigationIconContentColor = toolbarContentColor
                ),
                modifier = Modifier
                    .background(toolbarBgColor)
                    .statusBarsPadding()
            )
        }
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with rounded bottom corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (expandedHeightPx > 0f) Modifier.height(currentHeightDp)
                        else Modifier
                    )
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )
                    )
            ) {
                // Background — fills overlay image area
                AsyncImage(
                    model = "https://picsum.photos/1080/1920",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = 1f - collapseProgress * 0.3f
                        }
                )

                // Overlay image — determines header height
                AsyncImage(
                    model = "https://picsum.photos/800/600",
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    onSuccess = { imageLoaded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (size.height > 0 && imageLoaded && expandedHeightPx == 0f) {
                                expandedHeightPx = size.height.toFloat()
                                currentHeightPx = size.height.toFloat()
                            }
                        }
                        .graphicsLayer {
                            if (expandedHeightPx > 0f) {
                                alpha = 1f - collapseProgress
                                val scale = 1f - collapseProgress * 0.3f
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                )
            }

            // Scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = rememberLazyListState()
            ) {
                items(50) { index ->
                    Text(
                        text = "Item #$index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
