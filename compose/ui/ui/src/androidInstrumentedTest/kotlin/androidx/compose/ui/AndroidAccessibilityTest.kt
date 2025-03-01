/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_FOCUS
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS
import android.view.accessibility.AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
import android.view.accessibility.AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION
import android.view.accessibility.AccessibilityNodeProvider
import android.view.accessibility.AccessibilityRecord
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.DrawerValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ClassName
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.InvalidId
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextFieldClassName
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.getAllUncoveredSemanticsNodesToMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class AndroidAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: OpenComposeView
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var provider: AccessibilityNodeProvider

    private val argument = ArgumentCaptor.forClass(AccessibilityEvent::class.java)

    @Before
    fun setup() {
        // Use uiAutomation to enable accessibility manager.
        InstrumentationRegistry.getInstrumentation().uiAutomation

        rule.activityRule.scenario.onActivity { activity ->
            container = spy(OpenComposeView(activity)) {
                on { onRequestSendAccessibilityEvent(any(), any()) } doReturn false
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            activity.setContentView(container)
            androidComposeView = container.getChildAt(0) as AndroidComposeView
            delegate = ViewCompat.getAccessibilityDelegate(androidComposeView) as
                AndroidComposeViewAccessibilityDelegateCompat
            delegate.accessibilityForceEnabledForTesting = true
            provider = delegate.getAccessibilityNodeProvider(androidComposeView).provider
                as AccessibilityNodeProvider
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forToggleable() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)!!
        assertEquals("android.view.View", accessibilityNodeInfo.className)
        assertTrue(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(accessibilityNodeInfo.isCheckable)
        assertTrue(accessibilityNodeInfo.isChecked)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, "toggle")
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forSwitch() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = { checked = it }
                    )
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag, true)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)!!

        // We temporary send Switch role as a separate fake node
        val switchRoleNode = toggleableNode.replacedChildren.last()
        val switchRoleNodeInfo = provider.createAccessibilityNodeInfo(switchRoleNode.id)!!
        assertEquals("android.view.View", switchRoleNodeInfo.className)

        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals("On", stateDescription)
        assertTrue(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forDropdown() {
        val options = listOf("Option1", "Option2", "Option3", "Option4", "Option5")
        val tag = "Dropdown"
        container.setContent {
            var expanded by remember { mutableStateOf(false) }
            IconButton(
                modifier = Modifier
                    .semantics { role = Role.DropdownList }
                    .testTag(tag),
                onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(24.dp, 0.dp),
            ) {
                options.forEach {
                    DropdownMenuItem(onClick = {}) {
                        Text(it)
                    }
                }
            }
        }

        val dropdownIcon = rule.onNodeWithTag(tag, true)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(dropdownIcon.id)!!

        assertEquals("android.widget.Spinner", accessibilityNodeInfo.className)
        assertTrue(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forSelectable() {
        val tag = "Selectable"
        container.setContent {
            Box(Modifier.selectable(selected = true, onClick = {}).testTag(tag)) {
                BasicText("Text")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)!!
        assertEquals("android.view.View", accessibilityNodeInfo.className)
        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals("Selected", stateDescription)
        assertFalse(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(accessibilityNodeInfo.isCheckable)
        assertFalse(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forTab() {
        val tag = "Selectable"
        container.setContent {
            Box(Modifier.selectable(selected = true, onClick = {}, role = Role.Tab).testTag(tag)) {
                BasicText("Text")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)!!
        assertEquals("android.view.View", accessibilityNodeInfo.className)
        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertNull(stateDescription)
        assertFalse(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(accessibilityNodeInfo.isSelected)
        assertFalse(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_progressIndicator_determinate() {
        val tag = "progress"
        container.setContent {
            Box(Modifier.progressSemantics(0.5f).testTag(tag)) {
                BasicText("Text")
            }
        }

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!
        assertEquals("android.widget.ProgressBar", accessibilityNodeInfo.className)
        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals("50 percent.", stateDescription)
        assertEquals(
            AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
            accessibilityNodeInfo.rangeInfo.getType()
        )
        assertEquals(0.5f, accessibilityNodeInfo.rangeInfo.getCurrent())
        assertEquals(0f, accessibilityNodeInfo.rangeInfo.getMin())
        assertEquals(1f, accessibilityNodeInfo.rangeInfo.getMax())
    }

    @Test
    fun testCreateAccessibilityNodeInfo_progressIndicator_determinate_indeterminate() {
        val tag = "progress"
        container.setContent {
            Box(
                Modifier
                    .progressSemantics()
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)!!
        assertEquals("android.widget.ProgressBar", accessibilityNodeInfo.className)
        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals("In progress", stateDescription)
        assertNull(accessibilityNodeInfo.rangeInfo)
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forTextField() {
        val tag = "TextField"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textFieldNode.id)!!

        assertEquals("android.widget.EditText", accessibilityNodeInfo.className)
        assertEquals("hello", accessibilityNodeInfo.text.toString())
        assertTrue(accessibilityNodeInfo.isFocusable)
        assertFalse(accessibilityNodeInfo.isFocused)
        assertTrue(accessibilityNodeInfo.isEditable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_SET_SELECTION, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(
                    ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null
                )
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_FOCUS, null)
            )
        )
        assertFalse(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLEAR_FOCUS, null)
            )
        )
        if (Build.VERSION.SDK_INT >= 26) {
            assertThat(accessibilityNodeInfo.availableExtraData)
                .containsExactly(
                    "androidx.compose.ui.semantics.id",
                    AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
                    "androidx.compose.ui.semantics.testTag"
                )
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forText() {
        val text = "Test"
        container.setContent {
            BasicText(text = text)
        }

        val textNode = rule.onNodeWithText(text).fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textNode.id)

        assertThat(accessibilityNodeInfo?.className).isEqualTo("android.widget.TextView")
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forFocusable_notFocused() {
        val tag = "node"
        container.setContent {
            Box(Modifier.testTag(tag).focusable()) {
                BasicText("focusable")
            }
        }

        val focusableNode = rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(focusableNode.id)!!
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_FOCUS, null)
            )
        )
        assertFalse(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLEAR_FOCUS, null)
            )
        )
        @Suppress("DEPRECATION") accessibilityNodeInfo.recycle()
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forFocusable_focused() {
        val tag = "node"
        val focusRequester = FocusRequester()
        container.setContent {
            Box(Modifier.testTag(tag).focusRequester(focusRequester).focusable()) {
                BasicText("focusable")
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        val focusableNode = rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(focusableNode.id)!!
        assertFalse(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_FOCUS, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLEAR_FOCUS, null)
            )
        )
        @Suppress("DEPRECATION") accessibilityNodeInfo.recycle()
    }

    @Composable
    fun LastElementOverLaidColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        var yPosition = 0

        Layout(modifier = modifier, content = content) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { placeable ->
                    if (placeable != placeables[placeables.lastIndex]) {
                        placeable.placeRelative(x = 0, y = yPosition)
                        yPosition += placeable.height
                    } else {
                        // if the element is our last element (our overlaid node)
                        // then we'll put it over the middle of our previous elements
                        placeable.placeRelative(x = 0, y = yPosition / 2)
                    }
                }
            }
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_forTraversalBefore_overlaidNodeLayout() {
        val overlaidText = "Overlaid node text"
        val text1 = "Lorem1 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text2 = "Lorem2 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text3 = "Lorem3 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        container.setContent {
            LastElementOverLaidColumn(modifier = Modifier.padding(8.dp)) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(overlaidText)
                }
            }
        }

        val node3 = rule.onNodeWithText(text3).fetchSemanticsNode()
        val overlaidNode = rule.onNodeWithText(overlaidText).fetchSemanticsNode()

        val ani3 = provider.createAccessibilityNodeInfo(node3.id)
        val ani3TraversalBeforeVal = ani3?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Nodes 1, 2, and 3 are all children of a larger column; this means with a hierarchy
        // comparison (like SemanticsSort), the third text node should come before the overlaid node
        // — OverlaidNode should be read last
        assertNotEquals(ani3TraversalBeforeVal, 0)
        assertEquals(ani3TraversalBeforeVal, overlaidNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_forTraversalAfter_overlaidNodeLayout() {
        val overlaidText = "Overlaid node text"
        val text1 = "Lorem1 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text2 = "Lorem2 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text3 = "Lorem3 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        container.setContent {
            LastElementOverLaidColumn(modifier = Modifier.padding(8.dp)) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(overlaidText)
                }
            }
        }

        val node3 = rule.onNodeWithText(text3).fetchSemanticsNode()
        val overlaidNode = rule.onNodeWithText(overlaidText).fetchSemanticsNode()

        val node3ANI = provider.createAccessibilityNodeInfo(node3.id)
        val node3TraversalBefore =
            node3ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Nodes 1, 2, and 3 are all children of a larger column; this means with a hierarchy
        // comparison (like SemanticsSort), the third text node should come before the overlaid node
        // — OverlaidNode should be read last
        assertNotEquals(node3TraversalBefore, 0)
        assertEquals(node3TraversalBefore, overlaidNode.id)

        val overlaidANI = provider.createAccessibilityNodeInfo(overlaidNode.id)
        // `getInt` returns the value associated with the given key, or 0 if no mapping of
        // the desired type exists for the given key.
        val overlaidTraversalAfter =
            overlaidANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // Older versions of Samsung voice assistant crash if both traversalBefore
        // and traversalAfter redundantly express the same ordering relation, so
        // we should only have traversalBefore here.
        assertEquals(overlaidTraversalAfter, 0)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_readableTraversalGroups() {
        val clickableRowTag = "readableRow"
        val clickableButtonTag = "readableButton"
        container.setContent {
            Column {
                Row(
                    Modifier
                        .testTag(clickableRowTag)
                        .semantics { isTraversalGroup = true }
                        .clickable { }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
                    Button(onClick = { }, modifier = Modifier.testTag(clickableButtonTag)) {
                        Text("First button")
                    }
                }
            }
        }

        val rowNode = rule.onNodeWithTag(clickableRowTag).fetchSemanticsNode()
        val buttonNode = rule.onNodeWithTag(clickableButtonTag).fetchSemanticsNode()

        val rowANI = provider.createAccessibilityNodeInfo(rowNode.id)
        val rowBefore = rowANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Since the column is screenReaderFocusable, it comes before the button
        assertEquals(rowBefore, buttonNode.id)
    }

    @Composable
    fun CardRow(
        modifier: Modifier,
        columnNumber: Int,
        topSampleText: String,
        bottomSampleText: String
    ) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column {
                Text(topSampleText + columnNumber)
                Text(bottomSampleText + columnNumber)
            }
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_peerTraversalGroups_traversalIndex() {
        var topSampleText = "Top text in column "
        var bottomSampleText = "Bottom text in column "
        container.setContent {
            Column(
                Modifier
                    .testTag("Test Tag")
                    .semantics { isTraversalGroup = false }
            ) {
                Row() { Modifier.semantics { isTraversalGroup = false }
                    CardRow(
                        // Setting a bigger traversalIndex here means that this CardRow will be
                        // read second, even though it is visually to the left of the other CardRow
                        Modifier
                            .semantics { isTraversalGroup = true }
                            .semantics { traversalIndex = 1f },
                        1,
                        topSampleText,
                        bottomSampleText)
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        2,
                        topSampleText,
                        bottomSampleText)
                }
            }
        }

        val topText1 = rule.onNodeWithText(topSampleText + 1).fetchSemanticsNode()
        val topText2 = rule.onNodeWithText(topSampleText + 2).fetchSemanticsNode()
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).fetchSemanticsNode()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).fetchSemanticsNode()

        val topText1ANI = provider.createAccessibilityNodeInfo(topText1.id)
        val topText2ANI = provider.createAccessibilityNodeInfo(topText2.id)
        val bottomText2ANI = provider.createAccessibilityNodeInfo(bottomText2.id)

        val topText1Before = topText1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val topText2Before = topText2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val bottomText2Before = bottomText2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Expected behavior: "Top text in column 2" -> "Bottom text in column 2" ->
        // "Top text in column 1" -> "Bottom text in column 1"
        assertThat(topText2Before).isAtMost(bottomText2.id)
        assertThat(bottomText2Before).isAtMost(topText1.id)
        assertThat(topText1Before).isAtMost(bottomText1.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_outerFalse() {
        var topSampleText = "Top text in column "
        var bottomSampleText = "Bottom text in column "
        container.setContent {
            Column(
                Modifier
                    .testTag("Test Tag")
                    .semantics { isTraversalGroup = false }
            ) {
                Row() { Modifier.semantics { isTraversalGroup = false }
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        1,
                        topSampleText,
                        bottomSampleText)
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        2,
                        topSampleText,
                        bottomSampleText)
                }
            }
        }

        val topText1 = rule.onNodeWithText(topSampleText + 1).fetchSemanticsNode()
        val topText2 = rule.onNodeWithText(topSampleText + 2).fetchSemanticsNode()
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).fetchSemanticsNode()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).fetchSemanticsNode()

        val topText1ANI = provider.createAccessibilityNodeInfo(topText1.id)
        val topText2ANI = provider.createAccessibilityNodeInfo(topText2.id)

        val topText1Before = topText1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val topText2Before = topText2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Here we have the following hierarchy of containers:
        // `isTraversalGroup = false`
        //    `isTraversalGroup = false`
        //       `isTraversalGroup = true`
        //       `isTraversalGroup = true`
        // meaning the behavior should be as if the first two `isTraversalGroup = false` are not
        // present and all of column 1 should be read before column 2.
        assertEquals(topText1Before, bottomText1.id)
        assertEquals(topText2Before, bottomText2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_outerTrue() {
        var topSampleText = "Top text in column "
        var bottomSampleText = "Bottom text in column "
        container.setContent {
            Column(
                Modifier
                    .testTag("Test Tag")
                    .semantics { isTraversalGroup = true }
            ) {
                Row() { Modifier.semantics { isTraversalGroup = true }
                    CardRow(
                        Modifier
                            .testTag("Row 1")
                            .semantics { isTraversalGroup = false },
                        1,
                        topSampleText,
                        bottomSampleText)
                    CardRow(
                        Modifier
                            .testTag("Row 2")
                            .semantics { isTraversalGroup = false },
                        2,
                        topSampleText,
                        bottomSampleText)
                }
            }
        }

        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).fetchSemanticsNode()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).fetchSemanticsNode()

        val bottomText1ANI = provider.createAccessibilityNodeInfo(bottomText1.id)
        val bottomText1Before = bottomText1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Here we have the following hierarchy of traversal groups:
        // `isTraversalGroup = true`
        //    `isTraversalGroup = true`
        //       `isTraversalGroup = false`
        //       `isTraversalGroup = false`
        // In this case, we expect all the top text to be read first, then all the bottom text
        assertEquals(bottomText1Before, bottomText2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_tripleNestedTraversalGroups() {
        var topSampleText = "Top "
        var bottomSampleText = "Bottom "
        container.setContent {
            Row {
                CardRow(
                    Modifier.semantics { isTraversalGroup = false },
                    1,
                    topSampleText,
                    bottomSampleText)
                CardRow(
                    Modifier.semantics { isTraversalGroup = false },
                    2,
                    topSampleText,
                    bottomSampleText)
                CardRow(
                    Modifier.semantics { isTraversalGroup = true },
                    3,
                    topSampleText,
                    bottomSampleText)
            }
        }

        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).fetchSemanticsNode()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).fetchSemanticsNode()
        val bottomText3 = rule.onNodeWithText(bottomSampleText + 3).fetchSemanticsNode()
        val topText3 = rule.onNodeWithText(topSampleText + 3).fetchSemanticsNode()

        val bottomText1ANI = provider.createAccessibilityNodeInfo(bottomText1.id)
        val bottomText1Before = bottomText1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        val topText3ANI = provider.createAccessibilityNodeInfo(topText3.id)
        val topText3Before = topText3ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Here we have the following hierarchy of traversal groups:
        // `isTraversalGroup = false`
        // `isTraversalGroup = false`
        // `isTraversalGroup = true`
        // In this case, we expect to read in the order of: Top 1, Top 2, Bottom 1, Bottom 2,
        // then Top 3, Bottom 3. The first two traversal groups are effectively merged since they are both
        // set to false, while the third traversal group is structurally significant.
        assertEquals(bottomText1Before, bottomText2.id)
        assertEquals(topText3Before, bottomText3.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_hierarchy() {
        var topSampleText = "Top text in column "
        var bottomSampleText = "Bottom text in column "

        container.setContent {
            Row {
                CardRow(
                    Modifier
                        // adding a vertical scroll here makes the column scrollable, which would
                        // normally make it structurally significant
                        .verticalScroll(rememberScrollState())
                        // but adding in `traversalGroup = false` should negate that
                        .semantics { isTraversalGroup = false },
                    1,
                    topSampleText,
                    bottomSampleText
                )
                CardRow(
                    Modifier
                        // adding a vertical scroll here makes the column scrollable, which would
                        // normally make it structurally significant
                        .verticalScroll(rememberScrollState())
                        // but adding in `isTraversalGroup = false` should negate that
                        .semantics { isTraversalGroup = false },
                    2,
                    topSampleText,
                    bottomSampleText
                )
            }
        }

        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).fetchSemanticsNode()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).fetchSemanticsNode()

        val bottomText1ANI = provider.createAccessibilityNodeInfo(bottomText1.id)
        val bottomText1Before = bottomText1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // In this case, we expect all the top text to be read first, then all the bottom text
        assertThat(bottomText1Before).isAtMost(bottomText2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndex() {
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        container.setContent {
            LastElementOverLaidColumn(
                // None of the elements below should inherit `traversalIndex = 5f`
                modifier = Modifier.padding(8.dp).semantics { traversalIndex = 5f }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                // Since default traversalIndex is 0, `traversalIndex = -1f` here means that the
                // overlaid node is read first, even though visually it's below the other text.
                Row {
                    Text(
                        text = overlaidText,
                        modifier = Modifier.semantics { traversalIndex = -1f }
                    )
                }
            }
        }

        val node1 = rule.onNodeWithText(text1).fetchSemanticsNode()
        val overlaidNode = rule.onNodeWithText(overlaidText).fetchSemanticsNode()

        val overlaidANI = provider.createAccessibilityNodeInfo(overlaidNode.id)
        val overlaidTraversalBefore =
            overlaidANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Because the overlaid node has a smaller traversal index, it should be read before node 1
        assertThat(overlaidTraversalBefore).isAtMost(node1.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedAndPeerTraversalIndex() {
        val text0 = "Text 0\n"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        val text4 = "Text 4\n"
        val text5 = "Text 5\n"
        container.setContent {
            Column(
                Modifier
                    // Having a traversal index here as 8f shouldn't affect anything; this column
                    // has no other peers that its compared to
                    .semantics { traversalIndex = 8f; isTraversalGroup = true }
                    .padding(8.dp)
            ) {
                Row(
                    Modifier.semantics { traversalIndex = 3f; isTraversalGroup = true }
                ) {
                    Column(modifier = Modifier.testTag("Tag1")) {
                        Row { Text(text3) }
                        Row { Text(
                            text = text5, modifier = Modifier.semantics { traversalIndex = 1f })
                        }
                        Row { Text(text4) }
                    }
                }
                Row {
                    Text(text = text2, modifier = Modifier.semantics { traversalIndex = 2f })
                }
                Row {
                    Text(text = text1, modifier = Modifier.semantics { traversalIndex = 1f })
                }
                Row {
                    Text(text = text0)
                }
            }
        }

        val node0 = rule.onNodeWithText(text0).fetchSemanticsNode()
        val node1 = rule.onNodeWithText(text1).fetchSemanticsNode()
        val node2 = rule.onNodeWithText(text2).fetchSemanticsNode()
        val node3 = rule.onNodeWithText(text3).fetchSemanticsNode()
        val node4 = rule.onNodeWithText(text4).fetchSemanticsNode()
        val node5 = rule.onNodeWithText(text5).fetchSemanticsNode()

        val ANI0 = provider.createAccessibilityNodeInfo(node0.id)
        val ANI1 = provider.createAccessibilityNodeInfo(node1.id)
        val ANI2 = provider.createAccessibilityNodeInfo(node2.id)
        val ANI3 = provider.createAccessibilityNodeInfo(node3.id)
        val ANI4 = provider.createAccessibilityNodeInfo(node4.id)

        val traverseBefore0 =
            ANI0?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val traverseBefore1 =
            ANI1?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val traverseBefore2 =
            ANI2?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val traverseBefore3 =
            ANI3?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val traverseBefore4 =
            ANI4?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // We want to read the texts in order: 0 -> 1 -> 2 -> 3 -> 4 -> 5
        assertThat(traverseBefore0).isAtMost(node1.id)
        assertThat(traverseBefore1).isAtMost(node2.id)
        assertThat(traverseBefore2).isAtMost(node3.id)
        assertThat(traverseBefore3).isAtMost(node4.id)
        assertThat(traverseBefore4).isAtMost(node5.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndexInherited_indexFirst() {
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        container.setContent {
            LastElementOverLaidColumn(
                modifier = Modifier
                    .semantics { traversalIndex = -1f }
                    .semantics { isTraversalGroup = true }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(
                        text = overlaidText,
                        modifier = Modifier
                            .semantics { traversalIndex = 1f }
                            .semantics { isTraversalGroup = true }
                    )
                }
            }
        }

        val node3 = rule.onNodeWithText(text3).fetchSemanticsNode()
        val overlaidNode = rule.onNodeWithText(overlaidText).fetchSemanticsNode()

        val node3ANI = provider.createAccessibilityNodeInfo(node3.id)
        val node3TraverseBefore =
            node3ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Nodes 1 through 3 are read, and then overlaid node is read last
        assertThat(node3TraverseBefore).isAtMost(overlaidNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndexInherited_indexSecond() {
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        // This test is identical to the one above, except with `isTraversalGroup` coming first in
        // the modifier chain. Behavior-wise, this shouldn't change anything.
        container.setContent {
            LastElementOverLaidColumn(
                modifier = Modifier
                    .semantics { isTraversalGroup = true }
                    .semantics { traversalIndex = -1f }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(
                        text = overlaidText,
                        modifier = Modifier
                            .semantics { isTraversalGroup = true }
                            .semantics { traversalIndex = 1f }
                    )
                }
            }
        }

        val node3 = rule.onNodeWithText(text3).fetchSemanticsNode()
        val overlaidNode = rule.onNodeWithText(overlaidText).fetchSemanticsNode()

        val node3ANI = provider.createAccessibilityNodeInfo(node3.id)
        val node3TraverseBefore =
            node3ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Nodes 1 through 3 are read, and then overlaid node is read last
        assertThat(node3TraverseBefore).isAtMost(overlaidNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_SimpleTopAppBar() {
        val topAppBarText = "Top App Bar"
        val textBoxTag = "Text Box"
        container.setContent {
            Box(Modifier.testTag(textBoxTag)) {
                Text(text = "Lorem ipsum ".repeat(200))
            }

            TopAppBar(
                title = {
                    Text(text = topAppBarText)
                }
            )
        }

        val textBoxNode = rule.onNodeWithTag(textBoxTag).fetchSemanticsNode()
        val topAppBarNode = rule.onNodeWithText(topAppBarText).fetchSemanticsNode()

        val topAppBarANI = provider.createAccessibilityNodeInfo(topAppBarNode.id)
        val topAppTraverseBefore = topAppBarANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        assertThat(topAppTraverseBefore).isLessThan(textBoxNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_SimpleScrollingTopAppBar() {
        val topAppBarText = "Top App Bar"
        val sampleText = "Sample text "
        val sampleText1 = "Sample text 1"
        val sampleText2 = "Sample text 2"
        var counter = 1

        container.setContent {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                TopAppBar(title = { Text(text = topAppBarText) })
                repeat(100) {
                    Text(sampleText + counter++)
                }
            }
        }

        val topAppBarNode = rule.onNodeWithText(topAppBarText).fetchSemanticsNode()
        val topAppBarANI = provider.createAccessibilityNodeInfo(topAppBarNode.id)
        val topAppTraverseBefore = topAppBarANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        val node1 = rule.onNodeWithText(sampleText1).fetchSemanticsNode()
        val ANI1 = provider.createAccessibilityNodeInfo(node1.id)
        val traverseBefore1 = ANI1?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        val node2 = rule.onNodeWithText(sampleText2).fetchSemanticsNode()

        // Assert that the top bar comes before the first node (node 1) and that the first node
        // comes before the second (node 2)
        assertEquals(topAppTraverseBefore, node1.id)
        assertEquals(traverseBefore1, node2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_ScaffoldTopBar() {
        val topAppBarText = "Top App Bar"
        val contentText = "Content"
        val bottomAppBarText = "Bottom App Bar"
        container.setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = { FloatingActionButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
                } },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding -> Text(contentText, modifier = Modifier.padding(padding)) },
                bottomBar = { BottomAppBar {
                    Text(bottomAppBarText) } }
            )
        }
        val topAppBarNode = rule.onNodeWithText(topAppBarText).fetchSemanticsNode()
        val contentNode = rule.onNodeWithText(contentText).fetchSemanticsNode()
        val bottomAppBarNode = rule.onNodeWithText(bottomAppBarText).fetchSemanticsNode()

        val topAppBarANI = provider.createAccessibilityNodeInfo(topAppBarNode.id)
        val contentANI = provider.createAccessibilityNodeInfo(contentNode.id)
        val topAppTraverseBefore = topAppBarANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val contentTraverseBefore = contentANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        assertEquals(topAppTraverseBefore, contentNode.id)
        assertThat(contentTraverseBefore).isLessThan(bottomAppBarNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_clearSemantics() {
        val content1 = "Face 1"
        val content2 = "Face 2"
        val content3 = "Face 3"
        val contentText = "Content"
        container.setContent {
            Scaffold(
                topBar = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Face, contentDescription = content1)
                        }
                        IconButton(
                            onClick = { },
                            modifier = Modifier.clearAndSetSemantics { }
                        ) {
                            Icon(Icons.Default.Face, contentDescription = content2)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Face, contentDescription = content3)
                        }
                    }
                },
                content = { padding -> Text(contentText, modifier = Modifier.padding(padding)) }
            )
        }
        val faceNode1 = rule.onNodeWithContentDescription(content1).fetchSemanticsNode()
        val faceNode3 = rule.onNodeWithContentDescription(content3).fetchSemanticsNode()
        val contentNode = rule.onNodeWithText(contentText).fetchSemanticsNode()

        val ANI1 = provider.createAccessibilityNodeInfo(faceNode1.id)
        val ANI3 = provider.createAccessibilityNodeInfo(faceNode3.id)

        val traverseBefore1 = ANI1?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val traverseBefore3 = ANI3?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // On screen we have three faces in a top app bar, and then a content node:
        //
        //     Face1       Face2      Face3
        //               Content
        //

        // Since `clearAndSetSemantics` is set on Face2, it should not generate any semantics node.
        rule.onNodeWithTag(content2).assertDoesNotExist()

        // The traversal order for the elements on screen should then be Face1 -> Face3 -> content.
        assertEquals(traverseBefore1, faceNode3.id)
        assertEquals(traverseBefore3, contentNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_zOcclusion() {
        val parentBox1Tag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        val childThreeTag = "ChildThree"

        container.setContent {
            Column {
                Box(Modifier.testTag(parentBox1Tag)) {
                    with(LocalDensity.current) {
                        BasicText(
                            "Child One",
                            Modifier
                                // A child with larger [zIndex] will be drawn on top of all the
                                // children with smaller [zIndex]. So child 1 covers child 2.
                                .zIndex(1f)
                                .testTag(childOneTag)
                                .requiredSize(50.toDp())
                        )
                        BasicText(
                            "Child Two",
                            Modifier
                                .testTag(childTwoTag)
                                .requiredSize(50.toDp())
                        )
                    }
                }
                Box {
                    BasicText(
                        "Child Three",
                        Modifier
                            .testTag(childThreeTag)
                    )
                }
            }
        }

        val parentBox1Node = rule.onNodeWithTag(parentBox1Tag).fetchSemanticsNode()
        val childOneNode = rule.onNodeWithTag(
            childOneTag, useUnmergedTree = true).fetchSemanticsNode()
        val childTwoNode = rule.onNodeWithTag(
            childTwoTag, useUnmergedTree = true).fetchSemanticsNode()
        val childThreeNode = rule.onNodeWithTag(
            childThreeTag, useUnmergedTree = true).fetchSemanticsNode()

        val ANI1 = provider.createAccessibilityNodeInfo(childOneNode.id)
        val traverseBefore1 = ANI1?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Since child 2 is completely covered, it should not generate any ANI. The first box
        // parent should only have one child (child 1).
        assertEquals(
            1, provider.createAccessibilityNodeInfo(parentBox1Node.id)!!.childCount)
        assertNull(provider.createAccessibilityNodeInfo(childTwoNode.id))

        // The traversal order for the elements on screen should then be child 1 -> child 3,
        // completely skipping over child 2.
        assertEquals(traverseBefore1, childThreeNode.id)
    }

    @Composable
    fun ScrollColumn(
        padding: PaddingValues,
        firstElement: String,
        lastElement: String
    ) {
        var counter = 0
        var sampleText = "Sample text in column"
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Text(text = firstElement, modifier = Modifier.testTag(firstElement))
            repeat(100) {
                Text(sampleText + counter++)
            }
            Text(text = lastElement, modifier = Modifier.testTag(lastElement))
        }
    }

    @Composable
    fun ScrollColumnNoPadding(
        firstElement: String,
        lastElement: String
    ) {
        var counter = 0
        var sampleText = "Sample text in column"
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = firstElement, modifier = Modifier.testTag(firstElement))
            repeat(30) {
                Text(sampleText + counter++)
            }
            Text(text = lastElement, modifier = Modifier.testTag(lastElement))
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_ScaffoldScrollingTopBar() {
        val topAppBarText = "Top App Bar"
        val firstContentText = "First content text"
        val lastContentText = "Last content text"
        val bottomAppBarText = "Bottom App Bar"

        container.setContent {
            ScaffoldedSubcomposeLayout(
                modifier = Modifier,
                topBar = {
                    TopAppBar(
                        title = { Text(text = topAppBarText) }
                    )
                },
                content = { ScrollColumnNoPadding(firstContentText, lastContentText) },
                bottomBar = {
                    BottomAppBar {
                        Text(bottomAppBarText)
                    }
                }
            )
        }

        val topAppBarNode = rule.onNodeWithText(topAppBarText)
            .fetchSemanticsNode("couldn't find node with text $topAppBarText")
        val firstContentNode = rule.onNodeWithTag(firstContentText)
            .fetchSemanticsNode("couldn't find node with tag $firstContentText")
        val lastContentNode = rule.onNodeWithTag(lastContentText)
            .fetchSemanticsNode("couldn't find node with tag $lastContentText")

        val topAppBarANI = provider.createAccessibilityNodeInfo(topAppBarNode.id)
        val firstContentANI = provider.createAccessibilityNodeInfo(firstContentNode.id)

        val topAppTraverseBefore =
            topAppBarANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val firstContentBefore =
            firstContentANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // First content comes right after the top bar, so the `before` value equals the first
        // content node id.
        assertThat(topAppTraverseBefore).isNotEqualTo(0)
        assertThat(topAppTraverseBefore).isEqualTo(firstContentNode.id)

        // The scrolling content comes in between the first text element and the last, so
        // check that the traversal value is not 0, then assert the first text comes before the
        // last text.
        assertThat(firstContentBefore).isNotEqualTo(0)
        assertThat(firstContentBefore).isLessThan(lastContentNode.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_zIndex() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Column(Modifier.testTag(rootTag)) {
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .zIndex(1f)
                        .testTag(childTag1)
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).testTag(childTag2)
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child1TraverseBefore = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child2TraverseAfter = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child1 to come before child2
        assertEquals(2, root.replacedChildren.size)
        assertThat(child1TraverseBefore).isLessThan(child2.id)
        assertThat(child2TraverseAfter).isLessThan(child1.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_zIndex() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Row(
                Modifier.testTag(rootTag)
            ) {
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .zIndex(1f)
                        .testTag(childTag1)
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).testTag(childTag2)
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child1TraverseBefore = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child2TraverseAfter = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child1 to come before child2
        assertEquals(2, root.replacedChildren.size)
        assertThat(child1TraverseBefore).isLessThan(child2.id)
        assertThat(child2TraverseAfter).isLessThan(child1.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_offset() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Box(
                Modifier.testTag(rootTag)
            ) {
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .offset(x = 0.dp, y = 50.dp)
                        .testTag(childTag1)
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).testTag(childTag2)
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child1TraverseAfter = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertThat(child2TraverseBefore).isLessThan(child1.id)
        assertThat(child1TraverseAfter).isLessThan(child2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_offset() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Box(
                Modifier.testTag(rootTag)
            ) {
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .offset(x = 50.dp, y = 0.dp)
                        .testTag(childTag1)
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).testTag(childTag2)
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child1TraverseAfter = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertThat(child2TraverseBefore).isLessThan(child1.id)
        assertThat(child1TraverseAfter).isLessThan(child2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_offset_overlapped() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Box(
                Modifier.testTag(rootTag)
            ) {
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .offset(x = 0.dp, y = 20.dp)
                        .testTag(childTag1)
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).testTag(childTag2)
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child1TraverseAfter = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertThat(child2TraverseBefore).isLessThan(child1.id)
        assertThat(child1TraverseAfter).isLessThan(child2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_offset_overlapped() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        container.setContent {
            Box(
                Modifier.testTag(rootTag)
            ) {
                // Layouts need to have `.clickable` on them in order to make the nodes
                // speakable and therefore sortable
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .offset(x = 20.dp, y = 0.dp)
                        .testTag(childTag1)
                        .clickable(onClick = {})
                ) {}
                SimpleTestLayout(
                    Modifier
                        .requiredSize(50.dp)
                        .offset(x = 0.dp, y = 20.dp)
                        .testTag(childTag2)
                        .clickable(onClick = {})
                ) {}
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertEquals(child2TraverseBefore, child1.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_subcompose() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        val density = Density(1f)
        val size = with(density) { 100.dp.roundToPx() }.toFloat()
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                SimpleSubcomposeLayout(
                    Modifier.testTag(rootTag),
                    {
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(childTag1)
                        ) {}
                    },
                    Offset(0f, size),
                    {
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(childTag2)
                        ) {}
                    },
                    Offset(0f, 0f)
                )
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child1TraverseAfter = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertThat(child2TraverseBefore).isLessThan(child1.id)
        assertThat(child1TraverseAfter).isLessThan(child2.id)
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_subcompose() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        val density = Density(1f)
        val size = with(density) { 100.dp.roundToPx() }.toFloat()
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                SimpleSubcomposeLayout(
                    Modifier.testTag(rootTag),
                    {
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(childTag1)
                        ) {}
                    },
                    Offset(size, 0f),
                    {
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(childTag2)
                        ) {}
                    },
                    Offset(0f, 0f)
                )
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).fetchSemanticsNode()
        val child2 = rule.onNodeWithTag(childTag2).fetchSemanticsNode()

        val child1ANI = provider.createAccessibilityNodeInfo(child1.id)
        val child2ANI = provider.createAccessibilityNodeInfo(child2.id)
        val child2TraverseBefore = child2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val child1TraverseAfter = child1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // We want child2 to come before child1
        assertEquals(2, root.replacedChildren.size)
        assertThat(child2TraverseBefore).isLessThan(child1.id)
        assertThat(child1TraverseAfter).isLessThan(child2.id)
    }

    @Test
    fun testChildrenSortedByBounds_rtl() {
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        val childTag3 = "child3"
        val rtlChildTag1 = "rtlChild1"
        val rtlChildTag2 = "rtlChild2"
        val rtlChildTag3 = "rtlChild3"
        container.setContent {
            Column(Modifier.testTag(rootTag)) {
                Row {
                    SimpleTestLayout(
                        Modifier
                            .requiredSize(100.dp)
                            .testTag(childTag1)
                    ) {}
                    SimpleTestLayout(
                        Modifier
                            .requiredSize(100.dp)
                            .testTag(childTag2)
                    ) {}
                    SimpleTestLayout(
                        Modifier
                            .requiredSize(100.dp)
                            .testTag(childTag3)
                    ) {}
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // Will display rtlChild3 rtlChild2 rtlChild1
                    Row {
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(rtlChildTag1)
                        ) {}
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(rtlChildTag2)
                        ) {}
                        SimpleTestLayout(
                            Modifier
                                .requiredSize(100.dp)
                                .testTag(rtlChildTag3)
                        ) {}
                    }
                }
            }
        }

        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        assertEquals(6, root.replacedChildren.size)

        val rtlChild1 = rule.onNodeWithTag(rtlChildTag1).fetchSemanticsNode()
        val rtlChild2 = rule.onNodeWithTag(rtlChildTag2).fetchSemanticsNode()
        val rtlChild3 = rule.onNodeWithTag(rtlChildTag3).fetchSemanticsNode()

        val rtlChild1ANI = provider.createAccessibilityNodeInfo(rtlChild1.id)
        val rtlChild2ANI = provider.createAccessibilityNodeInfo(rtlChild2.id)

        val rtl1TraverseBefore = rtlChild1ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val rtl2TraverseBefore = rtlChild2ANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)

        // Rtl
        assertThat(rtl1TraverseBefore).isLessThan(rtlChild2.id)
        assertThat(rtl2TraverseBefore).isLessThan(rtlChild3.id)
    }

    @Composable
    fun InteropColumn(
        padding: PaddingValues,
        columnTag: String,
        interopText: String,
        firstButtonText: String,
        lastButtonText: String
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .testTag(columnTag)
        ) {
            Button(onClick = { }) {
                Text(firstButtonText)
            }

            AndroidView(::TextView) {
                it.text = interopText
            }

            Button(onClick = { }) {
                Text(lastButtonText)
            }
        }
    }

    @Test
    fun testChildrenSortedByBounds_ViewInterop() {
        val topAppBarText = "Top App Bar"
        val columnTag = "Column Tag"
        val interopText = "This is a text in a TextView"
        val firstButtonText = "First Button"
        val lastButtonText = "Last Button"
        val fabContentText = "FAB Icon"

        container.setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = { FloatingActionButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = fabContentText)
                } },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding -> InteropColumn(
                    padding, columnTag, interopText, firstButtonText, lastButtonText) },
                bottomBar = { BottomAppBar { Text("Bottom App Bar") } }
            )
        }

        val colSemanticsNode = rule.onNodeWithTag(columnTag)
            .fetchSemanticsNode("can't find node with tag $columnTag")
        val colAccessibilityNode = provider.createAccessibilityNodeInfo(colSemanticsNode.id)!!
        assertEquals(3, colAccessibilityNode.childCount)
        assertEquals(3, colSemanticsNode.replacedChildren.size)
        val viewHolder = androidComposeView.androidViewsHandler
            .layoutNodeToHolder[colSemanticsNode.replacedChildren[1].layoutNode]
        assertNotNull(viewHolder)

        val firstButton = rule.onNodeWithText(firstButtonText).fetchSemanticsNode()
        val lastButton = rule.onNodeWithText(lastButtonText).fetchSemanticsNode()

        val firstButtonANI = provider.createAccessibilityNodeInfo(firstButton.id)
        val lastButtonANI = provider.createAccessibilityNodeInfo(lastButton.id)
        val viewANI = viewHolder?.createAccessibilityNodeInfo()

        val firstButtonBefore = firstButtonANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val lastButtonAfter = lastButtonANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)
        val viewBefore = viewANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val viewAfter = viewANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // Desired ordering: Top App Bar -> first button -> android view -> last button -> FAB.
        // First check that the View exists
        if (viewHolder != null) {
            // Then verify that the first button comes before the View
            assertThat(firstButtonBefore).isEqualTo(viewHolder.layoutNode.semanticsId)
            // And the last button comes after the View
            assertThat(lastButtonAfter).isEqualTo(viewHolder.layoutNode.semanticsId)
        }
        // Check the View's `before` and `after` values have also been set
        assertEquals(viewAfter, firstButton.id)
        assertEquals(viewBefore, lastButton.id)
    }

    @Composable
    fun InteropColumnBackwards(
        padding: PaddingValues,
        columnTag: String,
        interopText: String,
        firstButtonText: String,
        thirdButtonText: String,
        fourthButtonText: String
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .testTag(columnTag)
        ) {
            Button(
                modifier = Modifier.semantics { traversalIndex = 3f },
                onClick = { }
            ) {
                Text(firstButtonText)
            }

            AndroidView(
                ::TextView,
                modifier = Modifier.semantics { traversalIndex = 2f }
            ) {
                it.text = interopText
            }

            Button(
                modifier = Modifier.semantics { traversalIndex = 1f },
                onClick = { }
            ) {
                Text(thirdButtonText)
            }

            Button(
                modifier = Modifier.semantics { traversalIndex = 0f },
                onClick = { }
            ) {
                Text(fourthButtonText)
            }
        }
    }

    @Test
    fun testChildrenSortedByBounds_ViewInteropBackwards() {
        val topAppBarText = "Top App Bar"
        val columnTag = "Column Tag"
        val interopText = "This is a text in a TextView"
        val firstButtonText = "First Button"
        val thirdButtonText = "Third Button"
        val fourthButtonText = "Fourth Button"
        val fabContentText = "FAB Icon"

        container.setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = { FloatingActionButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = fabContentText)
                } },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding -> InteropColumnBackwards(
                    padding, columnTag, interopText,
                    firstButtonText, thirdButtonText, fourthButtonText
                ) },
                bottomBar = { BottomAppBar { Text("Bottom App Bar") } }
            )
        }

        val colSemanticsNode = rule.onNodeWithTag(columnTag)
            .fetchSemanticsNode("can't find node with tag $columnTag")
        val colAccessibilityNode = provider.createAccessibilityNodeInfo(colSemanticsNode.id)!!
        assertEquals(4, colAccessibilityNode.childCount)
        assertEquals(4, colSemanticsNode.replacedChildren.size)
        val viewHolder = androidComposeView.androidViewsHandler
            .layoutNodeToHolder[colSemanticsNode.replacedChildren[1].layoutNode]
        assertNotNull(viewHolder)

        val firstButton = rule.onNodeWithText(firstButtonText).fetchSemanticsNode()
        val thirdButton = rule.onNodeWithText(thirdButtonText).fetchSemanticsNode()
        val fourthButton = rule.onNodeWithText(fourthButtonText).fetchSemanticsNode()

        val firstButtonANI = provider.createAccessibilityNodeInfo(firstButton.id)
        val thirdButtonANI = provider.createAccessibilityNodeInfo(thirdButton.id)
        val fourthButtonANI = provider.createAccessibilityNodeInfo(fourthButton.id)
        val viewANI = viewHolder?.createAccessibilityNodeInfo()

        val firstButtonAfter = firstButtonANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)
        val thirdButtonBefore = thirdButtonANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val fourthButtonBefore =
            fourthButtonANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val viewBefore = viewANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL)
        val viewAfter = viewANI?.extras?.getInt(EXTRA_DATA_TEST_TRAVERSALAFTER_VAL)

        // Desired ordering:
        // Top App Bar -> fourth button -> third button -> android view -> first button -> FAB.
        // Fourth button comes before the third button
        assertThat(fourthButtonBefore).isEqualTo(thirdButton.id)
        // Check that the View exists
        assertNotNull(viewHolder)
        if (viewHolder != null) {
            // Then verify that the third button comes before Android View
            assertThat(thirdButtonBefore).isEqualTo(viewHolder.layoutNode.semanticsId)
            // And the first button comes after the View
            assertThat(firstButtonAfter).isEqualTo(viewHolder.layoutNode.semanticsId)
        }
        // Check the View's `before` and `after` values have also been set
        assertEquals(viewAfter, thirdButton.id)
        assertEquals(viewBefore, firstButton.id)
    }

    companion object {
        private const val EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL =
            "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"

        private const val EXTRA_DATA_TEST_TRAVERSALAFTER_VAL =
            "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALAFTER_VAL"
    }

    @Test
    fun testPerformAction_showOnScreen() {
        rule.mainClock.autoAdvance = false

        val scrollState = ScrollState(initial = 0)
        val target1Tag = "target1"
        val target2Tag = "target2"
        container.setContent {
            Box {
                with(LocalDensity.current) {
                    Column(
                        Modifier
                            .size(200.toDp())
                            .verticalScroll(scrollState)
                    ) {
                        BasicText("Backward", Modifier.testTag(target2Tag).size(150.toDp()))
                        BasicText("Forward", Modifier.testTag(target1Tag).size(150.toDp()))
                    }
                }
            }
        }

        waitForSubtreeEventToSend()
        assertThat(scrollState.value).isEqualTo(0)

        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val targetNode1 = rule.onNodeWithTag(target1Tag)
            .fetchSemanticsNode("couldn't find node with tag $target1Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode1.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(scrollState.value).isGreaterThan(99)

        val targetNode2 = rule.onNodeWithTag(target2Tag)
            .fetchSemanticsNode("couldn't find node with tag $target2Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode2.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(scrollState.value).isEqualTo(0)
    }

    @Test
    fun testPerformAction_showOnScreen_lazy() {
        rule.mainClock.autoAdvance = false

        val lazyState = LazyListState()
        val target1Tag = "target1"
        val target2Tag = "target2"
        container.setContent {
            Box {
                with(LocalDensity.current) {
                    LazyColumn(
                        modifier = Modifier.size(200.toDp()),
                        state = lazyState
                    ) {
                        item {
                            BasicText("Backward", Modifier.testTag(target2Tag).size(150.toDp()))
                        }
                        item {
                            BasicText("Forward", Modifier.testTag(target1Tag).size(150.toDp()))
                        }
                    }
                }
            }
        }

        waitForSubtreeEventToSend()
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val targetNode1 = rule.onNodeWithTag(target1Tag)
            .fetchSemanticsNode("couldn't find node with tag $target1Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode1.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isGreaterThan(99)

        val targetNode2 = rule.onNodeWithTag(target2Tag)
            .fetchSemanticsNode("couldn't find node with tag $target2Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode2.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testPerformAction_showOnScreen_lazynested() {
        val parentLazyState = LazyListState()
        val lazyState = LazyListState()
        val target1Tag = "target1"
        val target2Tag = "target2"
        container.setContent {
            Box {
                with(LocalDensity.current) {
                    LazyRow(
                        modifier = Modifier.size(250.toDp()),
                        state = parentLazyState
                    ) {
                        item {
                            LazyColumn(
                                modifier = Modifier.size(200.toDp()),
                                state = lazyState
                            ) {
                                item {
                                    BasicText(
                                        "Backward",
                                        Modifier.testTag(target2Tag).size(150.toDp())
                                    )
                                }
                                item {
                                    BasicText(
                                        "Forward",
                                        Modifier.testTag(target1Tag).size(150.toDp())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        waitForSubtreeEventToSend()
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        // Test that child column scrolls to make it fully visible in its context, without being
        // influenced by or influencing the parent row.
        // TODO(b/190865803): Is this the ultimate right behavior we want?
        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val targetNode1 = rule.onNodeWithTag(target1Tag)
            .fetchSemanticsNode("couldn't find node with tag $target1Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode1.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isGreaterThan(99)
        assertThat(parentLazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        val targetNode2 = rule.onNodeWithTag(target2Tag)
            .fetchSemanticsNode("couldn't find node with tag $target2Tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(targetNode2.id, showOnScreen, null))
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)
        assertThat(parentLazyState.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testPerformAction_focus() {
        val tag = "node"
        container.setContent {
            Box(Modifier.testTag(tag).focusable()) {
                BasicText("focusable")
            }
        }

        val focusableNode = rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnUiThread {
            assertTrue(provider.performAction(focusableNode.id, ACTION_FOCUS, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun testPerformAction_clearFocus() {
        val tag = "node"
        val focusRequester = FocusRequester()
        container.setContent {
            Box(Modifier.testTag(tag).focusRequester(focusRequester).focusable()) {
                BasicText("focusable")
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        val focusableNode = rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnUiThread {
            assertTrue(provider.performAction(focusableNode.id, ACTION_CLEAR_FOCUS, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
    }

    @Test
    fun testPerformAction_succeedOnEnabledNodes() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(tag)
            .assertIsOff()
    }

    @Test
    fun testPerformAction_failOnDisabledNodes() {
        val tag = "DisabledToggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(
                        value = checked,
                        enabled = false,
                        onValueChange = { checked = it }
                    )
                    .testTag(tag),
                content = {
                    BasicText("ToggleableText")
                }
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnUiThread {
            assertFalse(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(tag)
            .assertIsOn()
    }

    @Test
    fun testTextField_performClickAction_succeedOnEnabledNode() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {}
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnUiThread {
            assertTrue(provider.performAction(textFieldNode.id, ACTION_CLICK, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun testTextField_performSetSelectionAction_succeedOnEnabledNode() {
        val tag = "TextField"
        var textFieldSelectionOne = false
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier = Modifier
                    .semantics {
                        // Make sure this block will be executed when selection changes.
                        this.textSelectionRange = value.selection
                        if (value.selection == TextRange(1)) {
                            textFieldSelectionOne = true
                        }
                    }
                    .testTag(tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1)
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 1)

        rule.runOnUiThread {
            textFieldSelectionOne = false
            assertTrue(provider.performAction(textFieldNode.id, ACTION_SET_SELECTION, argument))
        }
        rule.waitUntil(5_000) { textFieldSelectionOne }

        rule.onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(1)
                )
            )
    }

    @Test
    fun testTextField_testFocusClearFocusAction() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {}
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnUiThread {
            assertTrue(provider.performAction(textFieldNode.id, ACTION_FOCUS, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))

        rule.runOnUiThread {
            assertTrue(provider.performAction(textFieldNode.id, ACTION_CLEAR_FOCUS, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    fun testAddExtraDataToAccessibilityNodeInfo_notMerged() {
        val tag = "TextField"
        lateinit var textLayoutResult: TextLayoutResult

        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "texy",
                onValueChange = {},
                onTextLayout = { textLayoutResult = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        @Suppress("DEPRECATION") val info = AccessibilityNodeInfo.obtain()
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, 1)
        provider.addExtraDataToAccessibilityNodeInfo(
            textFieldNode.id,
            info,
            AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
            argument
        )
        val data = info.extras
            .getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        assertEquals(1, data!!.size)

        val rectF = data[0] as RectF // result in screen coordinates
        val expectedRectInLocalCoords = textLayoutResult.getBoundingBox(0).translate(
            textFieldNode.positionInWindow
        )
        val expectedTopLeftInScreenCoords = androidComposeView.localToScreen(
            expectedRectInLocalCoords.topLeft
        )
        assertEquals(expectedTopLeftInScreenCoords.x, rectF.left)
        assertEquals(expectedTopLeftInScreenCoords.y, rectF.top)
        assertEquals(expectedRectInLocalCoords.width, rectF.width())
        assertEquals(expectedRectInLocalCoords.height, rectF.height())

        val testTagKey = "androidx.compose.ui.semantics.testTag"
        provider.addExtraDataToAccessibilityNodeInfo(
            textFieldNode.id,
            info,
            testTagKey,
            argument
        )
        val testTagData = info.extras.getCharSequence(testTagKey)
        assertEquals(tag, testTagData.toString())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    fun getSemanticsNodeIdFromExtraData() {
        container.setContent { BasicText("texy") }
        val textNode = rule.onNodeWithText("texy").fetchSemanticsNode()
        @Suppress("DEPRECATION") val info = AccessibilityNodeInfo.obtain()
        val argument = Bundle()

        val idKey = "androidx.compose.ui.semantics.id"
        provider.addExtraDataToAccessibilityNodeInfo(
            textNode.id,
            info,
            idKey,
            argument
        )

        assertEquals(textNode.id, info.extras.getInt(idKey))
    }

    @Test
    fun sendClickedEvent_whenClick() {
        val tag = "Clickable"
        container.setContent {
            Box(Modifier.clickable(onClick = {}).testTag(tag)) {
                BasicText("Text")
            }
        }

        waitForSubtreeEventToSend()
        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(node.id, ACTION_CLICK, null))
        }

        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    }
                )
            )
        }
    }

    @Test
    fun sendStateChangeEvent_whenStateChange() {
        var state by mutableStateOf("state one")
        val tag = "State"
        container.setContent {
            Box(
                Modifier
                    .semantics { stateDescription = state }
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("state one")

        waitForSubtreeEventToSend()
        state = "state two"

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                    }
                )
            )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            )
        }
    }

    @Test
    fun sendStateChangeEvent_whenClickToggleable() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(
                    value = checked,
                    onValueChange = { checked = it }
                ).testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performClick()
            .assertIsOff()

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                    }
                )
            )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            )
        }
    }

    @Test
    fun sendStateChangeEvent_whenSelectedChange() {
        val tag = "Selectable"
        container.setContent {
            var selected by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .selectable(selected = selected, onClick = { selected = true })
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsNotSelected()

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performClick()
            .assertIsSelected()

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                    }
                )
            )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            )
        }
    }

    @Test
    fun sendViewSelectedEvent_whenSelectedChange_forTab() {
        val tag = "Tab"
        container.setContent {
            var selected by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .selectable(selected = selected, onClick = { selected = true }, role = Role.Tab)
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsNotSelected()

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performClick()
            .assertIsSelected()

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED &&
                            it.text.size == 1 &&
                            it.text[0].toString() == "Text"
                    }
                )
            )
        }
    }

    @Test
    fun sendStateChangeEvent_whenRangeInfoChange() {
        val tag = "Progress"
        var current by mutableStateOf(0.5f)
        container.setContent {
            Box(Modifier.progressSemantics(current).testTag(tag)) {
                BasicText("Text")
            }
        }
        waitForSubtreeEventToSend()

        current = 0.9f

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                    }
                )
            )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == node.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            )
        }
    }

    @Test
    fun sendTextEvents_whenSetText() {
        val locale = LocaleList("en_US")
        val tag = "TextField"
        val initialText = "h"
        val text = "hello"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue(initialText)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
                visualTransformation = {
                    TransformedText(
                        it.toUpperCase(locale),
                        OffsetMapping.Identity
                    )
                }
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString("H")
                )
            )

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(text)) }
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString("HELLO")
                )
            )

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")

        val textEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        textEvent.className = TextFieldClassName
        textEvent.fromIndex = initialText.length
        textEvent.removedCount = 0
        textEvent.addedCount = text.length - initialText.length
        textEvent.beforeText = initialText.toUpperCase(locale)
        textEvent.text.add(text.toUpperCase(locale))

        val selectionEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        )
        selectionEvent.fromIndex = text.length
        selectionEvent.toIndex = text.length
        selectionEvent.itemCount = text.length
        selectionEvent.text.add(text.toUpperCase(locale))

        rule.runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )

            val actualTextEvent = argument.allValues.first {
                it.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            }
            assertEquals(textEvent.toString(), actualTextEvent.toString())

            val actualSelectionEvent = argument.allValues.first {
                it.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            }
            assertEquals(selectionEvent.toString(), actualSelectionEvent.toString())
        }
    }

    @Test
    @Ignore("b/177656801")
    fun sendSubtreeChangeEvents_whenNodeRemoved() {
        val columnTag = "topColumn"
        val textFieldTag = "TextFieldTag"
        var isTextFieldVisible by mutableStateOf(true)

        container.setContent {
            Column(Modifier.testTag(columnTag)) {
                if (isTextFieldVisible) {
                    BasicTextField(
                        modifier = Modifier.testTag(textFieldTag),
                        value = "text",
                        onValueChange = {}
                    )
                }
            }
        }

        val parentNode = rule.onNodeWithTag(columnTag)
            .fetchSemanticsNode("couldn't find node with tag $columnTag")
        rule.onNodeWithTag(textFieldTag)
            .assertExists()
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == parentNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        // TextField is removed compared to setup.
        isTextFieldVisible = false

        rule.onNodeWithTag(textFieldTag)
            .assertDoesNotExist()
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == parentNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    fun selectionEventBeforeTraverseEvent_whenTraverseTextField() {
        val tag = "TextFieldTag"
        val text = "h"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue(text)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = {
                    BasicText("Label")
                    it()
                }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")
        waitForSubtreeEventToSend()
        rule.runOnUiThread {
            provider.performAction(
                textFieldNode.id,
                AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                createMovementGranularityCharacterArgs()
            )
        }

        val selectionEvent = createSelectionChangedFromIndexOneToOneEvent(textFieldNode)
        val traverseEvent = createCharacterTraverseFromIndexZeroEvent(textFieldNode)
        rule.runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            val traverseEventIndex = eventIndex(values, traverseEvent)
            val selectionEventIndex = eventIndex(values, selectionEvent)
            assertNotEquals(-1, traverseEventIndex)
            assertNotEquals(-1, selectionEventIndex)
            assertTrue(traverseEventIndex > selectionEventIndex)
        }
    }

    @Test
    fun selectionEventBeforeTraverseEvent_whenTraverseText() {
        val tag = "TextTag"
        val text = "h"
        container.setContent {
            BasicText(text, Modifier.testTag(tag))
        }

        val textNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")
        waitForSubtreeEventToSend()
        rule.runOnUiThread {
            provider.performAction(
                textNode.id,
                AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                createMovementGranularityCharacterArgs()
            )
        }

        val selectionEvent = createSelectionChangedFromIndexOneToOneEvent(textNode)
        val traverseEvent = createCharacterTraverseFromIndexZeroEvent(textNode)
        rule.runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            val traverseEventIndex = eventIndex(values, traverseEvent)
            val selectionEventIndex = eventIndex(values, selectionEvent)
            assertNotEquals(-1, traverseEventIndex)
            assertNotEquals(-1, selectionEventIndex)
            assertTrue(traverseEventIndex > selectionEventIndex)
        }
    }

    @Test
    @Ignore("b/177656801")
    fun semanticsNodeBeingMergedLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box {
                    BasicText("TextNode")
                }
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val textNode = rule.onNodeWithText("TextNode", useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with text TextNode")
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            for (i in 1..10) {
                delegate.onLayoutChange(textNode.layoutNode)
            }
        }

        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    @Ignore("b/177656801")
    fun layoutNodeWithoutSemanticsLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box {
                    BasicText("TextNode")
                }
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val textNode = rule.onNodeWithText("TextNode", useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with text TextNode")
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            for (i in 1..10) {
                // layout change for the parent box node
                delegate.onLayoutChange(textNode.layoutNode.parent!!)
            }
        }

        waitForSubtreeEventToSendAndVerify {
            // One from initialization and one from layout changes.
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    fun testSemanticsHitTest() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val toggleableNodeBounds = toggleableNode.boundsInRoot

        val toggleableNodeId = delegate.hitTestSemanticsAt(
            (toggleableNodeBounds.left + toggleableNodeBounds.right) / 2,
            (toggleableNodeBounds.top + toggleableNodeBounds.bottom) / 2,
        )
        assertEquals(toggleableNode.id, toggleableNodeId)
    }

    @Test
    fun testSemanticsHitTest_overlappedChildren() {
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        container.setContent {
            Box {
                with(LocalDensity.current) {
                    BasicText(
                        "Child One",
                        Modifier
                            .zIndex(1f)
                            .testTag(childOneTag)
                            .requiredSize(50.toDp())
                    )
                    BasicText(
                        "Child Two",
                        Modifier
                            .testTag(childTwoTag)
                            .requiredSize(50.toDp())
                    )
                }
            }
        }

        val overlappedChildOneNode = rule.onNodeWithTag(childOneTag)
            .fetchSemanticsNode("couldn't find node with tag $childOneTag")
        val overlappedChildTwoNode = rule.onNodeWithTag(childTwoTag)
            .fetchSemanticsNode("couldn't find node with tag $childTwoTag")
        val overlappedChildNodeBounds = overlappedChildTwoNode.boundsInRoot
        val overlappedChildNodeId = delegate.hitTestSemanticsAt(
            (overlappedChildNodeBounds.left + overlappedChildNodeBounds.right) / 2,
            (overlappedChildNodeBounds.top + overlappedChildNodeBounds.bottom) / 2
        )
        assertEquals(overlappedChildOneNode.id, overlappedChildNodeId)
        assertNotEquals(overlappedChildTwoNode.id, overlappedChildNodeId)
    }

    @Test
    fun testSemanticsHitTest_scrolled() {
        val scrollState = ScrollState(initial = 0)
        val targetTag = "target"
        var scope: CoroutineScope? = null
        container.setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }

            Box {
                with(LocalDensity.current) {
                    Column(
                        Modifier
                            .size(200.toDp())
                            .verticalScroll(scrollState)
                    ) {
                        BasicText("Before scroll", Modifier.size(200.toDp()))
                        BasicText("After scroll", Modifier.testTag(targetTag).size(200.toDp()))
                    }
                }
            }
        }

        waitForSubtreeEventToSend()
        assertThat(scrollState.value).isEqualTo(0)

        scope!!.launch {
            // Scroll to the bottom
            scrollState.scrollBy(10000f)
        }
        rule.waitForIdle()

        assertThat(scrollState.value).isGreaterThan(199)

        val childNode = rule.onNodeWithTag(targetTag)
            .fetchSemanticsNode("couldn't find node with tag $targetTag")
        val childNodeBounds = childNode.boundsInRoot
        val hitTestedId = delegate.hitTestSemanticsAt(
            (childNodeBounds.left + childNodeBounds.right) / 2,
            (childNodeBounds.top + childNodeBounds.bottom) / 2
        )
        assertEquals(childNode.id, hitTestedId)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testSemanticsHitTest_invisibleToUserSemantics() {
        val tag = "box"
        container.setContent {
            Box(Modifier.size(100.dp).clickable {}.testTag(tag).semantics { invisibleToUser() }) {
                BasicText("")
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("")
        val bounds = node.boundsInRoot

        val hitNodeId = delegate.hitTestSemanticsAt(
            bounds.left + bounds.width / 2,
            bounds.top + bounds.height / 2
        )
        assertEquals(InvalidId, hitNodeId)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun viewInteropIsInvisibleToUser() {
        container.setContent {
             AndroidView({ TextView(it).apply { text = "Test"; setScreenReaderFocusable(true) } })
        }
        Espresso
            .onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val viewParent = view.getParent()
                if (viewParent !is View) {
                    throw exception
                }
                val delegate = viewParent.getAccessibilityDelegate()
                if (viewParent.getAccessibilityDelegate() == null) {
                    throw exception
                }
                val info: AccessibilityNodeInfo = AccessibilityNodeInfo()
                delegate.onInitializeAccessibilityNodeInfo(view, info)
                // This is expected to be false, unlike
                // AndroidViewTest.androidViewAccessibilityDelegate, because this test suite sets
                // `accessibilityForceEnabledForTesting` to true.
                if (info.isVisibleToUser()) {
                    throw exception
                }
            }
    }

    @Test
    fun testSemanticsHitTest_transparentNode() {
        val tag = "box"
        container.setContent {
            Box(Modifier.alpha(0f).size(100.dp).clickable {}.testTag(tag)) {
                BasicText("")
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("")
        val bounds = node.boundsInRoot

        val hitNodeId = delegate.hitTestSemanticsAt(
            bounds.left + bounds.width / 2,
            bounds.top + bounds.height / 2
        )
        assertEquals(InvalidId, hitNodeId)
    }

    @Test
    fun testSemanticsHitTest_clearAndSet() {
        val outertag = "outerbox"
        val innertag = "innerbox"
        container.setContent {
            Box(Modifier.size(100.dp).clickable {}.testTag(outertag).clearAndSetSemantics {}) {
                Box(Modifier.size(100.dp).clickable {}.testTag(innertag)) {
                    BasicText("")
                }
            }
        }

        val outerNode = rule.onNodeWithTag(outertag).fetchSemanticsNode("")
        val innerNode = rule.onNodeWithTag(innertag, true).fetchSemanticsNode("")
        val bounds = innerNode.boundsInRoot

        val hitNodeId = delegate.hitTestSemanticsAt(
            bounds.left + bounds.width / 2,
            bounds.top + bounds.height / 2
        )
        assertEquals(outerNode.id, hitNodeId)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    fun testViewInterop_findViewByAccessibilityId() {
        val androidViewTag = "androidView"
        container.setContent {
            Column {
                AndroidView(
                    { context ->
                        LinearLayout(context).apply {
                            addView(TextView(context).apply { text = "Text1" })
                            addView(TextView(context).apply { text = "Text2" })
                        }
                    },
                    Modifier.testTag(androidViewTag)
                )
                BasicText("text")
            }
        }

        val getViewRootImplMethod = View::class.java.getDeclaredMethod("getViewRootImpl")
        getViewRootImplMethod.isAccessible = true
        val rootView = getViewRootImplMethod.invoke(container)

        val forName = Class::class.java.getMethod("forName", String::class.java)
        val getDeclaredMethod = Class::class.java.getMethod(
            "getDeclaredMethod",
            String::class.java,
            arrayOf<Class<*>>()::class.java
        )

        val viewRootImplClass = forName.invoke(null, "android.view.ViewRootImpl") as Class<*>
        val getAccessibilityInteractionControllerMethod = getDeclaredMethod.invoke(
            viewRootImplClass,
            "getAccessibilityInteractionController",
            arrayOf<Class<*>>()
        ) as Method
        getAccessibilityInteractionControllerMethod.isAccessible = true
        val accessibilityInteractionController =
            getAccessibilityInteractionControllerMethod.invoke(rootView)

        val accessibilityInteractionControllerClass =
            forName.invoke(null, "android.view.AccessibilityInteractionController") as Class<*>
        val findViewByAccessibilityIdMethod =
            getDeclaredMethod.invoke(
                accessibilityInteractionControllerClass,
                "findViewByAccessibilityId",
                arrayOf<Class<*>>(Int::class.java)
            ) as Method
        findViewByAccessibilityIdMethod.isAccessible = true

        val androidView = rule.onNodeWithTag(androidViewTag)
            .fetchSemanticsNode("can't find node with tag $androidViewTag")
        val viewGroup = androidComposeView.androidViewsHandler
            .layoutNodeToHolder[androidView.layoutNode]!!.view as ViewGroup
        val getAccessibilityViewIdMethod = View::class.java
            .getDeclaredMethod("getAccessibilityViewId")
        getAccessibilityViewIdMethod.isAccessible = true

        val textTwo = viewGroup.getChildAt(1)
        val textViewTwoId = getAccessibilityViewIdMethod.invoke(textTwo)
        val foundView = findViewByAccessibilityIdMethod.invoke(
            accessibilityInteractionController,
            textViewTwoId
        )
        assertNotNull(foundView)
        assertEquals(textTwo, foundView)
    }

    @Test
    fun testViewInterop_viewChildExists() {
        val colTag = "ColTag"
        val buttonText = "button text"
        container.setContent {
            Column(Modifier.testTag(colTag)) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText("text")
            }
        }

        val colSemanticsNode = rule.onNodeWithTag(colTag)
            .fetchSemanticsNode("can't find node with tag $colTag")
        val colAccessibilityNode = provider.createAccessibilityNodeInfo(colSemanticsNode.id)!!
        assertEquals(2, colAccessibilityNode.childCount)
        assertEquals(2, colSemanticsNode.replacedChildren.size)
        val buttonHolder = androidComposeView.androidViewsHandler
            .layoutNodeToHolder[colSemanticsNode.replacedChildren[0].layoutNode]
        assertNotNull(buttonHolder)
        assertEquals(
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
            buttonHolder!!.importantForAccessibility
        )
        assertEquals(buttonText, (buttonHolder.getChildAt(0) as Button).text)
    }

    @Test
    fun testViewInterop_hoverEnterExit() {
        val colTag = "ColTag"
        val textTag = "TextTag"
        val buttonText = "button text"
        container.setContent {
            Column(Modifier.testTag(colTag)) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText(text = "text", modifier = Modifier.testTag(textTag))
            }
        }

        val colSemanticsNode = rule.onNodeWithTag(colTag)
            .fetchSemanticsNode("can't find node with tag $colTag")
        rule.runOnUiThread {
            val bounds = colSemanticsNode.replacedChildren[0].boundsInRoot
            val hoverEnter = createHoverMotionEvent(
                action = ACTION_HOVER_ENTER,
                x = (bounds.left + bounds.right) / 2f,
                y = (bounds.top + bounds.bottom) / 2f
            )
            assertTrue(androidComposeView.dispatchHoverEvent(hoverEnter))
            assertEquals(
                AndroidComposeViewAccessibilityDelegateCompat.InvalidId,
                delegate.hoveredVirtualViewId
            )
        }
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
                    }
                )
            )
        }

        val textNode = rule.onNodeWithTag(textTag)
            .fetchSemanticsNode("can't find node with tag $textTag")
        rule.runOnUiThread {
            val bounds = textNode.boundsInRoot
            val hoverEnter = createHoverMotionEvent(
                action = ACTION_HOVER_MOVE,
                x = (bounds.left + bounds.right) / 2,
                y = (bounds.top + bounds.bottom) / 2
            )
            assertTrue(androidComposeView.dispatchHoverEvent(hoverEnter))
            assertEquals(
                textNode.id,
                delegate.hoveredVirtualViewId
            )
        }
        // verify hover exit accessibility event is sent from the previously hovered view
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
                    }
                )
            )
        }
    }

    @Test
    fun testViewInterop_dualHoverEnterExit() {
        val colTag = "ColTag"
        val textTag = "TextTag"
        val buttonText = "button text"
        val events = mutableListOf<PointerEvent>()
        container.setContent {
            Column(Modifier
                .testTag(colTag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes[0].consume()
                            events += event
                        }
                    }
                }
            ) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText(text = "text", modifier = Modifier.testTag(textTag))
            }
        }

        val colSemanticsNode = rule.onNodeWithTag(colTag)
            .fetchSemanticsNode("can't find node with tag $colTag")
        rule.runOnUiThread {
            val bounds = colSemanticsNode.replacedChildren[0].boundsInRoot
            val hoverEnter = createHoverMotionEvent(
                action = ACTION_HOVER_ENTER,
                x = (bounds.left + bounds.right) / 2f,
                y = (bounds.top + bounds.bottom) / 2f
            )
            assertTrue(androidComposeView.dispatchHoverEvent(hoverEnter))
            assertEquals(
                AndroidComposeViewAccessibilityDelegateCompat.InvalidId,
                delegate.hoveredVirtualViewId
            )
            // Assert that the hover event has also been dispatched
            assertThat(events).hasSize(1)
            // and that the hover event is an enter event
            assertHoverEvent(events[0], isEnter = true)
        }
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
                    }
                )
            )
        }
    }

    private fun assertHoverEvent(
        event: PointerEvent,
        isEnter: Boolean = false,
        isExit: Boolean = false
    ) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(change.previousPressed).isFalse()
        val expectedHoverType = when {
            isEnter -> PointerEventType.Enter
            isExit -> PointerEventType.Exit
            else -> PointerEventType.Move
        }
        assertThat(event.type).isEqualTo(expectedHoverType)
    }

    fun createHoverMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        val pointerProperties = MotionEvent.PointerProperties().apply {
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val pointerCoords = MotionEvent.PointerCoords().also {
            it.x = x
            it.y = y
        }
        return MotionEvent.obtain(
            0L /* downTime */,
            0L /* eventTime */,
            action,
            1 /* pointerCount */,
            arrayOf(pointerProperties),
            arrayOf(pointerCoords),
            0 /* metaState */,
            0 /* buttonState */,
            0f /* xPrecision */,
            0f /* yPrecision */,
            0 /* deviceId */,
            0 /* edgeFlags */,
            InputDevice.SOURCE_TOUCHSCREEN,
            0 /* flags */
        )
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_completelyCovered() {
        val parentTag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        container.setContent {
            Box(Modifier.testTag(parentTag)) {
                with(LocalDensity.current) {
                    BasicText(
                        "Child One",
                        Modifier
                            .zIndex(1f)
                            .testTag(childOneTag)
                            .requiredSize(50.toDp())
                    )
                    BasicText(
                        "Child Two",
                        Modifier
                            .testTag(childTwoTag)
                            .requiredSize(50.toDp())
                    )
                }
            }
        }

        val parentNode = rule.onNodeWithTag(parentTag)
            .fetchSemanticsNode("couldn't find node with tag $parentTag")
        val overlappedChildOneNode = rule.onNodeWithTag(childOneTag)
            .fetchSemanticsNode("couldn't find node with tag $childOneTag")
        val overlappedChildTwoNode = rule.onNodeWithTag(childTwoTag)
            .fetchSemanticsNode("couldn't find node with tag $childTwoTag")
        assertEquals(1, provider.createAccessibilityNodeInfo(parentNode.id)!!.childCount)
        assertEquals(
            "Child One",
            provider.createAccessibilityNodeInfo(overlappedChildOneNode.id)!!.text.toString()
        )
        assertNull(provider.createAccessibilityNodeInfo(overlappedChildTwoNode.id))
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_partiallyCovered() {
        val parentTag = "parent"
        val density = Density(2f)
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(Modifier.testTag(parentTag)) {
                    with(LocalDensity.current) {
                        BasicText(
                            "Child One",
                            Modifier
                                .zIndex(1f)
                                .requiredSize(100.toDp())
                        )
                        BasicText(
                            "Child Two",
                            Modifier.requiredSize(200.toDp(), 100.toDp())
                        )
                    }
                }
            }
        }

        val parentNode = rule.onNodeWithTag(parentTag)
            .fetchSemanticsNode("couldn't find node with tag $parentTag")
        assertEquals(2, provider.createAccessibilityNodeInfo(parentNode.id)!!.childCount)

        val childTwoNode = rule.onNodeWithText("Child Two")
            .fetchSemanticsNode("couldn't find node with text Child Two")
        val childTwoBounds = Rect()
        provider.createAccessibilityNodeInfo(childTwoNode.id)!!
            .getBoundsInScreen(childTwoBounds)
        assertEquals(100, childTwoBounds.height())
        assertEquals(100, childTwoBounds.width())
    }

    @Test
    fun testPaneAppear() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(
                    Modifier
                        .testTag(paneTag)
                        .semantics { paneTitle = paneTestTitle }
                ) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()
        val paneNode = rule.onNodeWithTag(paneTag).fetchSemanticsNode()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == paneNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED
                    }
                )
            )
        }
    }

    @Test
    fun testPaneTitleChange() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        var paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(
                    Modifier
                        .testTag(paneTag)
                        .semantics { paneTitle = paneTestTitle }
                ) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()

        paneTestTitle = "new pane title"
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "new pane title"
                )
            )
        val paneNode = rule.onNodeWithTag(paneTag).fetchSemanticsNode()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == paneNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE
                    }
                )
            )
        }
    }

    @Test
    fun testPaneDisappear() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(Modifier.testTag(paneTag).semantics { paneTitle = paneTestTitle }) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()

        isPaneVisible = false
        rule.onNodeWithTag(paneTag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
                    }
                )
            )
        }
    }

    @Test
    fun testMultiPanesDisappear() {
        val firstPaneTag = "Pane 1"
        val secondPaneTag = "Pane 2"
        var isPaneVisible by mutableStateOf(false)
        val firstPaneTestTitle by mutableStateOf("first pane title")
        val secondPaneTestTitle by mutableStateOf("second pane title")

        container.setContent {
            if (isPaneVisible) {
                Column {
                    with(LocalDensity.current) {
                        Box(
                            Modifier
                                .size(100.toDp())
                                .testTag(firstPaneTag)
                                .semantics { paneTitle = firstPaneTestTitle }) {}
                        Box(
                            Modifier
                                .size(100.toDp())
                                .testTag(secondPaneTag)
                                .semantics { paneTitle = secondPaneTestTitle }) {}
                    }
                }
            }
        }

        rule.onNodeWithTag(firstPaneTag).assertDoesNotExist()
        rule.onNodeWithTag(secondPaneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(firstPaneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "first pane title"
                )
            )
            .assertIsDisplayed()
        rule.onNodeWithTag(secondPaneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "second pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()

        isPaneVisible = false
        rule.onNodeWithTag(firstPaneTag).assertDoesNotExist()
        rule.onNodeWithTag(secondPaneTag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, times(2)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
                    }
                )
            )
        }
    }

    @Test
    fun testEventForPasswordTextField() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation()
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("Couldn't fetch node with tag $tag")
        val event = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )

        assertTrue(event.isPassword)
    }

    @Test
    fun testLayerParamChange_setCorrectBounds_syntaxOne() {
        var scale by mutableStateOf(1f)
        container.setContent {
            // testTag must not be on the same node with graphicsLayer, otherwise we will have
            // semantics change notification.
            with(LocalDensity.current) {
                Box(
                    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        .requiredSize(300.toDp())
                ) {
                    Box(Modifier.matchParentSize().testTag("node"))
                }
            }
        }

        val node = rule.onNodeWithTag("node").fetchSemanticsNode()
        @Suppress("DEPRECATION") var info: AccessibilityNodeInfo = AccessibilityNodeInfo.obtain()
        rule.runOnUiThread {
            info = provider.createAccessibilityNodeInfo(node.id)!!
        }
        val rect = Rect()
        info.getBoundsInScreen(rect)
        assertEquals(300, rect.width())
        assertEquals(300, rect.height())

        scale = 0.5f
        @Suppress("DEPRECATION") info.recycle()
        rule.runOnIdle {
            info = provider.createAccessibilityNodeInfo(node.id)!!
        }
        info.getBoundsInScreen(rect)
        assertEquals(150, rect.width())
        assertEquals(150, rect.height())
    }

    @Test
    fun testLayerParamChange_setCorrectBounds_syntaxTwo() {
        var scale by mutableStateOf(1f)
        container.setContent {
            // testTag must not be on the same node with graphicsLayer, otherwise we will have
            // semantics change notification.
            with(LocalDensity.current) {
                Box(
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.requiredSize(300.toDp())
                ) {
                    Box(Modifier.matchParentSize().testTag("node"))
                }
            }
        }

        val node = rule.onNodeWithTag("node").fetchSemanticsNode()
        @Suppress("DEPRECATION") var info: AccessibilityNodeInfo = AccessibilityNodeInfo.obtain()
        rule.runOnUiThread {
            info = provider.createAccessibilityNodeInfo(node.id)!!
        }
        val rect = Rect()
        info.getBoundsInScreen(rect)
        assertEquals(300, rect.width())
        assertEquals(300, rect.height())

        scale = 0.5f
        @Suppress("DEPRECATION") info.recycle()
        rule.runOnIdle {
            info = provider.createAccessibilityNodeInfo(node.id)!!
        }
        info.getBoundsInScreen(rect)
        assertEquals(150, rect.width())
        assertEquals(150, rect.height())
    }

    @Test
    fun testDialog_setCorrectBounds() {
        var dialogComposeView: AndroidComposeView? = null
        container.setContent {
            Dialog(onDismissRequest = {}) {
                dialogComposeView = LocalView.current as AndroidComposeView
                delegate = ViewCompat.getAccessibilityDelegate(dialogComposeView!!) as
                    AndroidComposeViewAccessibilityDelegateCompat
                provider = delegate.getAccessibilityNodeProvider(dialogComposeView!!).provider
                    as AccessibilityNodeProvider

                with(LocalDensity.current) {
                    Box(Modifier.size(300.toDp())) {
                        BasicText(
                            text = "text",
                            modifier = Modifier.offset(100.toDp(), 100.toDp()).fillMaxSize()
                        )
                    }
                }
            }
        }

        val textNode = rule.onNodeWithText("text").fetchSemanticsNode()
        @Suppress("DEPRECATION") var info: AccessibilityNodeInfo = AccessibilityNodeInfo.obtain()
        rule.runOnUiThread {
            info = provider.createAccessibilityNodeInfo(textNode.id)!!
        }

        val viewPosition = intArrayOf(0, 0)
        dialogComposeView!!.getLocationOnScreen(viewPosition)
        val offset = 100
        val size = 200
        val textPositionOnScreenX = viewPosition[0] + offset
        val textPositionOnScreenY = viewPosition[1] + offset

        val textRect = Rect()
        info.getBoundsInScreen(textRect)
        assertEquals(
            Rect(
                textPositionOnScreenX,
                textPositionOnScreenY,
                textPositionOnScreenX + size,
                textPositionOnScreenY + size
            ),
            textRect
        )
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun testTestTagsAsResourceId() {
        val tag1 = "box1"
        val tag2 = "box2"
        val tag3 = "box3"
        val tag4 = "box4"
        val tag5 = "box5"
        val tag6 = "box6"
        val tag7 = "box7"
        container.setContent {
            with(LocalDensity.current) {
                Column {
                    Box(Modifier.size(100.toDp()).testTag(tag1))
                    Box(Modifier.semantics { testTagsAsResourceId = true }) {
                        Box(Modifier.size(100.toDp()).testTag(tag2))
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = false }) {
                        Box(Modifier.size(100.toDp()).testTag(tag3))
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = true }) {
                        Box(Modifier.semantics { testTagsAsResourceId = false }) {
                            Box(Modifier.size(100.toDp()).testTag(tag4))
                        }
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = false }) {
                        Box(Modifier.semantics { testTagsAsResourceId = true }) {
                            Box(Modifier.size(100.toDp()).testTag(tag5))
                        }
                    }
                    Box(Modifier.semantics(true) { testTagsAsResourceId = true }) {
                        Box(Modifier.semantics { testTagsAsResourceId = false }) {
                            Box(Modifier.size(100.toDp()).testTag(tag6))
                        }
                    }
                    Box(Modifier.semantics(true) { testTagsAsResourceId = false }) {
                        Box(Modifier.semantics { testTagsAsResourceId = true }) {
                            Box(Modifier.size(100.toDp()).testTag(tag7))
                        }
                    }
                }
            }
        }

        val node1 = rule.onNodeWithTag(tag1).fetchSemanticsNode()
        val info1 = provider.createAccessibilityNodeInfo(node1.id)!!
        assertEquals(null, info1.viewIdResourceName)

        val node2 = rule.onNodeWithTag(tag2).fetchSemanticsNode()
        val info2 = provider.createAccessibilityNodeInfo(node2.id)!!
        assertEquals(tag2, info2.viewIdResourceName)

        val node3 = rule.onNodeWithTag(tag3).fetchSemanticsNode()
        val info3 = provider.createAccessibilityNodeInfo(node3.id)!!
        assertEquals(null, info3.viewIdResourceName)

        val node4 = rule.onNodeWithTag(tag4).fetchSemanticsNode()
        val info4 = provider.createAccessibilityNodeInfo(node4.id)!!
        assertEquals(null, info4.viewIdResourceName)

        val node5 = rule.onNodeWithTag(tag5).fetchSemanticsNode()
        val info5 = provider.createAccessibilityNodeInfo(node5.id)!!
        assertEquals(tag5, info5.viewIdResourceName)

        val node6 = rule.onNodeWithTag(tag6, true).fetchSemanticsNode()
        val info6 = provider.createAccessibilityNodeInfo(node6.id)!!
        assertEquals(null, info6.viewIdResourceName)

        val node7 = rule.onNodeWithTag(tag7, true).fetchSemanticsNode()
        val info7 = provider.createAccessibilityNodeInfo(node7.id)!!
        assertEquals(tag7, info7.viewIdResourceName)
    }

    @Test
    fun testContentDescription_notMergingDescendants_withOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics { contentDescription = "Column" }.testTag(tag)) {
                with(LocalDensity.current) {
                    BasicText("Text")
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box" })
                }
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals("Column", info.contentDescription)
    }

    @Test
    fun testContentDescription_notMergingDescendants_withoutOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics {}.testTag(tag)) {
                BasicText("Text")
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box" })
                }
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals(null, info.contentDescription)
    }

    @Test
    fun testContentDescription_singleNode_notMergingDescendants() {
        val tag = "box"
        container.setContent {
            with(LocalDensity.current) {
                with(LocalDensity.current) {
                    Box(
                        Modifier.size(100.toDp())
                            .testTag(tag)
                            .semantics { contentDescription = "Box" }
                    )
                }
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals("Box", info.contentDescription)
    }

    @Test
    fun testContentDescription_singleNode_mergingDescendants() {
        val tag = "box"
        container.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp()).testTag(tag)
                        .semantics(true) { contentDescription = "Box" }
                )
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals("Box", info.contentDescription)
    }

    @Test
    fun testContentDescription_replacingSemanticsNode() {
        val tag = "box"
        container.setContent {
            with(LocalDensity.current) {
                Column(
                    Modifier
                        .size(100.toDp())
                        .testTag(tag)
                        .clearAndSetSemantics { contentDescription = "Replacing description" }
                ) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box one" })
                    Box(
                        Modifier.size(100.toDp())
                            .semantics(true) { contentDescription = "Box two" }
                    )
                }
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals("Replacing description", info.contentDescription)
    }

    @Test
    fun testRole_doesNotMerge() {
        container.setContent {
            Row(Modifier.semantics(true) {}.testTag("Row")) {
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { role = Role.Button })
                    Box(Modifier.size(100.toDp()).semantics { role = Role.Image })
                }
            }
        }

        val node = rule.onNodeWithTag("Row").fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!

        assertEquals(AndroidComposeViewAccessibilityDelegateCompat.ClassName, info.className)
    }

    @Test
    fun testReportedBounds_clickableNode_includesPadding(): Unit = with(rule.density) {
        val size = 100.dp.roundToPx()
        container.setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier
                            .testTag("tag")
                            .clickable {}
                            .size(size.toDp())
                            .padding(10.toDp())
                            .semantics {
                                contentDescription = "Button"
                            }
                    )
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top

        assertEquals(size, resultWidth)
        assertEquals(size, resultHeight)
    }

    @Test
    fun testReportedBounds_clickableNode_excludesPadding(): Unit = with(rule.density) {
        val size = 100.dp.roundToPx()
        val density = Density(2f)
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Column {
                    with(density) {
                        Box(
                            Modifier
                                .testTag("tag")
                                .semantics { contentDescription = "Test" }
                                .size(size.toDp())
                                .padding(10.toDp())
                                .clickable {}
                        )
                    }
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top

        assertEquals(size - 20, resultWidth)
        assertEquals(size - 20, resultHeight)
    }

    @Test
    fun testReportedBounds_withClearAndSetSemantics() {
        val size = 100
        container.setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier
                            .testTag("tag")
                            .size(size.toDp())
                            .padding(10.toDp())
                            .clearAndSetSemantics {}
                            .clickable {}
                    )
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top

        assertEquals(size, resultWidth)
        assertEquals(size, resultHeight)
    }

    @Test
    fun testReportedBounds_withTwoClickable_outermostWins(): Unit = with(rule.density) {
        val size = 100.dp.roundToPx()
        container.setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier
                            .testTag("tag")
                            .clickable {}
                            .size(size.toDp())
                            .padding(10.toDp())
                            .clickable {}
                    )
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top

        assertEquals(size, resultWidth)
        assertEquals(size, resultHeight)
    }

    @Test
    fun testReportedBounds_outerMostSemanticsUsed() {
        val size = 100
        container.setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier
                            .testTag("tag")
                            .semantics { contentDescription = "Test1" }
                            .size(size.toDp())
                            .padding(10.toDp())
                            .semantics { contentDescription = "Test2" }
                    )
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top

        assertEquals(size, resultWidth)
        assertEquals(size, resultHeight)
    }

    @Test
    fun testReportedBounds_withOffset() {
        val size = 100
        val offset = 10
        val density = Density(1f)
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                with(LocalDensity.current) {
                    Column {
                        Box(
                            Modifier
                                .size(size.toDp())
                                .offset(offset.toDp(), offset.toDp())
                                .testTag("tag")
                                .semantics { contentDescription = "Test" }
                        )
                    }
                }
            }
        }

        val node = rule.onNodeWithTag("tag").fetchSemanticsNode()
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(node.id)!!

        val rect = android.graphics.Rect()
        accessibilityNodeInfo.getBoundsInScreen(rect)
        val resultWidth = rect.right - rect.left
        val resultHeight = rect.bottom - rect.top
        val resultInLocalCoords = androidComposeView.screenToLocal(rect.toComposeRect().topLeft)

        assertEquals(size, resultWidth)
        assertEquals(size, resultHeight)
        assertEquals(10f, resultInLocalCoords.x, 0.001f)
        assertEquals(10f, resultInLocalCoords.y, 0.001f)
    }

    @Test
    fun testSemanticsNodePositionAndBounds_doesNotThrow_whenLayoutNodeNotAttached() {
        var emitNode by mutableStateOf(true)
        container.setContent {
            if (emitNode) {
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).testTag("tag"))
                }
            }
        }

        val semanticNode = rule.onNodeWithTag("tag").fetchSemanticsNode()
        rule.runOnIdle {
            emitNode = false
        }

        rule.runOnIdle {
            assertEquals(Offset.Zero, semanticNode.positionInRoot)
            assertEquals(Offset.Zero, semanticNode.positionInWindow)
            assertEquals(androidx.compose.ui.geometry.Rect.Zero, semanticNode.boundsInRoot)
            assertEquals(androidx.compose.ui.geometry.Rect.Zero, semanticNode.boundsInWindow)
        }
    }

    @Test
    fun testSemanticsSort_doesNotThrow_whenCoordinatorNotAttached() {
        container.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).testTag("parent")) {
                    Box(Modifier.size(100.toDp()).testTag("child"))
                }
            }
        }

        val parent = rule.onNodeWithTag("parent").fetchSemanticsNode()
        val child = rule.onNodeWithTag("child").fetchSemanticsNode()

        rule.runOnIdle {
            child.layoutNode.innerCoordinator.onRelease()
        }

        rule.runOnIdle {
            assertEquals(1, parent.unmergedChildren(true).size)
            assertEquals(0, child.unmergedChildren(true).size)
        }
    }

    @Test
    fun testSemanticsSort_doesNotThrow_whenCoordinatorNotAttached_compare() {
        container.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).testTag("parent")) {
                    Box(Modifier.size(100.toDp()).testTag("child1")) {
                        Box(Modifier.size(50.toDp()).testTag("grandChild1"))
                    }
                    Box(Modifier.size(100.toDp()).testTag("child2")) {
                        Box(Modifier.size(50.toDp()).testTag("grandChild2"))
                    }
                }
            }
        }

        val parent = rule.onNodeWithTag("parent").fetchSemanticsNode()
        val grandChild1 = rule.onNodeWithTag("grandChild1").fetchSemanticsNode()
        val grandChild2 = rule.onNodeWithTag("grandChild2").fetchSemanticsNode()
        rule.runOnIdle {
            grandChild1.layoutNode.innerCoordinator.onRelease()
            grandChild2.layoutNode.innerCoordinator.onRelease()
        }

        rule.runOnIdle {
            assertEquals(2, parent.unmergedChildren(true).size)
        }
    }

    @Test
    fun testFakeNodeCreated_forContentDescriptionSemantics() {
        container.setContent {
            Column(
                Modifier
                    .semantics(true) { contentDescription = "Test" }
                    .testTag("Column")
            ) {
                BasicText("Text")
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Hello" })
                }
            }
        }

        val columnNode = rule.onNodeWithTag("Column", true).fetchSemanticsNode()
        val firstChild = columnNode.replacedChildren.firstOrNull()
        assertNotNull(firstChild)
        assertTrue(firstChild!!.isFake)
        assertEquals(
            firstChild.unmergedConfig.getOrNull(SemanticsProperties.ContentDescription)!!.first(),
            "Test"
        )
    }

    @Test
    fun testFakeNode_createdForButton() {
        container.setContent {
            Column(Modifier.clickable(role = Role.Button) {}.testTag("button")) {
                BasicText("Text")
            }
        }

        val buttonNode = rule.onNodeWithTag("button", true).fetchSemanticsNode()
        val lastChild = buttonNode.replacedChildren.lastOrNull()
        assertNotNull("Button has no children", lastChild)
        assertTrue("Last child should be fake Button role node", lastChild!!.isFake)
        assertEquals(
            Role.Button,
            lastChild.unmergedConfig.getOrNull(SemanticsProperties.Role),
        )
    }

    @Test
    fun testFakeNode_notCreatedForButton_whenNoChildren() {
        container.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).clickable(role = Role.Button) {}.testTag("button"))
            }
        }
        val buttonNode = rule.onNodeWithTag("button").fetchSemanticsNode()
        assertFalse(buttonNode.unmergedChildren().any { it.isFake })
        val info = provider.createAccessibilityNodeInfo(buttonNode.id)!!
        assertEquals("android.widget.Button", info.className)
    }

    @Test
    fun testFakeNode_reportParentBoundsAsFakeNodeBounds() {
        val density = Density(2f)
        val tag = "button"
        container.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                with(density) {
                    Box(Modifier.size(100.toDp()).clickable(role = Role.Button) {}.testTag(tag)) {
                        BasicText("Example")
                    }
                }
            }
        }

        // Button node
        val parentNode = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val parentBounds = Rect()
        provider.createAccessibilityNodeInfo(parentNode.id)!!
            .getBoundsInScreen(parentBounds)

        // Button role fake node
        val fakeRoleNode = parentNode.unmergedChildren(includeFakeNodes = true).last()
        val fakeRoleNodeBounds = Rect()
        provider.createAccessibilityNodeInfo(fakeRoleNode.id)!!
            .getBoundsInScreen(fakeRoleNodeBounds)

        assertEquals(parentBounds, fakeRoleNodeBounds)
    }

    @Test
    fun testContentDescription_withFakeNode_mergedCorrectly() {
        val testTag = "Column"
        container.setContent {
            Column(
                Modifier
                    .testTag(testTag)
                    .semantics(true) { contentDescription = "Hello" }
            ) {
                Box(Modifier.semantics { contentDescription = "World" })
            }
        }

        rule.onNodeWithTag(testTag).assertContentDescriptionEquals("Hello", "World")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_notSet_whenAncestorMergesDescendants() {
        container.setContent {
            Column(Modifier.semantics(true) { }) {
                BasicText("test", Modifier.testTag("child"))
            }
        }

        val childNode = rule.onNodeWithTag("child", useUnmergedTree = true).fetchSemanticsNode()
        val childInfo = provider.createAccessibilityNodeInfo(childNode.id)!!
        assertEquals(childInfo.isScreenReaderFocusable, false)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_set_whenAncestorDoesNotMerge() {
        container.setContent {
            Column(Modifier.semantics(false) { }) {
                BasicText("test", Modifier.testTag("child"))
            }
        }

        val childNode = rule.onNodeWithTag("child", useUnmergedTree = true).fetchSemanticsNode()
        val childInfo = provider.createAccessibilityNodeInfo(childNode.id)!!
        assertEquals(childInfo.isScreenReaderFocusable, true)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_notSet_whenChildNotSpeakable() {
        container.setContent {
            Column(Modifier.semantics(false) { }) {
                Box(Modifier.testTag("child").size(100.dp))
            }
        }

        val childNode = rule.onNodeWithTag("child", useUnmergedTree = true).fetchSemanticsNode()
        val childInfo = provider.createAccessibilityNodeInfo(childNode.id)!!
        assertEquals(childInfo.isScreenReaderFocusable, false)
    }

    @Test
    fun testImageRole_notSet_whenAncestorMergesDescendants() {
        container.setContent {
            Column(Modifier.semantics(true) { }) {
                Image(ImageBitmap(100, 100), "Image", Modifier.testTag("image"))
            }
        }

        val imageNode = rule.onNodeWithTag("image", true).fetchSemanticsNode()
        val imageInfo = provider.createAccessibilityNodeInfo(imageNode.id)!!
        assertEquals(ClassName, imageInfo.className)
    }

    @Test
    fun testImageRole_set_whenAncestorDoesNotMerge() {
        container.setContent {
            Column(Modifier.semantics { isEnabled() }) {
                Image(ImageBitmap(100, 100), "Image", Modifier.testTag("image"))
            }
        }

        val imageNode = rule.onNodeWithTag("image", true).fetchSemanticsNode()
        val imageInfo = provider.createAccessibilityNodeInfo(imageNode.id)!!
        assertEquals("android.widget.ImageView", imageInfo.className)
    }

    @Test
    fun testImageRole_set_whenImageItseldMergesDescendants() {
        container.setContent {
            Column(Modifier.semantics(true) {}) {
                Image(
                    ImageBitmap(100, 100),
                    "Image",
                    Modifier.testTag("image").semantics(true) { /* imitate clickable node */ }
                )
            }
        }

        val imageNode = rule.onNodeWithTag("image", true).fetchSemanticsNode()
        val imageInfo = provider.createAccessibilityNodeInfo(imageNode.id)!!
        assertEquals("android.widget.ImageView", imageInfo.className)
    }

    @Test
    fun testScrollableContainer_scrollViewClassNotSet_whenCollectionInfo() {
        val tagColumn = "lazy column"
        val tagRow = "scrollable row"
        container.setContent {
            LazyColumn(Modifier.testTag(tagColumn)) {
                item {
                    Row(
                        Modifier
                            .testTag(tagRow)
                            .scrollable(rememberScrollState(), Orientation.Horizontal)
                    ) {
                        BasicText("test")
                    }
                }
            }
        }

        val columnNode = rule.onNodeWithTag(tagColumn).fetchSemanticsNode()
        val columnInfo = provider.createAccessibilityNodeInfo(columnNode.id)!!
        assertNotEquals("android.widget.ScrollView", columnInfo.className)

        val rowNode = rule.onNodeWithTag(tagRow).fetchSemanticsNode()
        val rowInfo = provider.createAccessibilityNodeInfo(rowNode.id)!!
        assertNotEquals("android.widget.HorizontalScrollView", rowInfo.className)
    }

    @Test
    fun testTransparentNode_withAlphaModifier_notAccessible() {
        container.setContent {
            Column(Modifier.testTag("tag")) {
                val modifier = Modifier.size(100.dp)
                Box(Modifier.alpha(0f)) {
                    Box(modifier.semantics { contentDescription = "test" })
                }
                Box(Modifier.alpha(0f).then(modifier).semantics { contentDescription = "test" })
                Box(Modifier.alpha(0f).semantics { contentDescription = "test" }.then(modifier))
                Box(modifier.alpha(0f).semantics { contentDescription = "test" })
                Box(
                    Modifier
                        .size(100.dp)
                        .alpha(0f)
                        .shadow(2.dp)
                        .semantics { contentDescription = "test" }
                )
            }
        }

        rule.onNodeWithTag("tag").fetchSemanticsNode()

        val nodesWithContentDescr = androidComposeView.semanticsOwner
            .getAllUncoveredSemanticsNodesToMap()
            .filter {
                it.value.semanticsNode.config.contains(SemanticsProperties.ContentDescription)
            }
        assertEquals(nodesWithContentDescr.size, 5)
        nodesWithContentDescr.forEach {
            val node = it.value.semanticsNode
            val info = provider.createAccessibilityNodeInfo(node.id)!!
            assertEquals(false, info.isVisibleToUser)
        }
    }

    @Test
    fun testVisibleNode_withAlphaModifier_accessible() {
        container.setContent {
            Column(Modifier.testTag("tag")) {
                val modifier = Modifier.size(100.dp)
                Box(Modifier.semantics { contentDescription = "test" }.then(modifier).alpha(0f))
                Box(Modifier.semantics { contentDescription = "test" }.alpha(0f).then(modifier))
                Box(modifier.semantics { contentDescription = "test" }.alpha(0f))
            }
        }

        rule.onNodeWithTag("tag").fetchSemanticsNode()

        val nodesWithContentDescr = androidComposeView.semanticsOwner
            .getAllUncoveredSemanticsNodesToMap()
            .filter {
                it.value.semanticsNode.config.contains(SemanticsProperties.ContentDescription)
            }

        assertEquals(nodesWithContentDescr.size, 3)
        nodesWithContentDescr.forEach {
            val node = it.value.semanticsNode
            val info = provider.createAccessibilityNodeInfo(node.id)!!
            assertEquals(true, info.isVisibleToUser)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun progressSemantics_mergesSemantics_forTalkback() {
        container.setContent {
            Box(Modifier.progressSemantics(0.5f).testTag("box")) {
                 BasicText("test", Modifier.testTag("child"))
            }
        }

        val node = rule.onNodeWithTag("box", useUnmergedTree = true).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!
        assertEquals(info.isScreenReaderFocusable, true)

        val childNode = rule.onNodeWithTag("child", useUnmergedTree = true).fetchSemanticsNode()
        val childInfo = provider.createAccessibilityNodeInfo(childNode.id)!!
        assertEquals(childInfo.isScreenReaderFocusable, false)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun indeterminateProgressSemantics_mergesSemantics_forTalkback() {
        container.setContent {
            Box(Modifier.progressSemantics().testTag("box")) {
                 BasicText("test", Modifier.testTag("child"))
            }
        }

        val node = rule.onNodeWithTag("box", useUnmergedTree = true).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)!!
        assertEquals(info.isScreenReaderFocusable, true)

        val childNode = rule.onNodeWithTag("child", useUnmergedTree = true).fetchSemanticsNode()
        val childInfo = provider.createAccessibilityNodeInfo(childNode.id)!!
        assertEquals(childInfo.isScreenReaderFocusable, false)
    }

    @Test
    fun accessibilityStateChangeListenerRemoved_onDetach() {
        delegate.accessibilityForceEnabledForTesting = false

        rule.runOnIdle {
            assertTrue(androidComposeView.isAttachedToWindow)
        }

        rule.runOnUiThread {
            container.removeView(androidComposeView)
        }

        rule.runOnIdle {
            assertFalse(androidComposeView.isAttachedToWindow)

            val removed = delegate.accessibilityManager.removeAccessibilityStateChangeListener(
                delegate.enabledStateListener
            )
            assertFalse(removed)
        }
    }

    @Test
    fun touchExplorationChangeListenerRemoved_onDetach() {
        delegate.accessibilityForceEnabledForTesting = false

        rule.runOnIdle {
            assertTrue(androidComposeView.isAttachedToWindow)
        }

        rule.runOnUiThread {
            container.removeView(androidComposeView)
        }

        rule.runOnIdle {
            assertFalse(androidComposeView.isAttachedToWindow)

            val removed = delegate.accessibilityManager.removeTouchExplorationStateChangeListener(
                delegate.touchExplorationStateListener
            )
            assertFalse(removed)
        }
    }

    @Test
    fun isEnabled_returnsFalse_whenUIAutomatorIsTheOnlyEnabledService() {
        delegate.accessibilityForceEnabledForTesting = false

        rule.runOnIdle {
            // This test implies that UIAutomator is enabled and is the only enabled a11y service
            assertTrue(delegate.accessibilityManager.isEnabled)
            assertFalse(delegate.isEnabled)
        }
    }

    @Test
    fun canScroll_returnsFalse_whenAccessedOutsideOfMainThread() {
        container.setContent {
            Box(
                Modifier.semantics(mergeDescendants = true) { }
            ) {
                Column(
                    Modifier
                        .size(50.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    repeat(10) {
                        Box(Modifier.size(30.dp))
                    }
                }
            }
        }

        rule.runOnIdle {
            androidComposeView.dispatchTouchEvent(
                createHoverMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
            )

            assertTrue(androidComposeView.canScrollVertically(1))
        }

        assertFalse(androidComposeView.canScrollVertically(1))
    }

    private fun eventIndex(list: List<AccessibilityEvent>, event: AccessibilityEvent): Int {
        for (i in list.indices) {
            if (ReflectionEquals(list[i], null).matches(event)) {
                return i
            }
        }
        return -1
    }

    private fun containsEvent(list: List<AccessibilityEvent>, event: AccessibilityEvent): Boolean {
        return eventIndex(list, event) != -1
    }

    private fun getAccessibilityEventSourceSemanticsNodeId(event: AccessibilityEvent): Int {
        val getSourceNodeIdMethod = AccessibilityRecord::class.java
            .getDeclaredMethod("getSourceNodeId")
        getSourceNodeIdMethod.isAccessible = true
        return (getSourceNodeIdMethod.invoke(event) as Long shr 32).toInt()
    }

    private fun waitForSubtreeEventToSendAndVerify(verify: () -> Unit) {
        // TODO(aelias): Make this wait after the 100ms delay to check the second batch is also correct
        rule.waitForIdle()
        verify()
    }

    private fun waitForSubtreeEventToSend() {
        // When the subtree events are sent, we will also update our previousSemanticsNodes,
        // which will affect our next accessibility events from semantics tree comparison.
        rule.mainClock.advanceTimeBy(5000)
        rule.waitForIdle()
    }

    private fun createMovementGranularityCharacterArgs(): Bundle {
        return Bundle().apply {
            this.putInt(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
            )
            this.putBoolean(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false
            )
        }
    }

    private fun createSelectionChangedFromIndexOneToOneEvent(
        textNode: SemanticsNode
    ): AccessibilityEvent {
        return delegate.createEvent(
            textNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ).apply {
            this.fromIndex = 1
            this.toIndex = 1
            getTraversedText(textNode)?.let {
                this.itemCount = it.length
                this.text.add(it)
            }
        }
    }

    private fun createCharacterTraverseFromIndexZeroEvent(
        textNode: SemanticsNode
    ): AccessibilityEvent {
        return delegate.createEvent(
            textNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
        ).apply {
            this.fromIndex = 0
            this.toIndex = 1
            this.action = AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            this.movementGranularity = AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
            getTraversedText(textNode)?.let { this.text.add(it) }
        }
    }

    private fun getTraversedText(textNode: SemanticsNode): String? {
        return (
            textNode.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: textNode.config.getOrNull(SemanticsProperties.Text)?.joinToString(",")
            )
    }
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably.  Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeables = measurables.map {
                it.measure(constraints)
            }
            val (width, height) = with(placeables.filterNotNull()) {
                Pair(
                    max(
                        maxByOrNull { it.width }?.width ?: 0,
                        constraints.minWidth
                    ),
                    max(
                        maxByOrNull { it.height }?.height ?: 0,
                        constraints.minHeight
                    )
                )
            }
            layout(width, height) {
                for (placeable in placeables) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

/**
 * A simple SubComposeLayout which lays [contentOne] at [positionOne] and lays [contentTwo] at
 * [positionTwo]. [contentOne] is placed first and [contentTwo] is placed second. Therefore, the
 * semantics node for [contentOne] is before semantics node for [contentTwo] in
 * [SemanticsNode.children].
 */
@Composable
private fun SimpleSubcomposeLayout(
    modifier: Modifier = Modifier,
    contentOne: @Composable () -> Unit,
    positionOne: Offset,
    contentTwo: @Composable () -> Unit,
    positionTwo: Offset
) {
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val placeablesOne = subcompose(TestSlot.First, contentOne).fastMap {
                it.measure(looseConstraints)
            }

            val placeablesTwo = subcompose(TestSlot.Second, contentTwo).fastMap {
                it.measure(looseConstraints)
            }

            // Placing to control drawing order to match default elevation of each placeable
            placeablesOne.fastForEach {
                it.place(positionOne.x.toInt(), positionOne.y.toInt())
            }
            placeablesTwo.fastForEach {
                it.place(positionTwo.x.toInt(), positionTwo.y.toInt())
            }
        }
    }
}

/**
 * A simple layout which lays the first placeable in a top bar position, the last placeable in a
 * bottom bar position, and all the content in between.
 */
@Composable
fun ScaffoldedSubcomposeLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var yPosition = 0
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        layout(layoutWidth, layoutHeight) {
            val topPlaceables = subcompose(ScaffoldedSlots.Top, topBar).fastMap {
                it.measure(looseConstraints)
            }

            val contentPlaceables = subcompose(ScaffoldedSlots.Content, content).fastMap {
                it.measure(looseConstraints)
            }

            val bottomPlaceables = subcompose(ScaffoldedSlots.Bottom, bottomBar).fastMap {
                it.measure(looseConstraints)
            }

            topPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
            contentPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
            bottomPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
        }
    }
}

private enum class TestSlot { First, Second }
private enum class ScaffoldedSlots { Top, Content, Bottom }
