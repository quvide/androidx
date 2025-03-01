package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class DefaultThreePaneScaffoldState(
    initialFocusHistory: List<ThreePaneScaffoldRole>,
    initialLayoutDirective: AdaptiveLayoutDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
) {

    private val focusHistory = mutableStateListOf<ThreePaneScaffoldRole>().apply {
        addAll(initialFocusHistory)
    }

    var layoutDirective by mutableStateOf(initialLayoutDirective)
    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    val currentFocus: ThreePaneScaffoldRole?
        get() = focusHistory.lastOrNull()

    val layoutValue: ThreePaneScaffoldValue get() = calculateScaffoldValue(currentFocus)

    fun navigateTo(pane: ThreePaneScaffoldRole) {
        focusHistory.add(pane)
    }

    fun canNavigateBack(layoutValueMustChange: Boolean): Boolean =
        getPreviousFocusIndex(layoutValueMustChange) >= 0

    fun navigateBack(popUntilLayoutValueChange: Boolean): Boolean {
        val previousFocusIndex = getPreviousFocusIndex(popUntilLayoutValueChange)
        if (previousFocusIndex < 0) {
            focusHistory.clear()
            return false
        }
        val targetSize = previousFocusIndex + 1
        while (focusHistory.size > targetSize) {
            focusHistory.removeLast()
        }
        return true
    }

    private fun getPreviousFocusIndex(withLayoutValueChange: Boolean): Int {
        if (focusHistory.size <= 1) {
            // No previous focus
            return -1
        }
        if (!withLayoutValueChange) {
            return focusHistory.lastIndex - 1
        }
        for (previousFocusIndex in focusHistory.lastIndex - 1 downTo 0) {
            val newValue = calculateScaffoldValue(focusHistory[previousFocusIndex])
            if (newValue != layoutValue) {
                return previousFocusIndex
            }
        }
        return -1
    }

    private fun calculateScaffoldValue(
        focus: ThreePaneScaffoldRole?
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            layoutDirective.maxHorizontalPartitions,
            adaptStrategies,
            focus
        )

    companion object {
        /**
         * To keep focus history saved
         */
        fun saver(
            initialLayoutDirective: AdaptiveLayoutDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies
        ): Saver<DefaultThreePaneScaffoldState, *> = listSaver(
            save = {
                it.focusHistory.toList()
            },
            restore = {
                DefaultThreePaneScaffoldState(
                    initialFocusHistory = it,
                    initialLayoutDirective = initialLayoutDirective,
                    initialAdaptStrategies = initialAdaptStrategies
                )
            }
        )
    }
}

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun rememberDefaultThreePaneScaffoldState(
    layoutDirectives: AdaptiveLayoutDirective,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    initialFocusHistory: List<ThreePaneScaffoldRole>
): DefaultThreePaneScaffoldState =
    rememberSaveable(
        saver = DefaultThreePaneScaffoldState.saver(
            layoutDirectives,
            adaptStrategies,
        )
    ) {
        DefaultThreePaneScaffoldState(
            initialFocusHistory = initialFocusHistory,
            initialLayoutDirective = layoutDirectives,
            initialAdaptStrategies = adaptStrategies
        )
    }.apply {
        this.layoutDirective = layoutDirectives
        this.adaptStrategies = adaptStrategies
    }
