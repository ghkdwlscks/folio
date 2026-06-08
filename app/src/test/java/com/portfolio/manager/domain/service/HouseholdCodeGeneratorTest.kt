package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class HouseholdCodeGeneratorTest {

    private val generator = HouseholdCodeGenerator()

    @Test
    fun `generate - produces XXXX-XXXX from unambiguous charset`() {
        val code = generator.generate(Random(1))

        assertThat(code).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}")
    }

    @Test
    fun `generate - is deterministic for a given seed`() {
        assertThat(generator.generate(Random(7))).isEqualTo(generator.generate(Random(7)))
    }

    @Test
    fun `normalize - uppercases trims and inserts dash`() {
        assertThat(generator.normalize(" abcd2345 ")).isEqualTo("ABCD-2345")
        assertThat(generator.normalize("abcd-2345")).isEqualTo("ABCD-2345")
    }

    @Test
    fun `normalize - leaves wrong length cleaned but undashed`() {
        assertThat(generator.normalize("abc")).isEqualTo("ABC")
    }

    @Test
    fun `isValid - accepts canonical code`() {
        assertThat(generator.isValid("ABCD-2345")).isTrue()
    }

    @Test
    fun `isValid - rejects ambiguous or malformed codes`() {
        assertThat(generator.isValid("ABC-2345")).isFalse()   // too short
        assertThat(generator.isValid("ABCD-23O5")).isFalse()  // 'O' not in charset
        assertThat(generator.isValid("ABCD23450")).isFalse()  // no dash, wrong length
    }
}
