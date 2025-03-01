/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.draganddrop

import androidx.collection.ArraySet
import androidx.compose.ui.geometry.Offset
import java.awt.dnd.DropTargetEvent as AwtDropTargetEvent

actual class DragAndDropTransfer

/**
 * AWT [DragAndDropEvent] which delegates to a [AwtDropTargetEvent]
 */
actual class DragAndDropEvent(
    internal val dropTargetEvent: AwtDropTargetEvent,
    internal actual val interestedNodes: ArraySet<DragAndDropModifierNode> = ArraySet()
)

internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = TODO()
