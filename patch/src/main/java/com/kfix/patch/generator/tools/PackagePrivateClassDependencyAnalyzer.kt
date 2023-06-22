package com.kfix.patch.generator.tools

import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.kfix.patch.tools.AccessVisibleChecker

class PackagePrivateClassDependencyAnalyzer(
    private val classDefProvider: (classDefType: String) -> ClassDef?,
) {

    private val accessVisibleChecker = AccessVisibleChecker(classDefProvider)

    /**
     * @param startClassNames collection of fully qualified class names.
     * @return All dependencies(fully qualified class name), include transitive dependencies.
     */
    fun analyzeDependencies(
        startClassNames: Collection<String>,
        dependenciesFilter: (className: String) -> Boolean = { true },
    ): Collection<String> {
        val startSignatures = startClassNames.map(SigUtils::typeToSignature)
        val handled = mutableSetOf<String>()
        val notHandled = ArrayDeque(startSignatures)
        while (notHandled.isNotEmpty()) {
            val fqcn = notHandled.removeFirst()
            handled.add(fqcn)
            val classDef = classDefProvider.invoke(fqcn)
            if (classDef != null) {
                val dependencies = getDirectDependencies(classDef).filter {
                    dependenciesFilter.invoke(SigUtils.signatureToName(it))
                }
                val notHandledDependencies = dependencies.subtract(handled)
                notHandled.addAll(notHandledDependencies)
            }
        }

        return handled.subtract(startSignatures.toSet()).map(SigUtils::signatureToName)
    }

    private fun getDirectDependencies(
        classDef: ClassDef,
    ): Set<String> {
        return mutableSetOf<String?>()
            .apply {
                add(classDef.superclass)
                addAll(classDef.interfaces)
                addAll(classDef.annotations.map { it.type })
                addAll(classDef.fields.map { it.type })
            }
            .filterNotNull()
            .filterNot {
                accessVisibleChecker.isTypeVisible(
                    callerClass = classDef.type,
                    type = it
                )
            }
            .toMutableSet().apply {
                addAll(classDef.methods.flatMap { it.referenceTypes(accessVisibleChecker) })
            }
    }

    private fun Method.referenceTypes(accessVisibleChecker: AccessVisibleChecker): Set<String> {
        val references = mutableSetOf<String>()
        val callerClass = definingClass
        references.addAll(parameters.map { it.type })
        references.add(returnType)
        references.addAll(annotations.map { it.type })

        val invisibleSet = mutableSetOf<String>()

        val instructionTypes = implementation?.instructions?.flatMap { instruction ->
            when (instruction) {
                is ReferenceInstruction -> {
                    when (val reference = instruction.reference) {
                        is FieldReference -> mutableSetOf<String>().apply {
                            add(reference.type)
                            add(reference.definingClass)
                            if (!accessVisibleChecker.isFieldVisible(
                                    callerClass = callerClass,
                                    fieldName = reference.name,
                                    fieldDefiningClass = reference.definingClass
                                )
                            ) {
                                invisibleSet.add(reference.definingClass)
                            }
                        }

                        is MethodReference -> mutableSetOf<String>().apply {
                            add(reference.returnType)
                            add(reference.definingClass)
                            addAll(reference.parameterTypes.map { it.toString() })
                            if (!accessVisibleChecker.isMethodVisible(
                                    callerClass = callerClass,
                                    methodName = reference.name,
                                    methodDefiningClass = reference.definingClass,
                                    methodParameterTypes = reference.parameterTypes
                                )
                            ) {
                                invisibleSet.add(reference.definingClass)
                            }
                        }

                        is TypeReference -> listOf(
                            reference.type
                        )

                        else -> emptySet()
                    }
                }

                else -> emptySet()
            }
        }.orEmpty()
        references.addAll(instructionTypes)
        val union = references.filterNot {
            accessVisibleChecker.isTypeVisible(
                callerClass = callerClass,
                type = it
            )
        }.union(invisibleSet)
        return union
    }
}

