package com.kfix.patch.proguard

import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.apk.analyzer.internal.rewriters.FieldReferenceWithNameRewriter
import com.android.tools.apk.analyzer.internal.rewriters.MethodReferenceWithNameRewriter
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.rewriter.DexRewriter
import com.android.tools.smali.dexlib2.rewriter.Rewriter
import com.android.tools.smali.dexlib2.rewriter.RewriterModule
import com.android.tools.smali.dexlib2.rewriter.Rewriters
import com.android.tools.smali.dexlib2.rewriter.TypeRewriter
import com.kfix.patch.log.Logger

class PatchDexRewriter {

    fun rewrite(
        newDexFile: DexFile,
        oldProguardMap: PatchProguardMap,
        newProguardMap: PatchProguardMap,
    ): DexFile {
        return DexRewriter(
            DexRewriterModule(
                oldProguardMap,
                newProguardMap
            )
        ).dexFileRewriter.rewrite(newDexFile)
    }

    class DexRewriterModule(
        val oldProguardMap: PatchProguardMap,
        val newProguardMap: PatchProguardMap,
    ) : RewriterModule() {
        companion object {
            const val TAG = "DexRewriterModule"
        }
        override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
            return object : TypeRewriter() {
                override fun rewrite(typeName: String): String {
                    val relatedObfuscatedClass = findRelatedObfuscatedClassFromOld(typeName)
                    if (relatedObfuscatedClass == null) {
                        if (isNewApkClass(typeName)) {
                            Logger.i(TAG, "[TypeRewriter] added type: `${typeName}`")
                        }
                        return typeName
                    }
                    val targetType = SigUtils.typeToSignature(relatedObfuscatedClass)
                    if (targetType != typeName) {
                        Logger.i(TAG, "[TypeRewriter] remap type: `${typeName}` -> `$relatedObfuscatedClass`")
                    }
                    return SigUtils.typeToSignature(relatedObfuscatedClass)
                }
            }
        }

        private fun isNewApkClass(typeName: String): Boolean {
            return newProguardMap.containsTargetObfuscatedClassName(SigUtils.signatureToName(typeName))
        }

        private fun getClearClassNameFromNew(typeName: String): String {
            val newObfuscatedClassName = SigUtils.signatureToName(typeName)
            return newProguardMap.proguardMap.getClassName(newObfuscatedClassName)
        }

        private fun findRelatedObfuscatedClassFromOld(typeName: String): String? {
            val newClearClassName = getClearClassNameFromNew(typeName)
            return oldProguardMap.findObfuscatedClassName(newClearClassName)
        }

        override fun getFieldReferenceRewriter(
            rewriters: Rewriters,
        ): Rewriter<FieldReference> {
            return object : FieldReferenceWithNameRewriter(rewriters) {
                override fun rewriteName(fieldReference: FieldReference): String {
                    val typeName = fieldReference.definingClass
                    if (findRelatedObfuscatedClassFromOld(typeName) == null) {
                        return fieldReference.name
                    }
                    val clearClassName = getClearClassNameFromNew(typeName)
                    val obfuscatedFieldName = oldProguardMap.findObfuscatedFieldName(
                        clearClassName,
                        fieldReference.name
                    )
                    if (obfuscatedFieldName == null) {
                        Logger.i(TAG, "[FieldReferenceWithNameRewriter] not found field ${typeName}#${fieldReference.name} in old proguard map.")
                        return fieldReference.name
                    }
                    return obfuscatedFieldName
                }
            }
        }

        override fun getMethodReferenceRewriter(
            rewriters: Rewriters,
        ): Rewriter<MethodReference?> {
            return object : MethodReferenceWithNameRewriter(rewriters) {
                override fun rewriteName(methodReference: MethodReference): String {
                    val typeName = methodReference.definingClass
                    if (findRelatedObfuscatedClassFromOld(typeName) == null) {
                        return methodReference.name
                    }

                    val clearClassName = getClearClassNameFromNew(typeName)
                    val sigWithoutName =
                        DexFormatter.INSTANCE.getShortMethodDescriptor(methodReference)
                            .substring(methodReference.name.length)
                    val newFrame = newProguardMap.proguardMap.getFrame(
                        clearClassName,
                        methodReference.name,
                        sigWithoutName,
                        "",
                        0
                    )

                    return oldProguardMap.findObfuscatedMethodName(
                        clearClassName,
                        newFrame.methodName,
                        newFrame.signature
                    ) ?: methodReference.name.also {
                        Logger.i(TAG, "[MethodReferenceWithNameRewriter] not found related method ${typeName}#${methodReference.name} in old proguard map")
                    }
                }
            }
        }
    }
}