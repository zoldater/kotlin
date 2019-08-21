/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.junit.Assert
import org.junit.Test
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.scripting.repl.js.makeReplCodeLine

class ReplTest : TestCase() {

    @Test
    fun testIndependentLines() {
        JsReplBase().use { base ->
            val lines = listOf(
                "var x = 38",
                "var y = 99",
                "4 + 1"
            )
            Assert.assertEquals(5, compileAndEval(base, lines))
        }
    }

    @Test
    fun testDependentLines() {
        JsReplBase().use { base ->
            val lines = listOf(
                "var x = 7",
                "var y = 32",
                "x + y"
            )
            Assert.assertEquals(39, compileAndEval(base, lines))
        }
    }

    @Test
    fun testFunctionCall() {
        JsReplBase().use { base ->
            val lines = listOf(
                "var x = 2",
                "var y = 3",
                "fun foo(x: Int, unused: Int) = x + y",
                "foo(x, x) * foo(y, y)"
            )
            Assert.assertEquals(30, compileAndEval(base, lines))
        }
    }

    @Test
    fun testList() {
        JsReplBase().use { base ->
            val lines = listOf(
                "var a = 4",
                "var b = 6",
                "listOf(a, 5, b).last()"
            )
            Assert.assertEquals(6, compileAndEval(base, lines))
        }
    }

    @Test
    fun testMatchingNames() {
        JsReplBase().use { base ->
            val lines = listOf(
                "fun foo(i: Int) = i + 2",
                "fun foo(s: String) = s",
                "class C {fun foo(s: String) = s + s}",
                "foo(\"x\") + foo(2) + C().foo(\"class\")"
            )
            Assert.assertEquals("x4classclass", compileAndEval(base, lines))
        }
    }

    @Test
    fun testInline() {
        JsReplBase().use { base ->
            val lines = listOf(
                "inline fun foo(i : Int) = if (i % 2 == 0) {} else i",
                """
                fun box(): String {
                    val a = foo(1)
                    if (a != 1) return "fail1: ${'$'}a"
    
                    val b = foo(2)
                    if (b != Unit) return "fail2: ${'$'}b"
    
                    return "OK"
                },
                box()
                """
            )
            Assert.assertEquals("OK", compileAndEval(base, lines))
        }
    }

    @Test
    fun testAnonymous() {
        JsReplBase().use { base ->
            val lines = listOf(
                """
                inline fun foo(f: () -> String): () -> String {
                    val result = f()
                    return { result }
                }
                """,
                "fun bar(f: () -> String) = foo(f)()",
                "fun box(): String = bar { \"OK\" }",
                "box()"
            )
            Assert.assertEquals("OK", compileAndEval(base, lines))
        }
    }

    @Test
    fun testNoneLocalReturn() {
        JsReplBase().use { base ->
            val lines = listOf(
                """
                inline fun f(ignored: () -> Any): Any {
                    return ignored()
                }
                """,
                """
                fun test(): String {
                    f { return "OK" };
                    return "error"
                }
                """,
                "test()"
            )
            Assert.assertEquals("OK", compileAndEval(base, lines))
        }
    }

    @Test
    fun testInstanceOf() {
        JsReplBase().use { base ->
            val lines = listOf(
                """
                val list = listOf(1, 2, 3)
                val f: Boolean = list is List<Int>
                """,
                "val s = list is List<Int>",
                "f.toString() + s.toString()"
            )
            Assert.assertEquals("truetrue", compileAndEval(base, lines))
        }
    }


    @Test
    fun testScopes() {
        JsReplBase().use { base ->
            val lines = listOf(
                """
                fun foo(): Int {
                    var t = 2 * 2
                    class A(val value: Int = 5) {
                        fun bar(): Int {
                            var q = 4
                            var w = 1
                            return q + w
                        }
                    }
    
                    return A().bar() * 2
                }
                foo()
                """
            )
            Assert.assertEquals(10, compileAndEval(base, lines))
        }
    }

    @Test
    fun testEvaluateFunctionName() {
        JsReplBase().use { base ->
            val lines = listOf(
                "fun evaluateScript() = 5",
                "fun foo(i: Int) = i + evaluateScript()",
                "foo(5)"
            )
            Assert.assertEquals(10, compileAndEval(base, lines))
        }
    }

    private fun compileAndEval(base: JsReplBase, lines: List<String>): Any? {
        val state = base.compiler.createState()

        var result: Any? = null
        lines.forEach { line ->
            val compileResult = base.compiler.compile(
                state,
                makeReplCodeLine(base.newSnippetId(), line)
            )
            if (compileResult !is ReplCompileResult.CompiledClasses) return compileResult.toString()

            val evalResult = base.jsEngine.eval(state, compileResult)
            if (evalResult !is ReplEvalResult.ValueResult) return evalResult.toString()

            result = evalResult.value
        }
        return result
    }
}
