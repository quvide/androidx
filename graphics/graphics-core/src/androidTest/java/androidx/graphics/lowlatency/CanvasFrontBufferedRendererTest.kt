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

package androidx.graphics.lowlatency

import android.app.UiAutomation
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Paint
import android.hardware.DataSpace
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.graphics.drawSquares
import androidx.graphics.opengl.SurfaceViewTestActivity
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlUtils
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CanvasFrontBufferedRendererTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testFrontBufferedLayerRender() {
        if (!isSupported()) {
            return
        }
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                canvas.drawColor(Color.RED)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                canvas.drawColor(Color.BLUE)
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED ==
                    bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiBufferedLayerRender() {
        if (!isSupported()) {
            return
        }
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                canvas.drawColor(Color.RED)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                canvas.drawColor(Color.BLUE)
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
                renderer?.commit()
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (Math.abs(
                    Color.red(Color.BLUE) - Color.red(
                        bitmap.getPixel(
                            coords[0] + width / 2,
                            coords[1] + height / 2
                        )
                    )
                ) < 2) &&
                    (Math.abs(
                        Color.green(Color.BLUE) - Color.green(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2) &&
                    (Math.abs(
                        Color.blue(Color.BLUE) - Color.blue(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2)
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderMultiBufferLayer() {
        val squareSize = 100f
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Int> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Int
            ) {
                // NO-OP we do not render to the front buffered layer in this test case
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Int>
            ) {
                drawSquares(
                    canvas,
                    bufferWidth,
                    bufferHeight,
                    Color.RED,
                    Color.BLACK,
                    Color.YELLOW,
                    Color.BLUE
                )
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Int>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                val colors = listOf(Color.RED, Color.BLACK, Color.YELLOW, Color.BLUE)
                renderer?.renderMultiBufferedLayer(colors)
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            with(surfaceView!!) {
                getLocationOnScreen(coords)
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                val topLeft = bitmap.getPixel(
                    coords[0] + (squareSize / 4).toInt(),
                    coords[1] + (squareSize / 4).toInt()
                )
                val topRight = bitmap.getPixel(
                    coords[0] + (squareSize * 3f / 4f).roundToInt(),
                    coords[1] + (squareSize / 4).toInt()
                )
                val bottomLeft = bitmap.getPixel(
                    coords[0] + (squareSize / 4f).toInt(),
                    coords[1] + (squareSize * 3f / 4f).roundToInt()
                )
                val bottomRight = bitmap.getPixel(
                    coords[0] + (squareSize * 3f / 4f).roundToInt(),
                    coords[1] + (squareSize * 3f / 4f).roundToInt()
                )
                Color.RED == topLeft &&
                    Color.BLACK == topRight &&
                    Color.YELLOW == bottomLeft &&
                    Color.BLUE == bottomRight
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testCancelFrontBufferLayerRender() {
        if (!isSupported()) {
            return
        }
        val squareSize = 100f
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Int> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Int
            ) {
                canvas.drawColor(param)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Int>
            ) {
                for (p in params) {
                    canvas.drawColor(p)
                }
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Int>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                with(renderer!!) {
                    renderFrontBufferedLayer(Color.BLUE)
                    commit()
                    renderFrontBufferedLayer(Color.RED)
                    cancel()
                }
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            with(surfaceView!!) {
                getLocationOnScreen(coords)
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                val pixel = bitmap.getPixel(
                    coords[0] + (squareSize / 2).toInt(),
                    coords[1] + (squareSize / 2).toInt()
                )
                // After cancel is invoked the front buffered layer should not be visible
                Color.BLUE == pixel
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testClear() {
        if (!isSupported()) {
            return
        }

        val renderLatch = CountDownLatch(2)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Int> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Int
            ) {
                canvas.drawColor(param)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Int>
            ) {
                for (p in params) {
                    canvas.drawColor(p)
                }
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Int>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                with(renderer!!) {
                    renderFrontBufferedLayer(Color.BLUE)
                    commit()
                    renderFrontBufferedLayer(Color.RED)
                    clear()
                }
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            with(surfaceView!!) {
                getLocationOnScreen(coords)
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                with(bitmap) {
                    val leftQuadX = coords[0] + width / 4
                    val rightQuadX = leftQuadX + width / 2
                    val topQuadY = coords[1] + height / 4
                    val bottomQuadY = topQuadY + height / 2
                    Color.WHITE == getPixel(leftQuadX, topQuadY) &&
                    Color.WHITE == getPixel(rightQuadX, topQuadY) &&
                    Color.WHITE == getPixel(leftQuadX, bottomQuadY) &&
                    Color.WHITE == getPixel(rightQuadX, bottomQuadY)
                }
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiBufferedContentsNotPersisted() {
        val screenWidth = SurfaceViewTestActivity.WIDTH
        val screenHeight = SurfaceViewTestActivity.HEIGHT
        val firstRenderLatch = CountDownLatch(1)
        val commitLatch = AtomicReference<CountDownLatch?>()
        val paint = Paint().apply { color = Color.RED }
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Float> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Float
            ) {
                canvas.drawRect(
                    param,
                    0f,
                    screenHeight.toFloat(),
                    param + screenWidth,
                    paint
                )
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Float>
            ) {

                for (param in params) {
                    canvas.drawRect(
                        param,
                        0f,
                        screenHeight.toFloat(),
                        param + screenWidth,
                        paint
                    )
                }
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                firstRenderLatch.countDown()
                                commitLatch.get()?.countDown()
                            }
                        })
                } else {
                    firstRenderLatch.countDown()
                    commitLatch.get()?.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Float>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView().apply {
                        setZOrderOnTop(true)
                    }
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertTrue(firstRenderLatch.await(3000, TimeUnit.MILLISECONDS))

            renderer?.renderFrontBufferedLayer(0f)
            renderer?.commit()
            renderer?.renderFrontBufferedLayer(screenWidth / 2f)
            commitLatch.set(CountDownLatch(1))
            renderer?.commit()

            Assert.assertTrue(commitLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (bitmap.getPixel(
                    coords[0] + width / 4, coords[1] + height / 2
                ) == Color.WHITE) &&
                    (bitmap.getPixel(
                        coords[0] + 3 * width / 4 - 1,
                        coords[1] + height / 2
                    ) == Color.RED)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testParentLayerRotate90() = parentLayerRotationTest(UiAutomation.ROTATION_FREEZE_90)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testParentLayerRotate180() = parentLayerRotationTest(UiAutomation.ROTATION_FREEZE_180)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testParentLayerRotate270() = parentLayerRotationTest(UiAutomation.ROTATION_FREEZE_270)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testParentLayerRotate0() = parentLayerRotationTest(UiAutomation.ROTATION_FREEZE_0)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun parentLayerRotationTest(rotation: Int) {
        if (!isSupported()) {
            return
        }
        var surfaceView: SurfaceView? = null
        val renderLatch = CountDownLatch(1)
        val topLeftColor = Color.RED
        val topRightColor = Color.YELLOW
        val bottomRightColor = Color.BLACK
        val bottomLeftColor = Color.BLUE
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                // NO-OP
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                drawSquares(
                    canvas,
                    bufferWidth,
                    bufferHeight,
                    topLeft = topLeftColor,
                    topRight = topRightColor,
                    bottomRight = bottomRightColor,
                    bottomLeft = bottomLeftColor
                )
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }

        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var renderer: CanvasFrontBufferedRenderer<Any>? = null

        try {
            Assert.assertTrue(automation.setRotation(rotation))
            automation.waitForIdle(1000, 3000)

            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
                renderer?.commit()
            }

            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                val topLeftActual = bitmap.getPixel(
                    coords[0] + width / 4,
                    coords[1] + height / 4
                )
                val topRightActual = bitmap.getPixel(
                    coords[0] + width / 2 + width / 4,
                    coords[1] + height / 4
                )
                val bottomRightActual = bitmap.getPixel(
                    coords[0] + width / 2 + width / 4,
                    coords[1] + height / 2 + height / 4
                )
                val bottomLeftActual = bitmap.getPixel(
                    coords[0] + width / 4,
                    coords[1] + height / 2 + height / 4
                )
                topLeftActual == topLeftColor &&
                    topRightActual == topRightColor &&
                    bottomRightActual == bottomRightColor &&
                    bottomLeftActual == bottomLeftColor
            }
        } finally {
            automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
            automation.waitForIdle(1000, 3000)
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testReleaseRemovedSurfaceCallbacks() {
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                // no-op
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                // no-op
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceViewTestActivity.TestSurfaceView? = null
        val createLatch = CountDownLatch(1)
        ActivityScenario.launch(SurfaceViewTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                surfaceView = it.getSurfaceView()
                renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                createLatch.countDown()
            }

        Assert.assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))
        // Capture surfaceView with local val to avoid Kotlin warnings regarding the surfaceView
        // parameter changing potentially
        val resolvedSurfaceView = surfaceView
        try {
            if (resolvedSurfaceView != null) {
                Assert.assertEquals(1, resolvedSurfaceView.getCallbackCount())
                renderer?.release(true)
                renderer = null
                Assert.assertEquals(0, resolvedSurfaceView.getCallbackCount())
            } else {
                fail("Unable to resolve SurfaceView, was the Activity created?")
            }
        } finally {
            renderer?.release(true)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testFrontBufferRenderAfterActivityResume() {
        var renderCount = 0
        val surfaceChangedLatch = CountDownLatch(1)
        val renderStartLatch = CountDownLatch(1)
        val renderCountLatch = CountDownLatch(2)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                renderStartLatch.countDown()
                // Intentionally simulate slow rendering by waiting for a surface change callback
                // this helps verify the scenario where a change in surface
                // (ex Activity stop -> resume)
                surfaceChangedLatch.await(3000, TimeUnit.MILLISECONDS)
                renderCount++
                renderCountLatch.countDown()
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                // no-op
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        val stopLatch = CountDownLatch(1)
        var testActivity: SurfaceViewTestActivity? = null
        var surfaceView: SurfaceViewTestActivity.TestSurfaceView? = null
        val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                testActivity = it
                surfaceView = it.getSurfaceView()
                renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            renderer!!.renderFrontBufferedLayer(Any())
        }
        Assert.assertTrue(renderStartLatch.await(3000, TimeUnit.MILLISECONDS))
        // Go back to the Activity stop state to simulate an application moving to the background
        scenario.moveToState(Lifecycle.State.CREATED).onActivity {
            stopLatch.countDown()
        }

        Assert.assertTrue(stopLatch.await(3000, TimeUnit.MILLISECONDS))
        surfaceView!!.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {
                // NO-OP
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                // On Activity resume, a surface change callback will be invoked. At this point
                // a render is still happening. After this is signalled the render will complete
                // and the release callback will be invoked after we are tearing down/ recreating
                // the front buffered renderer state.
                surfaceChangedLatch.countDown()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // NO-OP
            }
        })

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            renderer!!.renderFrontBufferedLayer(Any())
        }

        try {
            // Verify that after resuming, we did not unintentionally release the newly created
            // front buffered renderer and the subsequent render request does occur
            Assert.assertTrue(renderCountLatch.await(3000, TimeUnit.MILLISECONDS))
            Assert.assertEquals(2, renderCount)
        } finally {
            renderer?.release(true)
            val destroyLatch = CountDownLatch(1)
            testActivity!!.setOnDestroyCallback { destroyLatch.countDown() }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            Assert.assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testFrontBufferRenderWithDisplayP3() {
        if (!isSupported()) {
            return
        }

        val displayP3ColorSpace = ColorSpace.getFromDataSpace(DataSpace.DATASPACE_DISPLAY_P3)!!
        val darkRed = Color.pack(0x6F / 255f, 0f, 0f, 1f, displayP3ColorSpace)
        val converted = Color.convert(
            Color.red(darkRed),
            Color.green(darkRed),
            Color.blue(darkRed),
            Color.alpha(darkRed),
            displayP3ColorSpace,
            ColorSpace.get(ColorSpace.Named.SRGB)
        )
        assertTrue(Color.isSrgb(converted))
        val argb = Color.toArgb(converted)
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                canvas.drawColor(darkRed)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                // NO-OP
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                transaction.setDataSpace(
                    frontBufferedLayerSurfaceControl,
                    DataSpace.DATASPACE_DISPLAY_P3
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        // Create a lambda to configure the CanvasFrontBufferedRenderer instance as
        // the AndroidTest runner will fail on older API levels even though there is
        // a minSdk check at the beginning of the test case.
        // The test runner will still attempt to resolve all methods on a class
        // and will fail to resolve the ColorSpace API for Android platform versions that
        // do not have it.
        val configureRenderer: (CanvasFrontBufferedRenderer<*>) -> Unit = { target ->
            target.colorSpace = displayP3ColorSpace
        }
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks).apply {
                        configureRenderer.invoke(this)
                    }
                }
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }
            SurfaceControlUtils.validateOutput { bitmap ->
                argb == bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testMultiBufferedLayerRenderWithDisplayP3() {
        if (!isSupported()) {
            return
        }

        val displayP3ColorSpace = ColorSpace.getFromDataSpace(DataSpace.DATASPACE_DISPLAY_P3)!!
        val darkRed = Color.pack(0x6F / 255f, 0f, 0f, 1f, displayP3ColorSpace)
        val converted = Color.convert(
            Color.red(darkRed),
            Color.green(darkRed),
            Color.blue(darkRed),
            Color.alpha(darkRed),
            displayP3ColorSpace,
            ColorSpace.get(ColorSpace.Named.SRGB)
        )
        assertTrue(Color.isSrgb(converted))
        val argb = Color.toArgb(converted)
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                // NO-OP
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                canvas.drawColor(darkRed)
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                transaction.setDataSpace(
                    multiBufferedLayerSurfaceControl,
                    DataSpace.DATASPACE_DISPLAY_P3
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        // Create a lambda to configure the CanvasFrontBufferedRenderer instance as
        // the AndroidTest runner will fail on older API levels even though there is
        // a minSdk check at the beginning of the test case.
        // The test runner will still attempt to resolve all methods on a class
        // and will fail to resolve the ColorSpace API for Android platform versions that
        // do not have it.
        val configureRenderer: (CanvasFrontBufferedRenderer<*>) -> Unit = { target ->
            target.colorSpace = displayP3ColorSpace
        }
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks).apply {
                        configureRenderer.invoke(this)
                    }
                }
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
                renderer?.commit()
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }
            SurfaceControlUtils.validateOutput { bitmap ->
                argb == bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun CanvasFrontBufferedRenderer<*>?.blockingRelease(timeoutMillis: Long = 3000) {
        if (this != null) {
            val destroyLatch = CountDownLatch(1)
            release(false) {
                destroyLatch.countDown()
            }
            Assert.assertTrue(destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS))
        } else {
            Assert.fail("CanvasFrontBufferedRenderer is not initialized")
        }
    }

    private fun isSupported(): Boolean =
        // See "b/277225133" these tests pass on cuttlefish + other devices but fail for some reason
        // FTL configured API level 33 emulator instances
        // Additionally some cuttlefish instances don't support rotation based testing (b/277764242)
        isSupportedGphone() && isSupportedCuttlefish()

    private fun isSupportedGphone() =
        !(Build.MODEL.contains("gphone") &&
            (Build.VERSION.SDK_INT == 33 || Build.VERSION.SDK_INT == 30))

    @OptIn(BuildCompat.PrereleaseSdkCheck::class)
    private fun isSupportedCuttlefish() =
        !(Build.MODEL.contains("Cuttlefish") &&
            (Build.VERSION.SDK_INT == 30 || BuildCompat.isAtLeastV()))
}
