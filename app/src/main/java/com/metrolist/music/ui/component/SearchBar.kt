@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.metrolist.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.TonalElevation
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.metrolist.music.constants.AppBarHeight
import kotlin.math.max
import kotlin.math.roundToInt

@ExperimentalMaterial3Api
@Composable
fun TopSearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    tonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = tonalElevation,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .height(96.dp)
        ) {}

        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = active,
            onActiveChange = onActiveChange,
            scrollBehavior = scrollBehavior,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = shape,
            colors = colors,
            content = content
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun SearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,    
    colors: SearchBarColors = SearchBarDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    tonalElevation: Dp = TonalElevation,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    content: @Composable ColumnScope.() -> Unit,
) {
    // Remove heightOffsetLimit calculation since we want the SearchBar to stay fixed

    val animationProgress: Float by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = MotionTokens.EasingLegacyCubicBezier,
        ),
        label = "",
    )

    val defaultInputFieldShape = SearchBarDefaults.inputFieldShape
    val defaultFullScreenShape = SearchBarDefaults.fullScreenShape
    val animatedShape by remember {
        derivedStateOf {
            when {
                shape == defaultInputFieldShape -> {
                    val animatedRadius = SearchBarCornerRadius * (1 - animationProgress)
                    RoundedCornerShape(CornerSize(animatedRadius))
                }
                animationProgress == 1f -> defaultFullScreenShape
                else -> shape
            }
        }
    }

    val topInset = windowInsets.asPaddingValues().calculateTopPadding()
    val startInset = windowInsets.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
    val endInset = windowInsets.asPaddingValues().calculateEndPadding(LocalLayoutDirection.current)

    val topPadding = SearchBarVerticalPadding + topInset
    val animatedSurfaceTopPadding = lerp(topPadding, 0.dp, animationProgress)
    val animatedInputFieldPadding by remember {
        derivedStateOf {
            PaddingValues(
                start = startInset * animationProgress,
                top = topPadding * animationProgress,
                end = endInset * animationProgress,
                bottom = SearchBarVerticalPadding * animationProgress,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier,  // Remove offset modifier to keep SearchBar fixed
        propagateMinConstraints = true,
    ) {
        val height: Dp
        val width: Dp
        val startPadding: Dp
        val endPadding: Dp
        with(LocalDensity.current) {
            val startWidth = constraints.maxWidth.toFloat()
            val startHeight = max(constraints.minHeight, InputFieldHeight.roundToPx())
                .coerceAtMost(constraints.maxHeight)
                .toFloat()
            val endWidth = constraints.maxWidth.toFloat()
            val endHeight = constraints.maxHeight.toFloat()

            height = lerp(startHeight, endHeight, animationProgress).toDp()
            width = lerp(startWidth, endWidth, animationProgress).toDp()
            startPadding = lerp((SearchBarHorizontalPadding + startInset).roundToPx().toFloat(), 0f, animationProgress).toDp()
            endPadding = lerp((SearchBarHorizontalPadding + endInset).roundToPx().toFloat(), 0f, animationProgress).toDp()
        }

        Surface(
            shape = animatedShape,
            color = colors.containerColor,
            contentColor = contentColorFor(colors.containerColor),
            tonalElevation = tonalElevation,
            modifier = Modifier
                .padding(
                    top = animatedSurfaceTopPadding,
                    start = startPadding,
                    end = endPadding,
                )
                .size(width = width, height = height),
        ) {
            Column {
                SearchBarInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    active = active,
                    onActiveChange = onActiveChange,
                    modifier = Modifier.padding(animatedInputFieldPadding),
                    enabled = enabled,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    colors = colors.inputFieldColors,
                    interactionSource = interactionSource,
                    focusRequester = focusRequester,
                )

                if (animationProgress > 0) {
                    Column(Modifier.alpha(animationProgress)) {
                        HorizontalDivider(color = colors.dividerColor)
                        content()
                    }
                }
            }
        }
    }

    BackHandler(enabled = active) {
        onActiveChange(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SearchBarInputField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focused = interactionSource.collectIsFocusedAsState().value

    val searchSemantics = getString(Strings.SearchBarSearch)
    val suggestionsAvailableSemantics = getString(Strings.SuggestionsAvailable)
    val textColor =
        LocalTextStyle.current.color.takeOrElse {
            colors.textColor(enabled, isError = false, focused = focused)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(InputFieldHeight),
    ) {
        if (leadingIcon != null) {
            Spacer(Modifier.width(SearchBarIconOffsetX))
            leadingIcon()
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            // Must be PointerEventPass.Initial to observe events before the text field
                            // consumes them in the Main pass
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (upEvent != null) {
                                onActiveChange(true)
                            }
                        }
                    }.semantics {
                        contentDescription = searchSemantics
                        if (active) {
                            stateDescription = suggestionsAvailableSemantics
                        }
                    }.onKeyEvent {
                        if (it.key == Key.Enter) {
                            onSearch(query.text)
                            return@onKeyEvent true
                        }
                        false
                    },
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(colors.cursorColor(isError = false)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
            interactionSource = interactionSource,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = query.text,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = placeholder,
                    shape = SearchBarDefaults.inputFieldShape,
                    colors = colors,
                    contentPadding = PaddingValues(),
                    container = {},
                )
            },
        )

        if (trailingIcon != null) {
            trailingIcon()
            Spacer(Modifier.width(SearchBarIconOffsetX))
        }
    }
}

// Measurement specs
val InputFieldHeight = 48.dp
private val SearchBarCornerRadius: Dp = InputFieldHeight / 2
internal val SearchBarMinWidth: Dp = 360.dp
private val SearchBarMaxWidth: Dp = 720.dp
internal val SearchBarVerticalPadding: Dp = 8.dp
internal val SearchBarHorizontalPadding: Dp = 12.dp

// Search bar has 16dp padding between icons and start/end, while by default text field has 12dp.
val SearchBarIconOffsetX: Dp = 4.dp

// Animation specs
private const val AnimationDurationMillis: Int = MotionTokens.DurationMedium2.toInt()
