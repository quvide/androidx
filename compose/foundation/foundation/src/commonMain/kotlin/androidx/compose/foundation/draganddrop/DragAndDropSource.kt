/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.draganddrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropInfo
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTransfer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

/**
 * A scope that allows for the detection of the start of a drag and drop gesture, and subsequently
 * starting a drag and drop session.
 */
@ExperimentalFoundationApi
interface DragAndDropSourceScope : PointerInputScope {
    /**
     * Starts a drag and drop session with [transfer] as the data to be transferred on gesture
     * completion
     */
    fun startTransfer(transfer: DragAndDropTransfer)
}

/**
 * A Modifier that allows an element it is applied to to be treated like a source for
 * drag and drop operations.
 *
 * Learn how to use [Modifier.dragAndDropSource] while providing a custom drag shadow:
 * @sample androidx.compose.foundation.samples.DragAndDropSourceWithColoredDragShadowSample
 *
 * @param onDrawDragShadow provides the visual representation of the item dragged during the
 * drag and drop gesture.
 * @param block A lambda with a [DragAndDropSourceScope] as a receiver
 * which provides a [PointerInputScope] to detect the drag gesture, after which a drag and drop
 * gesture can be started with [DragAndDropSourceScope.startTransfer].
 *
 */
@ExperimentalFoundationApi
fun Modifier.dragAndDropSource(
    onDrawDragShadow: DrawScope.() -> Unit,
    block: suspend DragAndDropSourceScope.() -> Unit
): Modifier = this then DragAndDropSourceElement(
    onDrawDragShadow = onDrawDragShadow,
    dragAndDropSourceHandler = block,
)

@ExperimentalFoundationApi
private data class DragAndDropSourceElement(
    /**
     * @see Modifier.dragAndDropSource
     */
    val onDrawDragShadow: DrawScope.() -> Unit,
    /**
     * @see Modifier.dragAndDropSource
     */
    val dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : ModifierNodeElement<DragAndDropSourceNode>() {
    override fun create() = DragAndDropSourceNode(
        onDrawDragShadow = onDrawDragShadow,
        dragAndDropSourceHandler = dragAndDropSourceHandler,
    )

    override fun update(node: DragAndDropSourceNode) = with(node) {
        onDrawDragShadow = this@DragAndDropSourceElement.onDrawDragShadow
        dragAndDropSourceHandler = this@DragAndDropSourceElement.dragAndDropSourceHandler
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSource"
        properties["onDrawDragShadow"] = onDrawDragShadow
        properties["dragAndDropSourceHandler"] = dragAndDropSourceHandler
    }
}

@ExperimentalFoundationApi
internal class DragAndDropSourceNode(
    var onDrawDragShadow: DrawScope.() -> Unit,
    var dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : DelegatingNode(),
    LayoutAwareModifierNode {

    private var size: IntSize = IntSize.Zero

    init {
        val dragAndDropModifierNode = delegate(
            DragAndDropModifierNode()
        )

        delegate(
            SuspendingPointerInputModifierNode {
                dragAndDropSourceHandler(
                    object : DragAndDropSourceScope, PointerInputScope by this {
                        override fun startTransfer(transfer: DragAndDropTransfer) =
                            dragAndDropModifierNode.drag(
                                DragAndDropInfo(
                                    transfer = transfer,
                                    size = size.toSize(),
                                    onDrawDragShadow = onDrawDragShadow
                                )
                            )
                    }
                )
            }
        )
    }

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }
}
