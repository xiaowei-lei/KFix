package com.kfix.patch.generator.collectors

interface Collector {
    data class Item(
        val className: String,
        val collectorTag: String
    )
    fun proceed(items: Collection<Item>): Collection<Item>

    fun tag(): String {
        return javaClass.simpleName
    }
}