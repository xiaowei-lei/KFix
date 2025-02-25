package com.kfix.patch.log

class Logger {
    companion object {
        private const val COMMON_TAG = "kfix"
        fun i(tag: String, text: String) {
            println("[$COMMON_TAG][$tag]: $text")
        }

        fun progress(tag: String, text: String) {
            print("\r[$COMMON_TAG][$tag]: $text")
            System.out.flush()
        }

        fun progressEnd(tag: String, text: String) {
            progress(tag, text)
            println()
        }
    }
}

