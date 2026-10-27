package com.shuaib.classmate.utils

import com.shuaib.classmate.models.Period
import com.shuaib.classmate.models.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherClassIdentifierTest {

    private val teacherUser = User(
        uid = "teacher_123",
        name = "Mahbubur Rahman",
        fullName = "Dr. Md. Mahbubur Rahman",
        email = "mahbub@gstu.ac.bd",
        teacherCode = "MR",
        role = "teacher"
    )

    @Test
    fun testTier1UidMatch() {
        val period = Period(id = "p1", subject = "Data Structures", teacher = "Someone Else", teacherUid = "teacher_123")
        assertTrue(TeacherClassIdentifier.isTeacherClass(period, teacherUser))
    }

    @Test
    fun testTier2EmailMatch() {
        val period = Period(id = "p2", subject = "Algorithms", teacher = "Dr. Unknown", teacherEmail = "mahbub@gstu.ac.bd")
        assertTrue(TeacherClassIdentifier.isTeacherClass(period, teacherUser))
    }

    @Test
    fun testTier3CodeMatch() {
        val period1 = Period(id = "p3", subject = "Database Systems", teacher = "MR")
        assertTrue(TeacherClassIdentifier.isTeacherClass(period1, teacherUser))

        val period2 = Period(id = "p4", subject = "Database Systems", teacher = "Unknown", teacherCode = "mr")
        assertTrue(TeacherClassIdentifier.isTeacherClass(period2, teacherUser))
    }

    @Test
    fun testTier4NormalizedNameMatch() {
        val period = Period(id = "p5", subject = "Networking", teacher = "Dr. Mahbubur Rahman")
        assertTrue(TeacherClassIdentifier.isTeacherClass(period, teacherUser))
    }

    @Test
    fun testMismatch() {
        val period = Period(id = "p6", subject = "Physics", teacher = "Dr. Anisur Rahman")
        assertFalse(TeacherClassIdentifier.isTeacherClass(period, teacherUser))
    }
}
