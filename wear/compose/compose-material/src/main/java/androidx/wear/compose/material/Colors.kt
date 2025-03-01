/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.wear.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

@Stable
public class Colors(
    primary: Color = Color(0xFFAECBFA),
    primaryVariant: Color = Color(0xFF8AB4F8),
    secondary: Color = Color(0xFFFDE293),
    secondaryVariant: Color = Color(0xFF594F33),
    background: Color = Color.Black,
    surface: Color = Color(0xFF303133),
    error: Color = Color(0xFFEE675C),
    onPrimary: Color = Color(0xFF303133),
    onSecondary: Color = Color(0xFF303133),
    onBackground: Color = Color.White,
    onSurface: Color = Color.White,
    onSurfaceVariant: Color = Color(0xFFDADCE0),
    onError: Color = Color(0xFF000000)
) {
    public var primary: Color by mutableStateOf(primary, structuralEqualityPolicy())
        internal set
    public var primaryVariant: Color by mutableStateOf(primaryVariant, structuralEqualityPolicy())
        internal set
    public var secondary: Color by mutableStateOf(secondary, structuralEqualityPolicy())
        internal set
    public var secondaryVariant: Color by mutableStateOf(
        secondaryVariant,
        structuralEqualityPolicy()
    )
        internal set
    public var background: Color by mutableStateOf(background, structuralEqualityPolicy())
        internal set
    public var surface: Color by mutableStateOf(surface, structuralEqualityPolicy())
        internal set
    public var error: Color by mutableStateOf(error, structuralEqualityPolicy())
        internal set
    public var onPrimary: Color by mutableStateOf(onPrimary, structuralEqualityPolicy())
        internal set
    public var onSecondary: Color by mutableStateOf(onSecondary, structuralEqualityPolicy())
        internal set
    public var onBackground: Color by mutableStateOf(onBackground, structuralEqualityPolicy())
        internal set
    public var onSurface: Color by mutableStateOf(onSurface, structuralEqualityPolicy())
        internal set
    public var onSurfaceVariant: Color by mutableStateOf(
        onSurfaceVariant,
        structuralEqualityPolicy()
    )
        internal set
    public var onError: Color by mutableStateOf(onError, structuralEqualityPolicy())
        internal set

    /**
     * Returns a copy of this Colors, optionally overriding some of the values.
     */
    public fun copy(
        primary: Color = this.primary,
        primaryVariant: Color = this.primaryVariant,
        secondary: Color = this.secondary,
        secondaryVariant: Color = this.secondaryVariant,
        background: Color = this.background,
        surface: Color = this.surface,
        error: Color = this.error,
        onPrimary: Color = this.onPrimary,
        onSecondary: Color = this.onSecondary,
        onBackground: Color = this.onBackground,
        onSurface: Color = this.onSurface,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        onError: Color = this.onError
    ): Colors = Colors(
        primary = primary,
        primaryVariant = primaryVariant,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        background = background,
        surface = surface,
        error = error,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        onError = onError
    )

    override fun toString(): String {
        return "Colors(" +
            "primary=$primary, " +
            "primaryVariant=$primaryVariant, " +
            "secondary=$secondary, " +
            "secondaryVariant=$secondaryVariant, " +
            "background=$background, " +
            "surface=$surface, " +
            "error=$error, " +
            "onPrimary=$onPrimary, " +
            "onSecondary=$onSecondary, " +
            "onBackground=$onBackground, " +
            "onSurface=$onSurface, " +
            "onSurfaceVariant=$onSurfaceVariant, " +
            "onError=$onError" +
            ")"
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [Colors], then returns [Color.Unspecified].
 *
 * @see contentColorFor
 */
public fun Colors.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary -> onPrimary
        primaryVariant -> onPrimary
        secondary -> onSecondary
        secondaryVariant -> onSecondary
        background -> onBackground
        surface -> onSurface
        error -> onError
        else -> Color.Unspecified
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * the current value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [Colors], then returns the current value of [LocalContentColor].
 *
 * @see Colors.contentColorFor
 */
@Composable
@ReadOnlyComposable
public fun contentColorFor(backgroundColor: Color): Color =
    MaterialTheme.colors.contentColorFor(backgroundColor).takeOrElse { LocalContentColor.current }

/**
 * Updates the internal values of the given [Colors] with values from the [other] [Colors]. This
 * allows efficiently updating a subset of [Colors], without recomposing every composable that
 * consumes values from [LocalColors].
 *
 * Because [Colors] is very wide-reaching, and used by many expensive composables in the
 * hierarchy, providing a new value to [LocalColors] causes every composable consuming
 * [LocalColors] to recompose, which is prohibitively expensive in cases such as animating one
 * color in the theme. Instead, [Colors] is internally backed by [mutableStateOf], and this
 * function mutates the internal state of [this] to match values in [other]. This means that any
 * changes will mutate the internal state of [this], and only cause composables that are reading
 * the specific changed value to recompose.
 */
internal fun Colors.updateColorsFrom(other: Colors) {
    primary = other.primary
    primaryVariant = other.primaryVariant
    secondary = other.secondary
    secondaryVariant = other.secondaryVariant
    background = other.background
    surface = other.surface
    error = other.error
    onPrimary = other.onPrimary
    onSecondary = other.onSecondary
    onBackground = other.onBackground
    onSurface = other.onSurface
    onSurfaceVariant = other.onSurfaceVariant
    onError = other.onError
}

/**
 * Convert given color to disabled color.
 * @param disabledContentAlpha Alpha used to represent disabled content colors.
 */
@Composable
internal fun Color.toDisabledColor(disabledContentAlpha: Float = ContentAlpha.disabled) =
    this.copy(alpha = disabledContentAlpha)

internal val LocalColors = staticCompositionLocalOf<Colors> { Colors() }
