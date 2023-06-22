package com.kfix.patch.generator

import com.kfix.patch.generator.collectors.Collector

class PatchClassCollectorChain(
    private val collectors: List<Collector>,
) {
    fun proceed(): Set<Collector.Item> {
        val acc = mutableSetOf<Collector.Item>()
        collectors.forEach { collector ->
            acc.addAll(collector.proceed(acc))
        }
        return acc
    }
}