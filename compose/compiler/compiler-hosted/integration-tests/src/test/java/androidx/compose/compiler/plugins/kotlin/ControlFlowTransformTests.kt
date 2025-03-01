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

package androidx.compose.compiler.plugins.kotlin

import org.intellij.lang.annotations.Language
import org.junit.Test

class ControlFlowTransformTests(useFir: Boolean) : AbstractControlFlowTransformTests(useFir) {
    @Test
    fun testIfNonComposable(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // No composable calls, so no group generated except for at function boundary
                if (x > 0) {
                    NA()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                NA()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testAND(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Example() {
                B() && B()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<B()>,<B()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_group = B(%composer, 0) && B(%composer, 0)
              tmp0_group
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testOR(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Example() {
                B() || B()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<B()>,<B()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_group = B(%composer, 0) || B(%composer, 0)
              tmp0_group
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfWithCallsInBranch(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Only one composable call in the result blocks, so we can just generate
                // a single group around the whole expression.
                if (x > 0) {
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfElseWithCallsInBranch(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Composable calls in the result blocks, so we can determine static number of
                // groups executed. This means we put a group around the "then" and the
                // "else" blocks
                if (x > 0) {
                    A(a)
                } else {
                    A(b)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<A(a)>")
                A(a, %composer, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<A(b)>")
                A(b, %composer, 0)
                %composer.endReplaceableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfWithCallInCondition(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Since the first condition of an if/else is unconditionally executed, it does not
                // necessitate a group of any kind, so we just end up with the function boundary
                // group
                if (B()) {
                    NA()
                } else {
                    NA()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<B()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (B(%composer, 0)) {
                NA()
              } else {
                NA()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    private fun verifyInlineReturn(
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            $source
        """,
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun A() { }

            @Composable
            fun Text(text: String) { }

            @Composable
            inline fun Wrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = Wrapper {
                content()
            }

            @Composable
            inline fun M2(content: @Composable () -> Unit) = Wrapper {
                Wrapper {
                    content()
                }
            }

            @Composable
            inline fun M3(content: @Composable () -> Unit) = Wrapper {
                Wrapper {
                    Wrapper {
                        content()
                    }
                }
            }

            inline fun <T> Identity(block: () -> T): T = block()

            @Composable
            fun Stack(content: @Composable () -> Unit) = content()
        """
    )

    @Test
    fun testInline_CM3_RFun() = verifyInlineReturn(
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return
                    }
                    A()
                }
                A()
            }

        """,
        """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<M3>,<A()>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (condition) {
                    %composer.endToMarker(tmp0_marker)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer@Test.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_CM3_RFun_CM3_RFun() = verifyInlineReturn(
        """
            @Composable
            fun Test(a: Boolean, b: Boolean) {
                A()
                M3 {
                    A()
                    if (a) {
                        return
                    }
                    A()
                }
                M3 {
                    A()
                    if (b) {
                        return
                    }
                    A()
                }
                A()
            }

        """,
        """
            @Composable
            fun Test(a: Boolean, b: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<M3>,<M3>,<A()>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (a) {
                    %composer.endToMarker(tmp0_marker)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer@Test.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(a, b, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (b) {
                    %composer.endToMarker(tmp0_marker)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer@Test.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(a, b, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_CM3_RM3() = verifyInlineReturn(
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                A()
            }

        """,
        """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<M3>,<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (condition) {
                    %composer.endReplaceableGroup()
                    return@M3
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_Lambda() = verifyComposeIrTransform(
        """
            fun Test(condition: Boolean) {
                T {
                    compose {
                        M1 {
                            if (condition) return@compose
                        }
                    }
                }
            }
        """,
        """
            fun Test(condition: Boolean) {
              T {
                %this%T.compose(composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<M1>:Test.kt")
                  val tmp0_marker = %composer.currentMarker
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    M1({ %composer: Composer?, %changed: Int ->
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "C:Test.kt")
                      if (condition) {
                        %composer.endToMarker(tmp0_marker)
                        if (isTraceInProgress()) {
                          traceEventEnd()
                        }
                        return@composableLambdaInstance
                      }
                      %composer.endReplaceableGroup()
                    }, %composer, 0)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                )
              }
            }
        """,
        """
            import androidx.compose.runtime.*

            class Scope {
                fun compose(block: @Composable () -> Unit) { }
            }

            fun T(block: suspend Scope.() -> Unit) { }

            @Composable
            inline fun M1(block: @Composable () -> Unit) { block() }

            @Composable
            fun Text(text: String) { }
        """
    )

    @Test
    fun testInline_M3_M1_Return_M1() = verifyInlineReturn(
        """
            @Composable
            fun Test_M3_M1_Return_M1(condition: Boolean) {
                A()
                M3 {
                    A()
                    M1 {
                        if (condition) {
                            return@M1
                        }
                    }
                    A()
                }
                A()
            }
        """,
        """
            @Composable
            fun Test_M3_M1_Return_M1(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test_M3_M1_Return_M1)<A()>,<M3>,<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<A()>,<M1>,<A()>:Test.kt")
                  A(%composer, 0)
                  M1({ %composer: Composer?, %changed: Int ->
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C:Test.kt")
                    if (condition) {
                      %composer.endReplaceableGroup()
                      return@M1
                    }
                    %composer.endReplaceableGroup()
                  }, %composer, 0)
                  A(%composer, 0)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test_M3_M1_Return_M1(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_M3_M1_Return_M3() = verifyInlineReturn(
        """
            @Composable
            fun Test_M3_M1_Return_M3(condition: Boolean) {
                A()
                M3 {
                    A()
                    M1 {
                        if (condition) {
                            return@M3
                        }
                    }
                    A()
                }
                A()
            }
        """,
        """
            @Composable
            fun Test_M3_M1_Return_M3(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test_M3_M1_Return_M3)<A()>,<M3>,<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  val tmp0_marker = %composer.currentMarker
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<M1>,<A()>:Test.kt")
                  A(%composer, 0)
                  M1({ %composer: Composer?, %changed: Int ->
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C:Test.kt")
                    if (condition) {
                      %composer.endToMarker(tmp0_marker)
                      return@M3
                    }
                    %composer.endReplaceableGroup()
                  }, %composer, 0)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test_M3_M1_Return_M3(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_M1_W_Return_Func() = verifyInlineReturn(
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean) {
                A()
                M1 {
                    A()
                    while(true) {
                        A()
                        if (condition) {
                            return
                        }
                        A()
                    }
                    A()
                }
                A()
            }

        """,
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(testInline_M1_W_Return_Func)<A()>,<M1>,<A()>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M1({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "*<A()>,<A()>")
                  while (true) {
                    A(%composer, 0)
                    if (condition) {
                      %composer.endToMarker(tmp0_marker)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                      %composer@testInline_M1_W_Return_Func.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                        testInline_M1_W_Return_Func(condition, %composer, updateChangedFlags(%changed or 0b0001))
                      }
                      return
                    }
                    A(%composer, 0)
                  }
                  %composer.endReplaceableGroup()
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                testInline_M1_W_Return_Func(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInline_CM3_Return_M3_CM3_Return_M3() = verifyInlineReturn(
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                A()
            }

        """,
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(testInline_M1_W_Return_Func)<A()>,<M3>,<M3>,<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(%composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (condition) {
                    %composer.endReplaceableGroup()
                    return@M3
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                M3({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (condition) {
                    %composer.endReplaceableGroup()
                    return@M3
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                testInline_M1_W_Return_Func(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun test_CM1_CCM1_RetFun(): Unit = verifyInlineReturn(
        """
            @Composable
            fun test_CM1_CCM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - begin")
                    if (condition) {
                        Text("if - begin")
                        M1 {
                            Text("In CCM1")
                            return@test_CM1_CCM1_RetFun
                        }
                    }
                    Text("M1 - end")
                }
                Text("Root - end")
            }

        """,
        """
            @Composable
            fun test_CM1_CCM1_RetFun(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test_CM1_CCM1_RetFun)<Text("...>,<M1>,<Text("...>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Text("Root - before", %composer, 0b0110)
                M1({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<Text("...>,<Text("...>:Test.kt")
                  Text("M1 - begin", %composer, 0b0110)
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<Text("...>,<M1>")
                  if (condition) {
                    Text("if - begin", %composer, 0b0110)
                    M1({ %composer: Composer?, %changed: Int ->
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "C<Text("...>:Test.kt")
                      Text("In CCM1", %composer, 0b0110)
                      %composer.endToMarker(tmp0_marker)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                      %composer@test_CM1_CCM1_RetFun.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                        test_CM1_CCM1_RetFun(condition, %composer, updateChangedFlags(%changed or 0b0001))
                      }
                      return
                      %composer.endReplaceableGroup()
                    }, %composer, 0)
                  }
                  %composer.endReplaceableGroup()
                  Text("M1 - end", %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("Root - end", %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test_CM1_CCM1_RetFun(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testInlineReturnLabel(): Unit = controlFlow(
        """
            @Composable
            @NonRestartableComposable
            fun CustomTextBroken(condition: Boolean) {
                FakeBox {
                    if (condition) {
                        return@FakeBox
                    }
                    A()
                }
            }
            @Composable
            inline fun FakeBox(content: @Composable () -> Unit) {
                content()
            }
        """,
        """
            @Composable
            @NonRestartableComposable
            fun CustomTextBroken(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(CustomTextBroken)<FakeBo...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              FakeBox({ %composer: Composer?, %changed: Int ->
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C<A()>:Test.kt")
                if (condition) {
                  %composer.endReplaceableGroup()
                  return@FakeBox
                }
                A(%composer, 0)
                %composer.endReplaceableGroup()
              }, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun FakeBox(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "CC(FakeBox)<conten...>:Test.kt")
              content(%composer, 0b1110 and %changed)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInNonInline_NormalComposable() = controlFlow(
        """
            object obj {
                val condition = true
            }

            @Composable
            fun Test(condition: Boolean) {
                if (condition) return
                with (obj) {
                    if (condition) return
                }
                A()
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            object obj {
              val condition: Boolean = true
              static val %stable: Int = 0
            }
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                if (condition) {
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                  }
                  return
                }
                with(obj) {
                  if (condition) {
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                }
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInNonInline_ReadOnlyComposable() = controlFlow(
        """
            import androidx.compose.runtime.currentComposer

            object obj {
                val condition = false
            }

            @Composable
            @ReadOnlyComposable
            fun Calculate(condition: Boolean): Boolean {
                if (condition) return false

                with (obj) {
                    if (condition) return false
                    return currentComposer.inserting
                }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            object obj {
              val condition: Boolean = false
              static val %stable: Int = 0
            }
            @Composable
            @ReadOnlyComposable
            fun Calculate(condition: Boolean, %composer: Composer?, %changed: Int): Boolean {
              sourceInformationMarkerStart(%composer, <>, "C(Calculate):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (condition) {
                val tmp0_return = false
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                sourceInformationMarkerEnd(%composer)
                return tmp0_return
              }
              with(obj) {
                if (condition) {
                  val tmp0_return = false
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  sourceInformationMarkerEnd(%composer)
                  return tmp0_return
                }
                val tmp1_return = %composer.inserting
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                sourceInformationMarkerEnd(%composer)
                return tmp1_return
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              sourceInformationMarkerEnd(%composer)
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInInline_Labeled() = controlFlow(
        """
            @Composable
            fun Test(condition: Boolean) {
                IW iw@ {
                    if (condition) return@iw
                    A()
                }
            }
        """,
        """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<IW>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                IW({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<A()>:Test.kt")
                  if (condition) {
                    %composer.endReplaceableGroup()
                    return@IW
                  }
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                Identity {
                    if (condition) return@Test
                }
                Text("Some more text")
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Text("...>,<Text("...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Text("Some text", %composer, 0b0110)
                Identity {
                  if (condition) {
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                }
                Text("Some more text", %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable_M1() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                M1 {
                    Identity {
                        if (condition) return@Test
                    }
                }
                Text("Some more text")
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Text("...>,<M1>,<Text("...>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Text("Some text", %composer, 0b0110)
                M1({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C:Test.kt")
                  Identity {
                    if (condition) {
                      %composer.endToMarker(tmp0_marker)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                      %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                        Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                      }
                      return
                    }
                  }
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("Some more text", %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable_M1_RM1() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                M1 {
                    Identity {
                        if (condition) return@M1
                    }
                }
                Text("Some more text")
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Text("...>,<M1>,<Text("...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Text("Some text", %composer, 0b0110)
                M1({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C:Test.kt")
                  Identity {
                    if (condition) {
                      %composer.endReplaceableGroup()
                      return@M1
                    }
                  }
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("Some more text", %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun verifyEarlyExitFromNestedInlineFunction() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean) {
                Text("Before outer")
                InlineLinearA {
                    Text("Before inner")
                    InlineLinearB inner@{
                        Text("Before return")
                        if (condition) return@inner
                        Text("After return")
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }
        """,
        expectedTransformed = """
            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<Text("...>,<Inline...>,<Text("...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              Text("Before outer", %composer, 0b0110)
              InlineLinearA({ %composer: Composer?, %changed: Int ->
                sourceInformationMarkerStart(%composer, <>, "C<Text("...>,<Inline...>,<Text("...>:Test.kt")
                Text("Before inner", %composer, 0b0110)
                InlineLinearB({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<Text("...>,<Text("...>:Test.kt")
                  Text("Before return", %composer, 0b0110)
                  if (condition) {
                    %composer.endReplaceableGroup()
                    return@InlineLinearB
                  }
                  Text("After return", %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("After inner", %composer, 0b0110)
                sourceInformationMarkerEnd(%composer)
              }, %composer, 0)
              Text("Before outer", %composer, 0b0110)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }

            @Composable
            inline fun InlineLinearA(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun InlineLinearB(content: @Composable () -> Unit) {
                content()
            }
        """
    )

    @Test
    fun verifyEarlyExitFromMultiLevelNestedInlineFunction() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean) {
                Text("Before outer")
                InlineLinearA outer@{
                    Text("Before inner")
                    InlineLinearB {
                        Text("Before return")
                        if (condition) return@outer
                        Text("After return")
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }
        """,
        expectedTransformed = """
            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<Text("...>,<Inline...>,<Text("...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              Text("Before outer", %composer, 0b0110)
              InlineLinearA({ %composer: Composer?, %changed: Int ->
                val tmp0_marker = %composer.currentMarker
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C<Text("...>,<Inline...>,<Text("...>:Test.kt")
                Text("Before inner", %composer, 0b0110)
                InlineLinearB({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<Text("...>,<Text("...>:Test.kt")
                  Text("Before return", %composer, 0b0110)
                  if (condition) {
                    %composer.endToMarker(tmp0_marker)
                    return@InlineLinearA
                  }
                  Text("After return", %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("After inner", %composer, 0b0110)
                %composer.endReplaceableGroup()
              }, %composer, 0)
              Text("Before outer", %composer, 0b0110)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }

            @Composable
            inline fun InlineLinearA(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun InlineLinearB(content: @Composable () -> Unit) {
                content()
            }
        """
    )

    @Test
    fun testEnsureRuntimeTestWillCompile_CL() {
        classLoader(
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun test_CM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - before")
                    if (condition) return
                    Text("M1 - after")
                }
                Text("Root - after")
            }
            @Composable
            inline fun InlineWrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

            @Composable
            fun Text(value: String) { }
            """,
            "Test.kt"
        )
    }

    @Test // regression 255350755
    fun testEnsureRuntimeTestWillCompile_CG() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun test_CM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - before")
                    if (condition) return
                    Text("M1 - after")
                }
                Text("Root - after")
            }

        """,
        """
            @Composable
            fun test_CM1_RetFun(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test_CM1_RetFun)<Text("...>,<M1>,<Text("...>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Text("Root - before", %composer, 0b0110)
                M1({ %composer: Composer?, %changed: Int ->
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<Text("...>,<Text("...>:Test.kt")
                  Text("M1 - before", %composer, 0b0110)
                  if (condition) {
                    %composer.endToMarker(tmp0_marker)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer@test_CM1_RetFun.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      test_CM1_RetFun(condition, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                  Text("M1 - after", %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                Text("Root - after", %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test_CM1_RetFun(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            inline fun InlineWrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

            @Composable @NonRestartableComposable
            fun Text(value: String) { }
        """
    )

    @Test
    fun testIfElseWithCallsInConditions(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Since the condition in the else-if is conditionally executed, it means we have
                // dynamic execution and we can't statically guarantee the number of groups. As a
                // result, we generate a group around the if statement in addition to a group around
                // each of the conditions with composable calls in them. Note that no group is
                // needed around the else condition
                if (B(a)) {
                    NA()
                } else if (B(b)) {
                    NA()
                } else {
                    NA()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (%composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "<B(a)>")
              val tmp0_group = B(a, %composer, 0)
              %composer.endReplaceableGroup()
              tmp0_group) {
                NA()
              } else if (%composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "<B(b)>")
              val tmp1_group = B(b, %composer, 0)
              %composer.endReplaceableGroup()
              tmp1_group) {
                NA()
              } else {
                NA()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndNoCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // nothing needed except for the function boundary group
                when (x) {
                    0 -> 8
                    1 -> 10
                    else -> x
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_subject = x
              when {
                tmp0_subject == 0 -> {
                  8
                }
                tmp0_subject == 0b0001 -> {
                  10
                }
                else -> {
                  x
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // calls only in the result block, which means we can statically guarantee the
                // number of groups, so no group around the when is needed, just groups around the
                // result blocks.
                when (x) {
                    0 -> A(a)
                    1 -> A(b)
                    else -> A(c)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_subject = x
              when {
                tmp0_subject == 0 -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(a)>")
                  A(a, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                tmp0_subject == 0b0001 -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(b)>")
                  A(b, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(c)>")
                  A(c, %composer, 0)
                  %composer.endReplaceableGroup()
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCallsWithResult(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // no need for a group around the when expression overall, but since the result
                // of the expression is now being used, we need to generate temporary variables to
                // capture the result but still do the execution of the expression inside of groups.
                val y = when (x) {
                    0 -> R(a)
                    1 -> R(b)
                    else -> R(c)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val y = <block>{
                val tmp0_subject = x
                when {
                  tmp0_subject == 0 -> {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<R(a)>")
                    val tmp0_group = R(a, %composer, 0)
                    %composer.endReplaceableGroup()
                    tmp0_group
                  }
                  tmp0_subject == 0b0001 -> {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<R(b)>")
                    val tmp1_group = R(b, %composer, 0)
                    %composer.endReplaceableGroup()
                    tmp1_group
                  }
                  else -> {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<R(c)>")
                    val tmp2_group = R(c, %composer, 0)
                    %composer.endReplaceableGroup()
                    tmp2_group
                  }
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A(a)
                    x > 30 -> A(b)
                    else -> A(c)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              when {
                x < 0 -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(a)>")
                  A(a, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                x > 30 -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(b)>")
                  A(b, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(c)>")
                  A(c, %composer, 0)
                  %composer.endReplaceableGroup()
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCallsInSomeResults(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A(a)
                    x > 30 -> NA()
                    else -> A(b)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              when {
                x < 0 -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(a)>")
                  A(a, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                x > 30 -> {
                  %composer.startReplaceableGroup(<>)
                  %composer.endReplaceableGroup()
                  NA()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A(b)>")
                  A(b, %composer, 0)
                  %composer.endReplaceableGroup()
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCallsInConditions(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group. Since there
                // are no other composable calls, the function body group will suffice.
                when {
                    x == R(a) -> NA()
                    x > R(b) -> NA()
                    else -> NA()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              when {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<R(a)>")
                val tmp0_group = x == R(a, %composer, 0)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<R(b)>")
                val tmp1_group = x > R(b, %composer, 0)
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                else -> {
                  NA()
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCallsInConditionsAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group.
                when {
                    x == R(a) -> NA()
                    x > R(b) -> NA()
                    else -> NA()
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "")
              when {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<R(a)>")
                val tmp0_group = x == R(a, %composer, 0)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<R(b)>")
                val tmp1_group = x > R(b, %composer, 0)
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                else -> {
                  NA()
                }
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testSafeCall(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int?) {
                // The composable call is made conditionally, which means it is like an if with
                // only one result having a composable call, so we just generate a single group
                // around the whole expression.
                x?.A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int?, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              x?.A(%composer, 0b1110 and %changed)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testElvis(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int?) {
                // The composable call is made conditionally, which means it is like an if, but with
                // only one result having a composable call, so we just generate a single group
                // around the whole expression.
                val y = x ?: R()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int?, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<R()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val y = <block>{
                val <elvis> = x
                val tmp0_group = when {
                  <elvis> == null -> {
                    R(%composer, 0)
                  }
                  else -> {
                    <elvis>
                  }
                }
                tmp0_group
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group. Since there are no other calls, the
                // function body group will suffice.
                for (i in items) {
                    P(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: List<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val <iterator> = items.iterator()
              while (<iterator>.hasNext()) {
                val i = <iterator>.next()
                P(i, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group.
                for (i in items) {
                    P(i)
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: List<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<P(i)>")
              val <iterator> = items.iterator()
              while (<iterator>.hasNext()) {
                val i = <iterator>.next()
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInSubject(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // The for loop's subject expression is only executed once, so we don't need any
                // additional groups
                for (i in L()) {
                    print(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<L()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val <iterator> = L(%composer, 0).iterator()
              while (<iterator>.hasNext()) {
                val i = <iterator>.next()
                print(i)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement. Since there are no calls after the while loop, the function
                // body group will suffice.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(item...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<P(item...>")
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInCondition(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall. The function
                // body group will suffice.
                while (B()) {
                    print("hello world")
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<B()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (B(%composer, 0)) {
                print("hello world")
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall.
                while (B()) {
                    print("hello world")
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<B()>")
              while (B(%composer, 0)) {
                print("hello world")
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall, but the function body group will suffice.
                while (B()) {
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<B()>,<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (B(%composer, 0)) {
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall.
                while (B()) {
                    A(a)
                }
                A(b)
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A(b)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<B()>,<A(a)>")
              while (B(%composer, 0)) {
                A(a, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(b, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsBeforeButNotAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // in the early return path, we need only close out the opened groups
                if (x > 0) {
                    A()
                    return
                }
                print("hello")
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return
              }
              print("hello")
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsAfterButNotBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // we can just close out the open groups at the return.
                if (x > 0) {
                    return
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return
              }
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    A()
                    return 1
                }
                return 2
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                A(%composer, 0)
                val tmp1_return = 1
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return tmp1_return
              }
              val tmp0 = 2
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testEarlyReturnValueWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return 1
                }
                A()
                return 2
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                val tmp1_return = 1
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return tmp1_return
              }
              A(%composer, 0)
              val tmp0 = 2
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnCallValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(): Int {
                // since the return expression is a composable call, we need to generate a
                // temporary variable and then return it after ending the open groups.
                A()
                return R()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>,<R()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              A(%composer, 0)
              val tmp0 = R(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testEarlyReturnCallValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return R()
                }
                return R()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<R()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "<R()>")
              if (x > 0) {
                val tmp1_return = R(%composer, 0)
                %composer.endReplaceableGroup()
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return tmp1_return
              }
              %composer.endReplaceableGroup()
              val tmp0 = R(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnFromLoop(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    val k = i
                    val l = i
                    P(i)
                    if (i == 0) {
                        P(j)
                        return
                    } else {
                        P(k)
                    }
                    P(l)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>,<P(l)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                val i = items.next()
                val j = i
                val k = i
                val l = i
                P(i, %composer, 0)
                if (i == 0) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<P(j)>")
                  P(j, %composer, 0)
                  %composer.endReplaceableGroup()
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                  return
                } else {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<P(k)>")
                  P(k, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                P(l, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testOrderingOfPushedEndCallsWithEarlyReturns(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    val k = i
                    val l = i
                    P(i)
                    if (i == 0) {
                        P(j)
                        return
                    } else {
                        P(k)
                    }
                    P(l)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>,<P(l)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                val i = items.next()
                val j = i
                val k = i
                val l = i
                P(i, %composer, 0)
                if (i == 0) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<P(j)>")
                  P(j, %composer, 0)
                  %composer.endReplaceableGroup()
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Example(items, %composer, updateChangedFlags(%changed or 0b0001))
                  }
                  return
                } else {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<P(k)>")
                  P(k, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                P(l, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(items, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testBreakWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                val i = items.next()
                if (i == 0) {
                  break
                }
                P(i, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (i == 0) {
                  break
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here, but the function body group will suffice
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(j)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<P(i)>,<P(j)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                val i = items.next()
                val j = i
                P(i, %composer, 0)
                if (i == 0) {
                  break
                }
                P(j, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfterAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<P(i)>,<P(i)>")
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (i == 0) {
                  break
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<P(i)>")
                val i = items.next()
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                P(i, %composer, 0)
                %composer.endReplaceableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    print(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<P(i)>")
                val i = items.next()
                P(i, %composer, 0)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                print(i)
                %composer.endReplaceableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (items.hasNext()) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<P(i)>,<P(i)>")
                val i = items.next()
                P(i, %composer, 0)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                P(i, %composer, 0)
                %composer.endReplaceableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLoopWithReturn(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                while (a.hasNext()) {
                    val x = a.next()
                    if (x > 100) {
                        return
                    }
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (a.hasNext()) {
                val x = a.next()
                if (x > 100) {
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                  return
                }
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLoopWithBreak(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == x) {
                            break@a
                        }
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              a@while (a.hasNext()) {
                val x = a.next()
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<A()>")
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (y == x) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNestedLoopsAndBreak(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    if (x == 0) {
                        break
                    }
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == 0) {
                            break
                        }
                        if (y == x) {
                            break@a
                        }
                        if (y > 100) {
                            return
                        }
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              a@while (a.hasNext()) {
                val x = a.next()
                if (x == 0) {
                  break${if (useFir) "@a" else ""}
                }
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<A()>")
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (y == 0) {
                    break${if (useFir) "@b" else ""}
                  }
                  if (y == x) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  if (y > 100) {
                    %composer.endReplaceableGroup()
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer.endReplaceableGroup()
                    return
                  }
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNestedLoops(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    b@while (b.hasNext()) {
                        A()
                    }
                    A()
                }
                A()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<A()>")
              a@while (a.hasNext()) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<A()>")
                b@while (b.hasNext()) {
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileInsideIfAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<A()>")
                while (x > 0) {
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileInsideIfAndCallBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                    while (x > 0) {
                        A()
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>,*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                A(%composer, 0)
                while (x > 0) {
                  A(%composer, 0)
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileInsideIf(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                while (x > 0) {
                  A(%composer, 0)
                }
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKey(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A()>")
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithTwoKeys(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A(a)
                    }
                    key(x+1) {
                        A(b)
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(a)>")
                A(a, %composer, 0)
                %composer.endMovableGroup()
                %composer.startMovableGroup(<>, x + 1)
                sourceInformation(%composer, "<A(b)>")
                A(b, %composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A(a)
                    }
                    A(b)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A(b)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(a)>")
                A(a, %composer, 0)
                %composer.endMovableGroup()
                A(b, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A(a)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                A(a, %composer, 0)
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(b)>")
                A(b, %composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                    A(c)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<A(a)>,<A(c)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                A(a, %composer, 0)
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(b)>")
                A(b, %composer, 0)
                %composer.endMovableGroup()
                A(c, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevel(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                key(x) {
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startMovableGroup(<>, x)
              sourceInformation(%composer, "<A()>")
              A(%composer, 0)
              %composer.endMovableGroup()
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                key(x) {
                    A(a)
                }
                A(b)
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A(b)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startMovableGroup(<>, x)
              sourceInformation(%composer, "<A(a)>")
              A(a, %composer, 0)
              %composer.endMovableGroup()
              A(b, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                A(a)
                key(x) {
                    A(b)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A(a)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              A(a, %composer, 0)
              %composer.startMovableGroup(<>, x)
              sourceInformation(%composer, "<A(b)>")
              A(b, %composer, 0)
              %composer.endMovableGroup()
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyInIf(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A()>")
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyInIfAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A(a)
                    }
                    A(b)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A(b)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(a)>")
                A(a, %composer, 0)
                %composer.endMovableGroup()
                A(b, %composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyInIfAndCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<A(a)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (x > 0) {
                A(a, %composer, 0)
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A(b)>")
                A(b, %composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyWithLotsOfValues(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Int, b: Int, c: Int, d: Int) {
                key(a, b, c, d) {
                    A()
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(a: Int, b: Int, c: Int, d: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startMovableGroup(<>, %composer.joinKey(%composer.joinKey(%composer.joinKey(a, b), c), d))
              sourceInformation(%composer, "<A()>")
              A(%composer, 0)
              %composer.endMovableGroup()
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyWithComposableValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while(x > 0) {
                    key(R()) {
                        A()
                    }
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)*<R()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              while (x > 0) {
                %composer.startMovableGroup(<>, R(%composer, 0))
                sourceInformation(%composer, "<A()>")
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAsAValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                val y = key(x) { R() }
                P(y)
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<P(y)>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val y = <block>{
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<R()>")
                val tmp0 = R(%composer, 0)
                %composer.endMovableGroup()
                tmp0
              }
              P(y, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testDynamicWrappingGroupWithReturnValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                return if (x > 0) {
                    if (B()) 1
                    else if (B()) 2
                    else 3
                } else 4
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = <block>{
                val tmp4_group = if (x > 0) {
                  val tmp3_group = if (%composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<B()>")
                  val tmp1_group = B(%composer, 0)
                  %composer.endReplaceableGroup()
                  tmp1_group) 1 else if (%composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<B()>")
                  val tmp2_group = B(%composer, 0)
                  %composer.endReplaceableGroup()
                  tmp2_group) 2 else 3
                  tmp3_group
                } else {
                  4
                }
                tmp4_group
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testTheThing(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Simple() {
              // this has a composable call in it, and since we don't know the number of times the
              // lambda will get called, we place a group around the whole call
              run {
                A()
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun WithReturn() {
              // this has an early return in it, so it needs to end all of the groups present.
              run {
                A()
                return@WithReturn
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun NoCalls() {
              // this has no composable calls in it, so shouldn't cause any groups to get created
              run {
                println("hello world")
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun NoCallsAfter() {
              // this has a composable call in the lambda, but not after it, which means the
              // group should be able to be coalesced into the group of the function
              run {
                A()
              }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Simple(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Simple)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<A()>")
              run {
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @NonRestartableComposable
            @Composable
            fun WithReturn(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(WithReturn)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<A()>")
              run {
                A(%composer, 0)
                %composer.endReplaceableGroup()
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                %composer.endReplaceableGroup()
                return
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @NonRestartableComposable
            @Composable
            fun NoCalls(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(NoCalls)<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              run {
                println("hello world")
              }
              A(%composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @NonRestartableComposable
            @Composable
            fun NoCallsAfter(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(NoCallsAfter)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              run {
                A(%composer, 0)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLetWithComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  A(a)
                }
                A(b)
              }
              A(c)
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<A(c)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val tmp0_safe_receiver = x
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<A(b)>")
                val tmp0_group = when {
                  tmp0_safe_receiver == null -> {
                    null
                  }
                  else -> {
                    tmp0_safe_receiver.let { it: Int ->
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "<A(a)>")
                      if (it > 0) {
                        A(a, %composer, 0)
                      }
                      %composer.endReplaceableGroup()
                      A(b, %composer, 0)
                    }
                  }
                }
                %composer.endReplaceableGroup()
                tmp0_group
                A(c, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testLetWithoutComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  NA()
                }
                NA()
              }
              A()
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                x?.let { it: Int ->
                  if (it > 0) {
                    NA()
                  }
                  NA()
                }
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testApplyOnComposableCallResult(): Unit = controlFlow(
        """
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.State

            @Composable
            fun <T> provided(value: T): State<T> = remember { mutableStateOf(value) }.apply {
                this.value = value
            }
        """,
        """
            @Composable
            fun <T> provided(value: T, %composer: Composer?, %changed: Int): State<T> {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(provided)*<rememb...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = remember({
                mutableStateOf(
                  value = value
                )
              }, %composer, 0).apply {
                %this%apply.value = value
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnInlinedExpressionWithCall(): Unit = controlFlow(
        """
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.State

            @Composable
            fun Test(x: Int): Int {
                return x.let {
                    A()
                    123
                }
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)*<A()>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = <block>{
                val tmp1_group = x.let { it: Int ->
                  A(%composer, 0)
                  val tmp0_return = 123
                  tmp0_return
                }
                tmp1_group
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testCallingAWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              W {
                A()
              }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<W>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                W(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<A()>:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  A(%composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testCallingAnInlineWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              IW {
                A()
              }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<IW>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                IW({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<A()>:Test.kt")
                  A(%composer, 0)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testRepeatedCallsToEffects(): Unit = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
                Wrap {
                    repeat(number) {
                        effects[it] = effect { 0 }
                    }
                    outside = effect { "0" }
                }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Wrap>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Wrap(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<effect>:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "*<effect>")
                  repeat(number) { it: Int ->
                    effects[it] = effect({
                      0
                    }, %composer, 0b0110)
                  }
                  %composer.endReplaceableGroup()
                  outside = effect({
                    "0"
                  }, %composer, 0b0110)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            var effects = mutableListOf<Any>()
            var outside: Any = ""
            var number = 1

            @Composable fun Wrap(block: @Composable () -> Unit) =  block()
            @Composable fun <T> effect(block: () -> T): T = block()
        """

    )

    @Test
    fun testComposableWithInlineClass(): Unit = controlFlow(
        """
            @Composable
            fun Test(value: InlineClass) {
                used(value)
                A()
            }
        """,
        """
            @Composable
            fun Test(value: InlineClass, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(0:InlineClass)<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(value))) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(value)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(value, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testParameterOrderInformation(): Unit = controlFlow(
        """
            @Composable fun Test01(p0: Int, p1: Int, p2: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test02(p0: Int, p1: Int, p3: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test03(p0: Int, p2: Int, p1: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test04(p0: Int, p2: Int, p3: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test05(p0: Int, p3: Int, p1: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test06(p0: Int, p3: Int, p2: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test07(p1: Int, p0: Int, p2: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test08(p1: Int, p0: Int, p3: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test09(p1: Int, p2: Int, p0: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test00(p1: Int, p2: Int, p3: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test11(p1: Int, p3: Int, p0: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test12(p1: Int, p3: Int, p2: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test13(p2: Int, p0: Int, p1: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test14(p2: Int, p0: Int, p3: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test15(p2: Int, p1: Int, p0: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test16(p2: Int, p1: Int, p3: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test17(p2: Int, p3: Int, p0: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test18(p2: Int, p3: Int, p1: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test19(p3: Int, p0: Int, p1: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test20(p3: Int, p0: Int, p2: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test21(p3: Int, p1: Int, p0: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test22(p3: Int, p1: Int, p2: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test23(p3: Int, p2: Int, p0: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test24(p3: Int, p2: Int, p1: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
        """,
        """
            @Composable
            fun Test01(p0: Int, p1: Int, p2: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test01):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test01(p0, p1, p2, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test02(p0: Int, p1: Int, p3: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test02)P(!2,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test02(p0, p1, p3, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test03(p0: Int, p2: Int, p1: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test03)P(!1,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test03(p0, p2, p1, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test04(p0: Int, p2: Int, p3: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test04)P(!1,2,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test04(p0, p2, p3, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test05(p0: Int, p3: Int, p1: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test05)P(!1,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test05(p0, p3, p1, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test06(p0: Int, p3: Int, p2: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test06)P(!1,3,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test06(p0, p3, p2, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test07(p1: Int, p0: Int, p2: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test07)P(1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test07(p1, p0, p2, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test08(p1: Int, p0: Int, p3: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test08)P(1!1,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test08(p1, p0, p3, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test09(p1: Int, p2: Int, p0: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test09)P(1,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test09(p1, p2, p0, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test00(p1: Int, p2: Int, p3: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test00)P(1,2,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test00(p1, p2, p3, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test11(p1: Int, p3: Int, p0: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test11)P(1,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test11(p1, p3, p0, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test12(p1: Int, p3: Int, p2: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test12)P(1,3,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test12(p1, p3, p2, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test13(p2: Int, p0: Int, p1: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test13)P(2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test13(p2, p0, p1, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test14(p2: Int, p0: Int, p3: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test14)P(2!1,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test14(p2, p0, p3, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test15(p2: Int, p1: Int, p0: Int, p3: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test15)P(2,1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test15(p2, p1, p0, p3, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test16(p2: Int, p1: Int, p3: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test16)P(2,1,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test16(p2, p1, p3, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test17(p2: Int, p3: Int, p0: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test17)P(2,3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test17(p2, p3, p0, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test18(p2: Int, p3: Int, p1: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test18)P(2,3,1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test18(p2, p3, p1, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test19(p3: Int, p0: Int, p1: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test19)P(3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test19(p3, p0, p1, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test20(p3: Int, p0: Int, p2: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test20)P(3!1,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test20(p3, p0, p2, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test21(p3: Int, p1: Int, p0: Int, p2: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test21)P(3,1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test21(p3, p1, p0, p2, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test22(p3: Int, p1: Int, p2: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test22)P(3,1,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test22(p3, p1, p2, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test23(p3: Int, p2: Int, p0: Int, p1: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test23)P(3,2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test23(p3, p2, p0, p1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test24(p3: Int, p2: Int, p1: Int, p0: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test24)P(3,2,1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(p3)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(p2)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p1)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(p0)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(p0)
                used(p1)
                used(p2)
                used(p3)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test24(p3, p2, p1, p0, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testSourceInformationWithPackageName(): Unit = verifyComposeIrTransform(
        source = """
            package androidx.compose.runtime.tests

            import androidx.compose.runtime.Composable

            @Composable
            fun Test(value: LocalInlineClass) {
                used(value)
            }
        """,
        extra = """
            package androidx.compose.runtime.tests

            inline class LocalInlineClass(val value: Int)
            fun used(x: Any?) {}
        """,
        expectedTransformed = """
            @Composable
            fun Test(value: LocalInlineClass, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(0:c#runtime.tests.LocalInlineClass):Test.kt#992ot2")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(value))) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, "androidx.compose.runtime.tests.Test (Test.kt:6)")
                }
                used(value)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(value, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceOffsetOrderForParameterExpressions(): Unit = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
                A(b(), c(), d())
                B()
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable fun A(a: Int, b: Int, c: Int) { }
            @Composable fun B() { }
            @Composable fun b(): Int = 1
            @Composable fun c(): Int = 1
            @Composable fun d(): Int = 1
        """,
        expectedTransformed = """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<b()>,<c()>,<d()>,<A(b(),>,<B()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, "Test (Test.kt:4)")
                }
                A(b(%composer, 0), c(%composer, 0), d(%composer, 0), %composer, 0)
                B(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceLocationOfCapturingComposableLambdas(): Unit = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            class SomeClass {
                var a = "Test"
                fun onCreate() {
                    setContent {
                        B(a)
                        B(a)
                    }
                }
            }

            fun Test() {
                var a = "Test"
                setContent {
                    B(a)
                    B(a)
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            fun setContent(block: @Composable () -> Unit) { }
            @Composable fun B(value: String) { }
        """,
        expectedTransformed = """
            @StabilityInferred(parameters = 0)
            class SomeClass {
              var a: String = "Test"
              fun onCreate() {
                setContent(composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<B(a)>,<B(a)>:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, "SomeClass.onCreate.<anonymous> (Test.kt:7)")
                    }
                    B(a, %composer, 0)
                    B(a, %composer, 0)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                )
              }
              static val %stable: Int = 8
            }
            fun Test() {
              var a = "Test"
              setContent(composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<B(a)>,<B(a)>:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, "Test.<anonymous> (Test.kt:16)")
                  }
                  B(a, %composer, 0)
                  B(a, %composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              )
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceLineInformationForNormalInline(): Unit = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
              W {
                IW {
                    T(2)
                    repeat(3) {
                        T(3)
                    }
                    T(4)
                }
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable fun W(block: @Composable () -> Unit) = block()
            @Composable inline fun IW(block: @Composable () -> Unit) = block()
            @Composable fun T(value: Int) { }
        """,
        expectedTransformed = """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<W>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, "Test (Test.kt:4)")
                }
                W(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<IW>:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, "ComposableSingletons%TestKt.lambda-1.<anonymous> (Test.kt:6)")
                  }
                  IW({ %composer: Composer?, %changed: Int ->
                    sourceInformationMarkerStart(%composer, <>, "C<T(2)>,<T(4)>:Test.kt")
                    T(2, %composer, 0b0110)
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "*<T(3)>")
                    repeat(3) { it: Int ->
                      T(3, %composer, 0b0110)
                    }
                    %composer.endReplaceableGroup()
                    T(4, %composer, 0b0110)
                    sourceInformationMarkerEnd(%composer)
                  }, %composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testInlineReadOnlySourceLocations() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ReadOnlyComposable

            val current
                @Composable
                @ReadOnlyComposable
                get() = 0

            @Composable
            @ReadOnlyComposable
            fun calculateSometing(): Int {
                return 0;
            }

            @Composable
            fun Test() {
                val c = current
                val cl = calculateSometing()
                Layout {
                    Text("${'$'}c ${'$'}cl")
                }
            }
        """,
        """
            val current: Int
              @Composable @ReadOnlyComposable @JvmName(name = "getCurrent")
              get() {
                sourceInformationMarkerStart(%composer, <>, "C:Test.kt")
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, "<get-current> (Test.kt:7)")
                }
                val tmp0 = 0
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                sourceInformationMarkerEnd(%composer)
                return tmp0
              }
            @Composable
            @ReadOnlyComposable
            fun calculateSometing(%composer: Composer?, %changed: Int): Int {
              sourceInformationMarkerStart(%composer, <>, "C(calculateSometing):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, "calculateSometing (Test.kt:11)")
              }
              val tmp0 = 0
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              sourceInformationMarkerEnd(%composer)
              return tmp0
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<curren...>,<calcul...>,<Layout>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, "Test (Test.kt:16)")
                }
                val c = <get-current>(%composer, 0)
                val cl = calculateSometing(%composer, 0)
                Layout({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<Text("...>:Test.kt")
                  Text("%c %cl", %composer, 0)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Layout(content: @Composable () -> Unit) { content() }

            @Composable
            fun Text(text: String) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testReadOnlyInlineValSourceLocations() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ReadOnlyComposable

            class CurrentHolder {
                inline val current: Int
                    @ReadOnlyComposable
                    @Composable
                    get() = 0
            }

            class HolderHolder {
                private val _currentHolder = CurrentHolder()
                val current: Int
                    @ReadOnlyComposable
                    @Composable
                    get() = _currentHolder.current
            }

            val holderHolder = HolderHolder()

            @Composable
            @ReadOnlyComposable
            fun calculateSomething(): Int {
                return 0;
            }

            @Composable
            fun Test() {
                val c = holderHolder.current
                val cl = calculateSomething()
                Layout {
                    Text("${'$'}c ${'$'}cl")
                }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class CurrentHolder {
              val current: Int
                @ReadOnlyComposable @Composable @JvmName(name = "getCurrent")
                get() {
                  sourceInformationMarkerStart(%composer, <>, "CC:Test.kt")
                  val tmp0 = 0
                  sourceInformationMarkerEnd(%composer)
                  return tmp0
                }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class HolderHolder {
              val _currentHolder: CurrentHolder = CurrentHolder()
              val current: Int
                @ReadOnlyComposable @Composable @JvmName(name = "getCurrent")
                get() {
                  sourceInformationMarkerStart(%composer, <>, "C<curren...>:Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, "HolderHolder.<get-current> (Test.kt:16)")
                  }
                  val tmp0 = _currentHolder.<get-current>(%composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  sourceInformationMarkerEnd(%composer)
                  return tmp0
                }
              static val %stable: Int = 0
            }
            val holderHolder: HolderHolder = HolderHolder()
            @Composable
            @ReadOnlyComposable
            fun calculateSomething(%composer: Composer?, %changed: Int): Int {
              sourceInformationMarkerStart(%composer, <>, "C(calculateSomething):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, "calculateSomething (Test.kt:23)")
              }
              val tmp0 = 0
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              sourceInformationMarkerEnd(%composer)
              return tmp0
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<curren...>,<calcul...>,<Layout>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, "Test (Test.kt:28)")
                }
                val c = holderHolder.<get-current>(%composer, 0b0110)
                val cl = calculateSomething(%composer, 0)
                Layout({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<Text("...>:Test.kt")
                  Text("%c %cl", %composer, 0)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Layout(content: @Composable () -> Unit) { content() }

            @Composable
            fun Text(text: String) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testReadOnlyComposableWithEarlyReturn() = controlFlow(
        source = """
            @ReadOnlyComposable
            @Composable
            fun getSomeValue(a: Int): Int {
                if (a < 100) return 0
                return 1
            }
        """,
        """
            @ReadOnlyComposable
            @Composable
            fun getSomeValue(a: Int, %composer: Composer?, %changed: Int): Int {
              sourceInformationMarkerStart(%composer, <>, "C(getSomeValue):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              if (a < 100) {
                val tmp1_return = 0
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
                sourceInformationMarkerEnd(%composer)
                return tmp1_return
              }
              val tmp0 = 1
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              sourceInformationMarkerEnd(%composer)
              return tmp0
            }
        """
    )

    @Test
    fun testMultipleNestedInlines() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun AttemptedToRealizeGroupTwice() {
                Wrapper {
                    repeat(1) {
                        repeat(1) {
                            Leaf(0)
                        }
                        Leaf(0)
                    }
                }
            }
        """,
        expectedTransformed = """
            @Composable
            fun AttemptedToRealizeGroupTwice(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(AttemptedToRealizeGroupTwice)<Wrappe...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Wrapper({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C:Test.kt")
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "*<Leaf(0...>")
                  repeat(1) { it: Int ->
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "*<Leaf(0...>")
                    repeat(1) { it: Int ->
                      Leaf(0, %composer, 0b0110, 0)
                    }
                    %composer.endReplaceableGroup()
                    Leaf(0, %composer, 0b0110, 0)
                  }
                  %composer.endReplaceableGroup()
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                AttemptedToRealizeGroupTwice(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Wrapper(content: @Composable () -> Unit) { }

            @Composable
            fun Leaf(default: Int = 0) {}
        """
    )

    @Test // Regression test for 205590513
    fun testGroupAroundExtensionFunctions() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(start: Int, end: Int) {
                val a = remember { A() }
                for (i in start until end) {
                    val b = a.get(bKey)
                    if (i == 2) {
                        a.get(cKey)
                    }
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            class A

            class B

            class C

            val bKey: () -> B = { B() }
            val cKey: () -> C = { C() }

            @Composable
            fun <T> A.get(block: () -> T) = block()
        """,
        expectedTransformed = """
            @Composable
            fun Test(start: Int, end: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(1)<rememb...>,*<get(bK...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(start)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(end)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val a = remember({
                  A()
                }, %composer, 0)
                val <iterator> = start until end.iterator()
                while (<iterator>.hasNext()) {
                  val i = <iterator>.next()
                  val b = a.get(bKey, %composer, 0b00110110)
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<get(cK...>")
                  if (i == 0b0010) {
                    a.get(cKey, %composer, 0b00110110)
                  }
                  %composer.endReplaceableGroup()
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(start, end, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    // There are a number of "inline constructors" in the Kotlin standard library for Array types.
    // These are special cases, since normal constructors cannot be inlined.
    @Test
    fun testInlineArrayConstructor() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            fun ArrayConstructorTest(n: Int) {
                Array(n) { remember { it } }
                ByteArray(n) { remember { it.toByte() } }
                CharArray(n) { remember { it.toChar() } }
                ShortArray(n) { remember { it.toShort() } }
                IntArray(n) { remember { it } }
                LongArray(n) { remember { it.toLong() } }
                FloatArray(n) { remember { it.toFloat() } }
                DoubleArray(n) { remember { it.toDouble() } }
                BooleanArray(n) { remember { false } }
            }
        """,
        """
            @Composable
            fun ArrayConstructorTest(n: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(ArrayConstructorTest)<rememb...>,<rememb...>,<rememb...>,<rememb...>,<rememb...>,<rememb...>,<rememb...>,<rememb...>,<rememb...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(n)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Array(n) { it: Int ->
                  val tmp0_return = remember({
                    it
                  }, %composer, 0)
                  tmp0_return
                }
                ByteArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toByte()
                  }, %composer, 0)
                  tmp0_return
                }
                CharArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toChar()
                  }, %composer, 0)
                  tmp0_return
                }
                ShortArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toShort()
                  }, %composer, 0)
                  tmp0_return
                }
                IntArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it
                  }, %composer, 0)
                  tmp0_return
                }
                LongArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toLong()
                  }, %composer, 0)
                  tmp0_return
                }
                FloatArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toFloat()
                  }, %composer, 0)
                  tmp0_return
                }
                DoubleArray(n) { it: Int ->
                  val tmp0_return = remember({
                    it.toDouble()
                  }, %composer, 0)
                  tmp0_return
                }
                BooleanArray(n) { it: Int ->
                  val tmp0_return = remember({
                    false
                  }, %composer, 0)
                  tmp0_return
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                ArrayConstructorTest(n, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testComposeIrSkippingWithDefaultsRelease() = verifyComposeIrTransform(
        """
            import androidx.compose.ui.text.input.TextFieldValue
            import androidx.compose.runtime.*
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.text.KeyboardActions
            import androidx.compose.material.*

            object Ui {}

            @Composable
            fun Ui.UiTextField(
                isError: Boolean = false,
                keyboardActions2: Boolean = false,
            ) {
                println("t41 insideFunction ${'$'}isError")
                println("t41 insideFunction ${'$'}keyboardActions2")
                Column {
                    Text("${'$'}isError")
                    Text("${'$'}keyboardActions2")
                }
            }
        """.trimIndent(),
        """
            @StabilityInferred(parameters = 0)
            object Ui {
              static val %stable: Int = 0
            }
            @Composable
            @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
            fun Ui.UiTextField(isError: Boolean, keyboardActions2: Boolean, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(UiTextField)<Column>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(isError)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(keyboardActions2)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011010001 !== 0b10010000 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  isError = false
                }
                if (%default and 0b0010 !== 0) {
                  keyboardActions2 = false
                }
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                println("t41 insideFunction %isError")
                println("t41 insideFunction %keyboardActions2")
                Column(null, null, null, { %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<Text("...>,<Text("...>:Test.kt")
                  Text("%isError", null, <unsafe-coerce>(0L), <unsafe-coerce>(0L), null, null, null, <unsafe-coerce>(0L), null, null, <unsafe-coerce>(0L), <unsafe-coerce>(0), false, 0, 0, null, null, %composer, 0, 0, 0b00011111111111111110)
                  Text("%keyboardActions2", null, <unsafe-coerce>(0L), <unsafe-coerce>(0L), null, null, null, <unsafe-coerce>(0L), null, null, <unsafe-coerce>(0L), <unsafe-coerce>(0), false, 0, 0, null, null, %composer, 0, 0, 0b00011111111111111110)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0, 0b0111)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                UiTextField(isError, keyboardActions2, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testRememberInConditionalCallArgument() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Test(
                    if (param == null) {
                       remember { "" }
                    } else {
                        null
                    },
                )
            }
        """,
        expectedTransformed = """
            @Composable
            private fun Test(param: String?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Test(>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(param)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Test(<block>{
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<rememb...>")
                  val tmp0_group = if (param == null) {
                    remember({
                      ""
                    }, %composer, 0)
                  } else {
                    null
                  }
                  %composer.endReplaceableGroup()
                  tmp0_group
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(param, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testRememberInNestedConditionalCallArgument() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?): String? {
                return Test(
                    if (param == null) {
                       Test(
                            if (param == null) {
                                remember { "" }
                            } else {
                                null
                            }
                       )
                    } else {
                        null
                    },
                )
            }
        """,
        expectedTransformed = """
            @Composable
            private fun Test(param: String?, %composer: Composer?, %changed: Int): String? {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<Test(>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = Test(<block>{
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<Test(>")
                val tmp2_group = if (param == null) {
                  Test(<block>{
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<rememb...>")
                    val tmp1_group = if (param == null) {
                      remember({
                        ""
                      }, %composer, 0)
                    } else {
                      null
                    }
                    %composer.endReplaceableGroup()
                    tmp1_group
                  }, %composer, 0)
                } else {
                  null
                }
                %composer.endReplaceableGroup()
                tmp2_group
              }, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testInlineLambdaBeforeACall() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?): String? {
                InlineNonComposable {
                    repeat(10) {
                        Test("InsideInline")
                    }
                }
                return Test("AfterInline")
            }
        """,
        expectedTransformed = """
            @Composable
            private fun Test(param: String?, %composer: Composer?, %changed: Int): String? {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<Test("...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "*<Test("...>")
              InlineNonComposable {
                repeat(10) { it: Int ->
                  Test("InsideInline", %composer, 0b0110)
                }
              }
              %composer.endReplaceableGroup()
              val tmp0 = Test("AfterInline", %composer, 0b0110)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            inline fun InlineNonComposable(block: () -> Unit) {}
        """
    )

    @Test
    fun testInlineLambda_nonLocalReturn() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Inline1 {
                    Inline2 {
                        if (true) return@Inline1
                    }
                }
            }
        """,
        expectedTransformed = """
            @Composable
            private fun Test(param: String?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Inline...>:Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Inline1({ %composer: Composer?, %changed: Int ->
                  val tmp0_marker = %composer.currentMarker
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C<Inline...>:Test.kt")
                  Inline2({ %composer: Composer?, %changed: Int ->
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C:Test.kt")
                    if (true) {
                      %composer.endToMarker(tmp0_marker)
                      return@Inline1
                    }
                    %composer.endReplaceableGroup()
                  }, %composer, 0)
                  %composer.endReplaceableGroup()
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(param, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            inline fun Inline1(block: @Composable () -> Unit) {
                block()
            }

            @Composable
            inline fun Inline2(block: @Composable () -> Unit) {
                block()
            }
        """
    )

    fun testNothingBody() = verifyComposeIrTransform(
        source = """
        import androidx.compose.runtime.*

        val test1: @Composable () -> Unit = TODO()

        @Composable
        fun Test2(): Unit = TODO()

        @Composable
        fun Test3() {
            Wrapper {
                TODO()
            }
        }
        """,
        extra = """
        import androidx.compose.runtime.*

        @Composable
        fun Wrapper(content: @Composable () -> Unit) = content()
        """,
        expectedTransformed = """
        val test1: Function2<Composer, Int, Unit> = TODO()
        @Composable
        fun Test2(%composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test2):Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %changed, -1, <>)
            }
            TODO()
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test2(%composer, updateChangedFlags(%changed or 0b0001))
          }
        }
        @Composable
        fun Test3(%composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test3)<Wrappe...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %changed, -1, <>)
            }
            Wrapper(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test3(%composer, updateChangedFlags(%changed or 0b0001))
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              TODO()
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            } else {
              %composer.skipToGroupEnd()
            }
          }
        }
        """
    )

    @Test
    fun testEarlyReturnFromCrossInlinedLambda() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Dialog {
                    if (false) Test(param)
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            internal inline fun Dialog(crossinline block: @Composable () -> Unit) {}
        """.trimIndent(),
        expectedTransformed = """
           @Composable
           private fun Test(param: String?, %composer: Composer?, %changed: Int) {
             %composer = %composer.startRestartGroup(<>)
             sourceInformation(%composer, "C(Test)<Dialog>:Test.kt")
             val %dirty = %changed
             if (%changed and 0b1110 === 0) {
               %dirty = %dirty or if (%composer.changed(param)) 0b0100 else 0b0010
             }
             if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
               if (isTraceInProgress()) {
                 traceEventStart(<>, %dirty, -1, <>)
               }
               Dialog({ %composer: Composer?, %changed: Int ->
                 sourceInformationMarkerStart(%composer, <>, "C:Test.kt")
                 %composer.startReplaceableGroup(<>)
                 sourceInformation(%composer, "<Test(p...>")
                 if (false) {
                   Test(param, %composer, 0b1110 and %dirty)
                 }
                 %composer.endReplaceableGroup()
                 sourceInformationMarkerEnd(%composer)
               }, %composer, 0)
               if (isTraceInProgress()) {
                 traceEventEnd()
               }
             } else {
               %composer.skipToGroupEnd()
             }
             %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
               Test(param, %composer, updateChangedFlags(%changed or 0b0001))
             }
           }
        """.trimIndent(),
    )

    @Test
    fun testEarlyReturnFromWhenStatement() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                val state = remember { mutableStateOf(false) }
                when (state.value) {
                    true -> return Text(text = "true")
                    else -> Text(text = "false")
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable fun Text(text: String) {}
        """,
        expectedTransformed = """
            @Composable
            private fun Test(param: String?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>:Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val state = remember({
                  mutableStateOf(
                    value = false
                  )
                }, %composer, 0)
                val tmp0_subject = state.value
                when {
                  tmp0_subject == true -> {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<Text(t...>")
                    val tmp0_return = Text("true", %composer, 0b0110)
                    %composer.endReplaceableGroup()
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(param, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return tmp0_return
                  }
                  else -> {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "<Text(t...>")
                    Text("false", %composer, 0b0110)
                    %composer.endReplaceableGroup()
                  }
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(param, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent(),
    )
}
