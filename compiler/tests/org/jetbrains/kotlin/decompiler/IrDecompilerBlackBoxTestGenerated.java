/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/decompiler/box")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class IrDecompilerBlackBoxTestGenerated extends AbstractIrDecompilerBlackBoxTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInBox() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/decompiler/box"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("classes.kt")
    public void testClasses() throws Exception {
        runTest("compiler/testData/decompiler/box/classes.kt");
    }

    @TestMetadata("dummy.kt")
    public void testDummy() throws Exception {
        runTest("compiler/testData/decompiler/box/dummy.kt");
    }

    @TestMetadata("loops.kt")
    public void testLoops() throws Exception {
        runTest("compiler/testData/decompiler/box/loops.kt");
    }

    @TestMetadata("paramWhen.kt")
    public void testParamWhen() throws Exception {
        runTest("compiler/testData/decompiler/box/paramWhen.kt");
    }

    @TestMetadata("simpleOperators.kt")
    public void testSimpleOperators() throws Exception {
        runTest("compiler/testData/decompiler/box/simpleOperators.kt");
    }

    @TestMetadata("simpleWhen.kt")
    public void testSimpleWhen() throws Exception {
        runTest("compiler/testData/decompiler/box/simpleWhen.kt");
    }
}
