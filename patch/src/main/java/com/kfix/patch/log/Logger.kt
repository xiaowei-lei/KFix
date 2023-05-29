package com.kfix.patch.log

class Logger {
    companion object {
        private const val COMMON_TAG = "KFix"
        fun i(tag: String, text: String) {
            println("[$COMMON_TAG][$tag]: $text")
        }

        fun progress(tag: String, text: String) {
            print("\r[$COMMON_TAG][$tag]: $text")
            System.out.flush()
        }

        fun progressEnd() {
            println()
        }
    }
}

