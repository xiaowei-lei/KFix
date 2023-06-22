package com.kfix.patch.generator

import com.kfix.patch.generator.collectors.Collector
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class PatchClassCollectorChainTest {

    @Test
    fun `should acc all collectors result`() {
        val collector1 = mockk<Collector> {
            every { proceed(any()) } returns listOf(
                Collector.Item(
                    className = "com.example.MainActivity",
                    collectorTag = "tag1"
                )
            )
        }
        val collector2 = mockk<Collector> {
            every { proceed(any()) } returns listOf(
                Collector.Item(
                    className = "com.example.MainViewModel",
                    collectorTag = "tag2"
                )
            )
        }
        PatchClassCollectorChain(
            collectors = listOf(
                collector1,
                collector2
            )
        ).proceed() shouldBe setOf(
            Collector.Item(
                className = "com.example.MainActivity",
                collectorTag = "tag1"
            ),
            Collector.Item(
                className = "com.example.MainViewModel",
                collectorTag = "tag2"
            )
        )
    }
}