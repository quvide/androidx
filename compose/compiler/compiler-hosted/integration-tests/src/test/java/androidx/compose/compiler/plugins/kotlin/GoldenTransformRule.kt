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

package androidx.compose.compiler.plugins.kotlin

import java.io.File
import java.io.FileNotFoundException
import org.jetbrains.kotlin.incremental.createDirectory
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

private const val ENV_GENERATE_GOLDEN = "GENERATE_GOLDEN"
private const val GOLDEN_FILE_TYPE = "txt"

/**
 * GoldenTransformRule
 *
 * Compare transformed IR source to a golden test file. Golden files contain both the
 * pre-transformed and post-transformed source for easier review.
 * To regenerate the set of golden tests, pass GENERATE_GOLDEN=true as an environment variable.
 **/
class GoldenTransformRule(
    private val pathToGoldens: String,
    private val generateGoldens: Boolean =
        (System.getenv(ENV_GENERATE_GOLDEN) ?: "false").toBoolean()
) : TestRule {
    private lateinit var goldenFile: File
    private lateinit var testIdentifier: String

    private val testWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            goldenFile =
                File(getGoldenFilePath(description.className, description.methodName))
            testIdentifier = "${description.className}_${description.methodName}"
        }
    }

    private fun getGoldenFilePath(
        className: String,
        methodName: String
    ) = "$pathToGoldens/$className/$methodName.$GOLDEN_FILE_TYPE"

    override fun apply(base: Statement, description: Description): Statement {
        return base.run {
            testWatcher.apply(this, description)
        }
    }

    /**
     * Verify the current test against the matching golden file.
     * If generateGoldens is true, the golden file will first be generated.
     */
    fun verifyGolden(testInfo: GoldenTransformTestInfo) {
        if (generateGoldens) {
            saveGolden(testInfo)
        }

        if (!goldenFile.exists()) {
            throw FileNotFoundException("Could not find golden file: ${goldenFile.absolutePath}")
        }

        val loadedTestInfo = try {
            GoldenTransformTestInfo.fromEncodedString(goldenFile.readText())
        } catch (e: IllegalStateException) {
            error("Golden ${goldenFile.absolutePath} file could not be parsed.\n${e.message}")
        }

        Assert.assertEquals(
            "Transformed source does not match golden file ${goldenFile.name}",
            loadedTestInfo.transformed,
            testInfo.transformed
        )
    }

    private fun saveGolden(testInfo: GoldenTransformTestInfo) {
        val directory = goldenFile.parentFile!!
        if (!directory.exists()) {
            directory.createDirectory()
        }
        goldenFile.writeText(testInfo.encodeToString())
    }
}

/**
 * GoldenTransformTestInfo
 * @param source The pre-transformed source code.
 * @param transformed Post transformed IR tree source.
 */
data class GoldenTransformTestInfo(
    val source: String,
    val transformed: String
) {
    fun encodeToString(): String {
        return "$source$DELIMITER$transformed"
    }

    companion object {
        const val DELIMITER = "\n/********\n * TRANSFORMED\n ********/\n\n"

        fun fromEncodedString(encoded: String): GoldenTransformTestInfo {
            val split = encoded.split(DELIMITER)
            if (split.size != 2) {
                error("Could not parse encoded golden string. " +
                    "Expected 2 sections but was ${split.size}.")
            }
            return GoldenTransformTestInfo(split[0], split[1])
        }
    }
}
