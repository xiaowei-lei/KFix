package com.kfix.patch.generator.tools

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.kfix.patch.tools.AccessVisibleChecker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class AccessVisibleCheckerTest {
    private val classDef = mockk<ClassDef>(relaxed = true)
    private val checker = AccessVisibleChecker { classDef }
    private val noClassFoundChecker = AccessVisibleChecker { null }

    private fun prepareClassDef(
        classAccessFlags: Int = 0,
        fieldName: String = "isLoading",
        fieldAccessFlags: Int = 0,
        methodName: String = "reset",
        methodParameterTypes: List<CharSequence> = listOf(),
        methodAccessFlags: Int = 0,
    ): ClassDef {
        return classDef.apply {
            every { accessFlags } returns classAccessFlags
            every { classDef.fields } returns listOf(
                mockk {
                    every { name } returns fieldName
                    every { accessFlags } returns fieldAccessFlags
                }
            )
            every { classDef.methods } returns listOf(
                mockk {
                    every { name } returns methodName
                    every { accessFlags } returns methodAccessFlags
                    every { parameterTypes } returns methodParameterTypes
                }
            )
        }
    }

    @Test
    fun `field should be visible when fieldDefiningClass accessFlags is public and field accessFlags is public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            fieldAccessFlags = AccessFlags.PUBLIC.value,
            fieldName = "isLoading"
        )

        checker.isFieldVisible(
            fieldDefiningClass = "Lcom/example/MainViewModel;",
            fieldName = "isLoading"
        ) shouldBe true
    }

    @Test
    fun `field should be invisible when fieldDefiningClass accessFlags is not public`() {
        prepareClassDef(
            classAccessFlags = 0,
            fieldAccessFlags = AccessFlags.PUBLIC.value,
        )

        checker.isFieldVisible(
            fieldDefiningClass = "Lcom/example/MainViewModel;",
            fieldName = "isLoading"
        ) shouldBe false
    }

    @Test
    fun `field should be invisible when field accessFlags is not public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            fieldAccessFlags = 0,
        )

        checker.isFieldVisible(
            fieldDefiningClass = "Lcom/example/MainViewModel;",
            fieldName = "isLoading"
        ) shouldBe false
    }

    @Test
    fun `field should be visible when fieldDefiningClass is not found`() {
        noClassFoundChecker.isFieldVisible(
            fieldDefiningClass = "Lcom/example/MainViewModel;",
            fieldName = "isLoading"
        ) shouldBe true
    }

    @Test
    fun `field should be visible when fieldDefiningClass is public and field is protected`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            fieldName = "isLoading",
            fieldAccessFlags = AccessFlags.PROTECTED.value
        )
        checker.isFieldVisible(
            callerClass = "Lcom/example/MainViewModel",
            fieldDefiningClass = "Lcom/example/common/BaseViewModel;",
            fieldName = "isLoading",
        ) shouldBe true
    }

    @Test
    fun `method should be visible when methodDefiningClass accessFlags is public and method accessFlags is public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            methodAccessFlags = AccessFlags.PUBLIC.value,
            methodName = "reset",
            methodParameterTypes = listOf()
        )

        checker.isMethodVisible(
            methodDefiningClass = "Lcom/example/MainViewModel;",
            methodName = "reset",
            methodParameterTypes = listOf()
        ) shouldBe true
    }

    @Test
    fun `method should be invisible when methodDefiningClass accessFlags is not public`() {
        prepareClassDef(
            classAccessFlags = 0,
            methodAccessFlags = AccessFlags.PUBLIC.value,
            methodName = "reset",
            methodParameterTypes = listOf()
        )

        checker.isMethodVisible(
            methodDefiningClass = "Lcom/example/MainViewModel;",
            methodName = "reset",
            methodParameterTypes = listOf()
        ) shouldBe false
    }

    @Test
    fun `method should be invisible when methodDefiningClass accessFlags is public but method accessFlags is not public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            methodAccessFlags = 0,
            methodName = "reset",
            methodParameterTypes = listOf()
        )

        checker.isMethodVisible(
            methodDefiningClass = "Lcom/example/MainViewModel;",
            methodName = "reset",
            methodParameterTypes = listOf()
        ) shouldBe false
    }


    @Test
    fun `method should be visible when methodDefiningClass is not found`() {
        noClassFoundChecker.isMethodVisible(
            methodDefiningClass = "Lcom/example/MainViewModel;",
            methodName = "reset",
            methodParameterTypes = listOf()
        ) shouldBe true
    }

    @Test
    fun `method should be visible when methodDefiningClass is public and method is protected`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value,
            methodName = "onClear",
            methodAccessFlags = AccessFlags.PROTECTED.value
        )
        checker.isMethodVisible(
            callerClass = "Lcom/example/MainViewModel",
            methodDefiningClass = "Lcom/example/common/BaseViewModel;",
            methodName = "onClear",
            methodParameterTypes = listOf()
        ) shouldBe true
    }

    @Test
    fun `type should be visible when accessFlags is public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PUBLIC.value
        )

        checker.isTypeVisible(type = "Lcom/example/MainViewModel;") shouldBe true
    }

    @Test
    fun `type should be invisible when accessFlags is not public`() {
        prepareClassDef(
            classAccessFlags = AccessFlags.PROTECTED.value
        )

        checker.isTypeVisible(type = "Lcom/example/MainViewModel;") shouldBe false
    }

    @Test
    fun `type should be visible when type is not found`() {
        noClassFoundChecker.isTypeVisible(type = "Lcom/example/MainViewModel;") shouldBe true
    }
}