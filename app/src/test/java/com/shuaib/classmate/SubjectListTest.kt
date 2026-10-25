package com.shuaib.classmate

import com.shuaib.classmate.utils.SubjectList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectListTest {

    @Test
    fun testSameCourseMatchingWithCEandCSEtypo() {
        val courseA = "CSE204 - Object Oriented Programming II Sessional"
        val courseB = "CE204 - Object Oriented Programming II Sessional"
        assertTrue(SubjectList.isSameCourse(courseA, courseB))
        assertTrue(SubjectList.isSameCourse(courseB, courseA))
    }

    @Test
    fun testSameCourseWithCodeOnlyAndTypo() {
        val full = "CSE204 - Object Oriented Programming II Sessional"
        val codeTypo = "CE204"
        assertTrue(SubjectList.isSameCourse(full, codeTypo))
        assertTrue(SubjectList.isSameCourse(codeTypo, full))
    }

    @Test
    fun testDifferentCoursesDoNotMatch() {
        val theory = "CSE203 - Object Oriented Programming II (Python)"
        val lab = "CSE204 - Object Oriented Programming II Sessional"
        assertFalse(SubjectList.isSameCourse(theory, lab))
        assertFalse(SubjectList.isSameCourse(lab, theory))
    }

    @Test
    fun testSameCourseDataStructures() {
        val withCode = "CSE201 - Data Structure"
        val nameOnly = "Data Structure"
        val plural = "Data Structures"
        assertTrue(SubjectList.isSameCourse(withCode, nameOnly))
        assertTrue(SubjectList.isSameCourse(withCode, plural))
        assertTrue(SubjectList.isSameCourse(nameOnly, plural))
    }

    @Test
    fun testTheoryAndLabDifferentiation() {
        val theory = "CSE201 - Data Structure"
        val lab = "CSE202 - Data Structure Lab"
        assertFalse(SubjectList.isSameCourse(theory, lab))
    }

    @Test
    fun testDifferentDepartmentsDoNotMatch() {
        val eee = "EEE204"
        val cse = "CSE204"
        assertFalse(SubjectList.isSameCourse(eee, cse))
    }

    @Test
    fun testExtractCourseCode() {
        assertEquals("CE204", SubjectList.extractCourseCode("CE204 - Object Oriented Programming II Sessional"))
        assertEquals("CSE204", SubjectList.extractCourseCode("CSE204 - Object Oriented Programming II Sessional"))
        assertEquals("CSE201", SubjectList.extractCourseCode("CSE201 - Data Structure"))
    }
}
