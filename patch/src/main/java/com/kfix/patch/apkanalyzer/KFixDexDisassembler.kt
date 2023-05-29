package com.example.plugin.patch.apkanalyzer

import com.android.tools.apk.analyzer.dex.PackageTreeCreator
import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.apk.analyzer.internal.rewriters.FieldReferenceWithNameRewriter
import com.android.tools.apk.analyzer.internal.rewriters.MethodReferenceWithNameRewriter
import com.android.tools.proguard.ProguardMap
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.rewriter.DexRewriter
import com.android.tools.smali.dexlib2.rewriter.Rewriter
import com.android.tools.smali.dexlib2.rewriter.RewriterModule
import com.android.tools.smali.dexlib2.rewriter.Rewriters
import com.android.tools.smali.dexlib2.rewriter.TypeRewriter
import com.android.tools.smali.util.IndentingWriter
import java.io.IOException
import java.io.StringWriter

/**
 * @see com.android.tools.apk.analyzer.dex.DexDisassembler
 */
class KFixDexDisassembler(dexFile: DexBackedDexFile, proguardMap: ProguardMap?) {

    private val proguardMap: ProguardMap?

    private val classDefMap: Map<String, ClassDef>

    init {
        val rewriteDexFile = if (proguardMap == null) dexFile else rewriteDexFile(dexFile, proguardMap)
        this.proguardMap = proguardMap
        this.classDefMap = rewriteDexFile.classes.associateBy(
            keySelector = { it.type },
            valueTransform = {
                SortedClassDef(it)
            }
        )
    }

    private fun classCount(): Int = classDefMap.size

    @Throws(IOException::class)
    fun disassembleClass(fqcn: String): String {
        val className =
            PackageTreeCreator.decodeClassName(SigUtils.typeToSignature(fqcn), proguardMap)
        val classDef: ClassDef? = getClassDef(className)
        checkNotNull(classDef) { "Unable to locate class definition for $className" }
        return classDef.smaliText()
    }

    fun onEachClassDef(action: (index: Int, total: Int, classDef: ClassDef) -> Unit) {
        classDefMap.onEachIndexed { index, classDefEntry ->
            action.invoke(index, classCount(), classDefEntry.value)
        }
    }

    private fun getClassDef(fqcn: String): ClassDef? {
        val signature = SigUtils.typeToSignature(fqcn)
        return classDefMap[signature]
    }

    companion object {

        private fun rewriteDexFile(dexFile: DexFile, map: ProguardMap): DexFile {
            val rewriter = getRewriter(map)
            return rewriter.dexFileRewriter.rewrite(dexFile)
        }

        private fun getRewriter(map: ProguardMap): DexRewriter {
            return DexRewriter(
                object : RewriterModule() {

                    override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
                        return object : TypeRewriter() {

                            override fun rewrite(typeName: String): String {
                                return SigUtils.typeToSignature(
                                    PackageTreeCreator.decodeClassName(typeName, map)
                                )
                            }
                        }
                    }


                    override fun getFieldReferenceRewriter(
                        rewriters: Rewriters,
                    ): Rewriter<FieldReference> {
                        return object : FieldReferenceWithNameRewriter(rewriters) {
                            override fun rewriteName(fieldReference: FieldReference): String {
                                return PackageTreeCreator.decodeFieldName(fieldReference, map)
                            }
                        }
                    }


                    override fun getMethodReferenceRewriter(
                        rewriters: Rewriters,
                    ): Rewriter<MethodReference> {
                        return object : MethodReferenceWithNameRewriter(rewriters) {
                            override fun rewriteName(methodReference: MethodReference?): String {
                                return PackageTreeCreator.decodeMethodName(methodReference, map)
                            }
                        }
                    }
                })
        }
    }

    class SortedClassDef(private val classDef: ClassDef) : ClassDef by classDef {
        override fun getType(): String {
            return classDef.type
        }

        override fun getAnnotations(): MutableSet<out Annotation> {
            return classDef.annotations.toSortedSet()
        }

        override fun getInterfaces(): List<String> {
            return classDef.interfaces.sorted()
        }

        override fun getStaticFields(): Iterable<Field> {
            return classDef.staticFields.sorted()
        }

        override fun getInstanceFields(): Iterable<Field> {
            return classDef.instanceFields.sorted()
        }

        override fun getFields(): Iterable<Field> {
            return classDef.fields.sorted()
        }

        override fun getDirectMethods(): Iterable<Method> {
            return classDef.directMethods.sorted()
        }

        override fun getVirtualMethods(): Iterable<Method> {
            return classDef.virtualMethods.sorted()
        }
    }
}

fun ClassDef.smaliText(): String {
    val options = BaksmaliOptions()
    val classDefinition = ClassDefinition(options, this)
    val writer = StringWriter(1024)
    BaksmaliWriter(IndentingWriter(writer)).use { iw -> classDefinition.writeTo(iw) }
    return writer.toString().replace("\r", "")
}
