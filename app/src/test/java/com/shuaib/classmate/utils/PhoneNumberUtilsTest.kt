package com.shuaib.classmate.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun testNormalizeStartsWith01() {
        val input = "01712345678"
        val expected = "+8801712345678"
        assertEquals(expected, PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizeStartsWith8801() {
        val input = "8801812345678"
        val expected = "+8801812345678"
        assertEquals(expected, PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizeAlreadyPlus8801() {
        val input = "+8801912345678"
        val expected = "+8801912345678"
        assertEquals(expected, PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizeWithSpacesAndHyphens() {
        val input = " 01712-345 678 "
        val expected = "+8801712345678"
        assertEquals(expected, PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizeWithParentheses() {
        val input = "+88 (017) 12345678"
        val expected = "+8801712345678"
        assertEquals(expected, PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizePreservesEmergencyShortCode() {
        val input = "999"
        assertEquals("999", PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizePreservesLandline() {
        val input = "029555555"
        assertEquals("029555555", PhoneNumberUtils.normalize(input))
    }

    @Test
    fun testNormalizeEmptyOrBlank() {
        assertEquals("", PhoneNumberUtils.normalize(""))
        assertEquals("", PhoneNumberUtils.normalize("   "))
        assertEquals("", PhoneNumberUtils.normalize(null))
    }
}
