package com.metrolist.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.R
import com.metrolist.music.constants.*
import com.metrolist.music.ui.utils.canNavigateUp
import com.metrolist.music.utils.rememberEnumPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    appBarConfig: AppBarConfig,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    background: Color = MaterialTheme.colorScheme.background,
    searchBarBackground: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
    navController: NavController,
    localSearchScreen: @Composable (query: String, onDismiss: () -> Unit) -> Unit,
    onlineSearchScreen: @Composable (query: String, onDismiss: () -> Unit) -> Unit,
    onSearchOnline: (String) -> Unit,
) {
    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.systemBars.getTop(density).toDp() }
    val heightOffsetLimit = with(density) { -(AppBarHeight + topInset).toPx() }
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != heightOffsetLimit) {
            scrollBehavior.state.heightOffsetLimit = heightOffsetLimit
        }
    }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

    val expandTransition = updateTransition(targetState = isSearchExpanded || !appBarConfig.searchable, "searchExpanded")
    val searchTransitionProgress by expandTransition.animateFloat(label = "") { if (it) 1f else 0f }
    val horizontalPadding by expandTransition.animateDp(label = "") { if (it) 0.dp else 12.dp }
    val verticalPadding by expandTransition.animateDp(label = "") { if (it) 0.dp else 8.dp }
    val cornerShapePercent by expandTransition.animateInt(label = "") { if (it) 0 else 50 }

    val barBackground by animateColorAsState(when {
        appBarConfig.searchable -> searchBarBackground
        appBarConfig.transparentBackground -> Color.Transparent
        else -> MaterialTheme.colorScheme.background
    })

    val focusRequester = remember {
        FocusRequester()
    }

    val backStateEntry by navController.currentBackStackEntryAsState()
    val canNavigateUp = remember(backStateEntry) {
        navController.canNavigateUp
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
        }
        val heightOffset = scrollBehavior.state.heightOffset
        animate(
            initialValue = heightOffset,
            targetValue = 0f
        ) { value, _ ->
            scrollBehavior.state.heightOffset = value
        }
    }

    AnimatedVisibility(
        visible = isSearchExpanded,
        enter = fadeIn(tween(easing = LinearOutSlowInEasing)) + slideInVertically(tween()) {
            with(density) { -AppBarHeight.toPx().roundToInt() }
        },
        exit = fadeOut()
    ) {
        BackHandler {
            onSearchExpandedChange(false)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(top = AppBarHeight)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Crossfade(
                targetState = searchSource
            ) { searchSource ->
                when (searchSource) {
                    SearchSource.LOCAL -> localSearchScreen(
                        query = textFieldValue.text,
                        onDismiss = { onSearchExpandedChange(false) }
                    )
                    SearchSource.ONLINE -> onlineSearchScreen(
                        query = textFieldValue.text,
                        onDismiss = { onSearchExpandedChange(false) }
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier.offset {
            IntOffset(x = 0, y = scrollBehavior.state.heightOffset.roundToInt())
        }
    ) {
        AnimatedVisibility(
            visible = !appBarConfig.transparentBackground,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topInset)
                    .drawBehind {
                        drawRect(if (appBarConfig.searchable && isSearchExpanded) {
                            searchBarBackground.copy(alpha = searchTransitionProgress)
                        } else {
                            background
                        })
                    }
            )
        }

        Box(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .fillMaxWidth()
                .height(AppBarHeight)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(cornerShapePercent)
                }
                .drawBehind {
                    drawRect(barBackground)
                }
        ) {
            AnimatedVisibility(
                visible = appBarConfig.searchable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = appBarConfig.searchable && !isSearchExpanded) {
                            onSearchExpandedChange(true)
                        }
                        .focusable()
                ) {
                    IconButton(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        onClick = {
                            when {
                                isSearchExpanded -> onSearchExpandedChange(false)
                                !appBarConfig.isRootDestination && canNavigateUp -> navController.navigateUp()
                                else -> onSearchExpandedChange(true)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isSearchExpanded || (!appBarConfig.isRootDestination && canNavigateUp)) {
                                    R.drawable.ic_arrow_back
                                } else {
                                    R.drawable.ic_search
                                }
                            ),
                            contentDescription = null
                        )
                    }

                    if (isSearchExpanded) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = onTextFieldValueChange,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            singleLine = true,
                            maxLines = 1,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    onSearchExpandedChange(false)
                                    onSearchOnline(textFieldValue.text)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = stringResource(when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }),
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .alpha(0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                    } else {
                        appBarConfig.title(this)
                    }

                    if (isSearchExpanded) {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onTextFieldValueChange(TextFieldValue(""))
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = null
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                            }
                        ) {
                            Icon(
                                painter = painterResource(when (searchSource) {
                                    SearchSource.LOCAL -> R.drawable.ic_library_music
                                    SearchSource.ONLINE -> R.drawable.ic_language
                                }),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !appBarConfig.searchable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        onClick = {
                            when {
                                isSearchExpanded -> onSearchExpandedChange(false)
                                !appBarConfig.isRootDestination && canNavigateUp -> navController.navigateUp()
                                else -> onSearchExpandedChange(true)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isSearchExpanded || !appBarConfig.isRootDestination && canNavigateUp) {
                                    R.drawable.ic_arrow_back
                                } else {
                                    R.drawable.ic_search
                                }
                            ),
                            contentDescription = null
                        )
                    }

                    appBarConfig.title(this)

                    appBarConfig.actions(this)
                }
            }
        }
    }
}

@Stable
class AppBarConfig(
    val isRootDestination: Boolean = false,
    title: @Composable RowScope.() -> Unit = {},
    searchable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    transparentBackground: Boolean = false,
) {
    var title by mutableStateOf(title)
    var searchable by mutableStateOf(searchable)
    var actions by mutableStateOf(actions)
    var transparentBackground by mutableStateOf(transparentBackground)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TopAppBarScrollBehavior =
    AppBarScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll
    )

@ExperimentalMaterial3Api
class AppBarScrollBehavior constructor(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!canScroll()) return Offset.Zero
            state.contentOffset += consumed.y
            if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                if (consumed.y == 0f && available.y > 0f) {
                    // Reset the total content offset to zero when scrolling all the way down.
                    // This will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
            }
            state.heightOffset += consumed.y
            return Offset.Zero
        }
    }
}
