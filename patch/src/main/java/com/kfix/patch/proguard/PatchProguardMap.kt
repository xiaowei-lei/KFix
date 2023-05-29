/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.kfix.patch.proguard

import com.android.tools.proguard.ProguardMap
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.Reader
import java.text.ParseException

// Based on com.android.tools.proguard.ProguardMap, add some enhancement.
class PatchProguardMap {

    private val packageFromClearName = mutableMapOf<String, String>()
    private val classesFromClearName: MutableMap<String, ClassData> = HashMap()
    private val classesFromObfuscatedName: MutableMap<String, ClassData> = HashMap()

    val proguardMap = ProguardMap()

    // Read in proguard mapping information from the given file.
    @Throws(FileNotFoundException::class, IOException::class, ParseException::class)
    fun readFromFile(mapFile: File) {
        proguardMap.readFromFile(mapFile)
        readFromReader(FileReader(mapFile))
    }

    // Read in proguard mapping information from the given Reader.
    @Throws(IOException::class, ParseException::class)
    private fun readFromReader(mapReader: Reader) {
        val reader = BufferedReader(mapReader)
        var line = reader.readLine()
        while (line != null) {
            // Line may start with '#' as part of R8 markers, e.g.,
            //   '# compiler: R8'
            // Allow comments or empty lines in class mapping lines.
            var trimmed = line.trim { it <= ' ' }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                line = reader.readLine()
                continue
            }
            // Class lines are of the form:
            //   'clear.class.name -> obfuscated_class_name:'
            var sep = line.indexOf(" -> ")
            if (sep == -1 || sep + 5 >= line.length) {
                parseException("Error parsing class line: '$line'")
            }
            val clearClassName = line.substring(0, sep)
            val obfuscatedClassName = line.substring(sep + 4, line.length - 1)
            val classData = ClassData(clearClassName, obfuscatedClassName)
            classesFromClearName[clearClassName] = classData
            classesFromObfuscatedName[obfuscatedClassName] = classData

            val clearPackageName = clearClassName.replaceAfterLast(".",  "")
            val obfuscatedPackageName = obfuscatedClassName.replaceAfterLast(".", "")
            packageFromClearName[clearPackageName] = obfuscatedPackageName

            // After the class line comes zero or more field/method lines of the form:
            //   '    type clearName -> obfuscatedName'
            line = reader.readLine()
            while (line != null) {
                trimmed = line.trim { it <= ' ' }
                // Allow comments or empty lines in field/method mapping lines.
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    line = reader.readLine()
                    continue
                }
                // After skipping comments or empty line,
                // make sure this is a field/method mapping line.
                if (!line.startsWith("    ")) {
                    break
                }
                val ws = trimmed.indexOf(' ')
                sep = trimmed.indexOf(" -> ")
                if (ws == -1 || sep == -1) {
                    parseException("Error parse field/method line: '$line'")
                }
                var type = trimmed.substring(0, ws)
                var clearName = trimmed.substring(ws + 1, sep)
                val obfuscatedName = trimmed.substring(sep + 4, trimmed.length)

                // If the clearName contains '(', then this is for a method instead of a
                // field.
                if (clearName.indexOf('(') == -1) {
                    classData.addField(obfuscatedName, clearName)
                } else {
                    // For methods, the type is of the form: [#:[#:]]<returnType>
                    var obfuscatedLine = 0
                    var colon = type.indexOf(':')
                    if (colon != -1) {
                        obfuscatedLine = type.substring(0, colon).toInt()
                        type = type.substring(colon + 1)
                    }
                    colon = type.indexOf(':')
                    if (colon != -1) {
                        type = type.substring(colon + 1)
                    }

                    // For methods, the clearName is of the form: <clearName><sig>[:#[:#]]
                    val op = clearName.indexOf('(')
                    val cp = clearName.indexOf(')')
                    if (op == -1 || cp == -1) {
                        parseException("Error parse method line: '$line'")
                    }
                    val sig = clearName.substring(op, cp + 1)
                    var clearLine = obfuscatedLine
                    colon = clearName.lastIndexOf(':')
                    if (colon != -1) {
                        clearLine = clearName.substring(colon + 1).toInt()
                        clearName = clearName.substring(0, colon)
                    }
                    colon = clearName.lastIndexOf(':')
                    if (colon != -1) {
                        clearLine = clearName.substring(colon + 1).toInt()
                        clearName = clearName.substring(0, colon)
                    }
                    clearName = clearName.substring(0, op)
                    val clearSig = fromProguardSignature(sig + type)
                    classData.addFrame(
                        obfuscatedName, clearName, clearSig,
                        obfuscatedLine, clearLine
                    )
                }
                line = reader.readLine()
            }
        }
        reader.close()
    }

    // Returns the deobfuscated version of the given class name. If no
    // deobfuscated version is known, the original string is returned.
    private fun getClearClassName(obfuscatedClassName: String): String {
        // Class names for arrays may have trailing [] that need to be
        // stripped before doing the lookup.
        var baseName = obfuscatedClassName
        var arraySuffix = ""
        while (baseName.endsWith(ARRAY_SYMBOL)) {
            arraySuffix += ARRAY_SYMBOL
            baseName = baseName.substring(0, baseName.length - ARRAY_SYMBOL.length)
        }
        val classData = classesFromObfuscatedName[baseName]
        val clearBaseName = classData?.clearName ?: baseName

        return clearBaseName + arraySuffix
    }

    // Returns the obfuscated version of the given class name. If no
    // obfuscated version is known, the original string is returned.
    fun findObfuscatedClassName(clearClassName: String): String? {
        var baseName = clearClassName
        var arraySuffix = ""
        while (baseName.endsWith(ARRAY_SYMBOL)) {
            arraySuffix += ARRAY_SYMBOL
            baseName = baseName.substring(0, baseName.length - ARRAY_SYMBOL.length)
        }

        val classData = classesFromClearName[baseName]
        val clearBaseName = classData?.obfuscatedName ?: return null
        return clearBaseName + arraySuffix
    }

    fun containsTargetObfuscatedClassName(obfuscatedClassName: String): Boolean {
        return classesFromObfuscatedName.containsKey(obfuscatedClassName)
    }

    // Returns the deobfuscated version of the given field name for the given
    // (clear) class name. If no deobfuscated version is known, the original
    // string is returned.
    fun findObfuscatedFieldName(clearClassName: String, clearFieldName: String): String? {
        return classesFromClearName[clearClassName]?.getObfuscatedFieldName(clearFieldName)
    }
    fun findObfuscatedMethodName(
        clearClassName: String, clearMethodName: String, clearMethodSignature: String
    ): String? {
        return classesFromClearName[clearClassName]?.findObfuscatedMethod(clearMethodName, clearMethodSignature)
    }

    // Return a clear signature for the given obfuscated signature.
    private fun getSignature(obfuscatedSig: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < obfuscatedSig.length) {
            if (obfuscatedSig[i] == 'L') {
                val e = obfuscatedSig.indexOf(';', i)
                builder.append('L')
                val cls = obfuscatedSig.substring(i + 1, e).replace('/', '.')
                builder.append(getClearClassName(cls).replace('.', '/'))
                builder.append(';')
                i = e
            } else {
                builder.append(obfuscatedSig[i])
            }
            i++
        }
        return builder.toString()
    }

    companion object {
        private const val ARRAY_SYMBOL = "[]"

        fun create(mapFile: File): PatchProguardMap {
            return PatchProguardMap().apply {
                readFromFile(mapFile)
            }
        }

        @Throws(ParseException::class)
        private fun parseException(msg: String) {
            throw ParseException(msg, 0)
        }

        // Converts a proguard-formatted method signature into a Java formatted
        // method signature.
        @Throws(ParseException::class)
        private fun fromProguardSignature(sig: String): String {
            return if (sig.startsWith("(")) {
                val end = sig.indexOf(')')
                if (end == -1) {
                    parseException("Error parsing signature: $sig")
                }
                val converted = StringBuilder()
                converted.append('(')
                if (end > 1) {
                    for (arg in sig.substring(1, end).split(",").toTypedArray()) {
                        converted.append(fromProguardSignature(arg))
                    }
                }
                converted.append(')')
                converted.append(fromProguardSignature(sig.substring(end + 1)))
                converted.toString()
            } else if (sig.endsWith(ARRAY_SYMBOL)) {
                "[" + fromProguardSignature(sig.substring(0, sig.length - 2))
            } else if (sig == "boolean") {
                "Z"
            } else if (sig == "byte") {
                "B"
            } else if (sig == "char") {
                "C"
            } else if (sig == "short") {
                "S"
            } else if (sig == "int") {
                "I"
            } else if (sig == "long") {
                "J"
            } else if (sig == "float") {
                "F"
            } else if (sig == "double") {
                "D"
            } else if (sig == "void") {
                "V"
            } else {
                "L" + sig.replace('.', '/') + ";"
            }
        }

        // Return a file name for the given clear class name.
        private fun getFileName(clearClass: String): String {
            var filename = clearClass
            val dot = filename.lastIndexOf('.')
            if (dot != -1) {
                filename = filename.substring(dot + 1)
            }
            val dollar = filename.indexOf('$')
            if (dollar != -1) {
                filename = filename.substring(0, dollar)
            }
            return "$filename.java"
        }
    }

    class FrameData(
        var obfuscatedMethodName: String
    )

    class ClassData     // Constructs a ClassData object for a class with the given clear name.
        (
        // Returns the clear name of the class.
        val clearName: String,
        val obfuscatedName: String,
    ) {
        // Mapping from obfuscated field name to clear field name.
        private val _fieldsFromObfuscatedName: MutableMap<String, String> = HashMap()
        private val fieldsFromObfuscatedName: Map<String, String> = _fieldsFromObfuscatedName

        // Mapping from clear field name to obfuscated field name.
        private val _fieldsFromClearName: MutableMap<String, String> = HashMap()
        private val fieldsFromClearName: Map<String, String> = _fieldsFromClearName

        // obfuscatedMethodName + clearSignature -> FrameData
        private val framesFromObfuscatedName: MutableMap<String, FrameData> = HashMap()
        private val framesFromClearName: MutableMap<String, FrameData> = HashMap()

        fun addField(obfuscatedName: String, clearName: String) {
            _fieldsFromObfuscatedName[obfuscatedName] = clearName
            _fieldsFromClearName[clearName] = obfuscatedName
        }

        fun getClearFieldName(obfuscatedName: String): String? {
            return fieldsFromObfuscatedName[obfuscatedName]
        }

        fun getObfuscatedFieldName(clearName: String): String? {
            return fieldsFromClearName[clearName]
        }

        // TODO: Does this properly interpret the meaning of line numbers? Is
        // it possible to have multiple frame entries for the same method
        // name and signature that differ only by line ranges?
        fun addFrame(
            obfuscatedMethodName: String, clearMethodName: String,
            clearSignature: String, obfuscatedLine: Int, clearLine: Int
        ) {
            val key = obfuscatedMethodName + clearSignature
            val frameData = FrameData(
                obfuscatedMethodName
            )
            framesFromObfuscatedName[key] = frameData
            framesFromClearName[clearMethodName + clearSignature] = frameData
        }

        fun findObfuscatedMethod(clearMethodName: String, clearSignature: String): String? {
            return framesFromClearName[clearMethodName + clearSignature]?.obfuscatedMethodName
        }
    }
}