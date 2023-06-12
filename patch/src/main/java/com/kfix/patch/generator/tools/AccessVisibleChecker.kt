package com.kfix.patch.tools

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef

class AccessVisibleChecker(
    private val classDefProvider: (classDefType: String) -> ClassDef?,
) {
    fun isTypeVisible(
        @Suppress("UNUSED_PARAMETER")
        callerClass: String = "",
        type: String,
    ): Boolean {

        val classDef = classDefProvider.invoke(type) ?: return true
        return AccessFlags.PUBLIC.isSet(classDef.accessFlags)
    }

    fun isFieldVisible(
        callerClass: String = "",
        fieldDefiningClass: String,
        fieldName: String,
    ): Boolean {
        val classDef = classDefProvider.invoke(fieldDefiningClass) ?: return true
        if (AccessFlags.PUBLIC.isSet(classDef.accessFlags)) {
            val field = classDef.fields.find { it.name == fieldName } ?: return false
            if (AccessFlags.PUBLIC.isSet(field.accessFlags)) {
                return true
            }
            if (AccessFlags.PROTECTED.isSet(field.accessFlags)) {
                return !isUnderSamePackage(callerClass, fieldDefiningClass)
            }
        }
        return false
    }

    fun isMethodVisible(
        callerClass: String = "",
        methodDefiningClass: String,
        methodName: String,
        methodParameterTypes: List<CharSequence>,
    ): Boolean {
        val classDef = classDefProvider.invoke(methodDefiningClass) ?: return true
        if (AccessFlags.PUBLIC.isSet(classDef.accessFlags)) {
            val method = classDef.methods.find {
                it.name == methodName && it.parameterTypes == methodParameterTypes
            } ?: return false
            if (AccessFlags.PUBLIC.isSet(method.accessFlags)) {
                return true
            }
            if (AccessFlags.PROTECTED.isSet(method.accessFlags)) {
                return !isUnderSamePackage(first = callerClass,  second = methodDefiningClass)
            }
        }
        return false
    }

    private fun  isUnderSamePackage(first: String, second: String): Boolean {
        return first.substringBeforeLast("/") == second.substringBeforeLast("/")
    }
}