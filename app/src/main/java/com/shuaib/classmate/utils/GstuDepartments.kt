package com.shuaib.classmate.utils

import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import com.shuaib.classmate.ui.DepartmentPickerBottomSheet
import java.util.Locale

/**
 * Standard GSTU Departments registry and search utility.
 * Contains the official 34 academic departments recognized at Gopalganj Science and Technology University (GSTU).
 */
object GstuDepartments {

    data class DepartmentItem(
        val name: String,
        val acronym: String,
        val faculty: String,
        val aliases: List<String> = emptyList()
    )

    /**
     * Canonical list of all 34 GSTU Departments in order.
     */
    val LIST = listOf(
        "Computer Science and Engineering",
        "Electrical and Electronic Engineering",
        "Electronics and Telecommunication Engineering",
        "Applied Chemistry and Chemical Engineering",
        "Civil Engineering",
        "Food and Agroprocess Engineering",
        "Architecture",

        "Mathematics",
        "Statistics",
        "Chemistry",
        "Physics",
        "Environmental Science and Disaster Management",

        "Pharmacy",
        "Biotechnology and Genetic Engineering",
        "Biochemistry and Molecular Biology",
        "Psychology",
        "Botany",

        "English",
        "Bangla",
        "History",

        "Sociology",
        "Public Administration",
        "International Relations",
        "Economics",
        "Political Science",

        "Management Studies",
        "Accounting and Information Systems",
        "Marketing",
        "Finance and Banking",
        "Tourism and Hospitality Management",

        "Law",

        "Agriculture",
        "Fisheries and Marine Bioscience",
        "Animal Science and Veterinary Medicine"
    )

    /**
     * Metadata mapped items with acronyms and faculties for enhanced search and display.
     */
    val ITEMS: List<DepartmentItem> = listOf(
        // Engineering & Technology
        DepartmentItem("Computer Science and Engineering", "CSE", "Faculty of Engineering", listOf("CS", "Computer")),
        DepartmentItem("Electrical and Electronic Engineering", "EEE", "Faculty of Engineering", listOf("Electrical")),
        DepartmentItem("Electronics and Telecommunication Engineering", "ETE", "Faculty of Engineering", listOf("Telecom")),
        DepartmentItem("Applied Chemistry and Chemical Engineering", "ACCE", "Faculty of Engineering", listOf("Chemical", "App Chem")),
        DepartmentItem("Civil Engineering", "CE", "Faculty of Engineering", listOf("Civil")),
        DepartmentItem("Food and Agroprocess Engineering", "FAE", "Faculty of Engineering", listOf("Food", "Agroprocess")),
        DepartmentItem("Architecture", "ARCH", "Faculty of Architecture", listOf("Arch")),

        // Science
        DepartmentItem("Mathematics", "MATH", "Faculty of Science", listOf("Maths")),
        DepartmentItem("Statistics", "STAT", "Faculty of Science", listOf("Stats")),
        DepartmentItem("Chemistry", "CHEM", "Faculty of Science", listOf("Chem")),
        DepartmentItem("Physics", "PHY", "Faculty of Science", listOf("Phys")),
        DepartmentItem("Environmental Science and Disaster Management", "ESDM", "Faculty of Science", listOf("Env", "Environment", "Disaster")),

        // Life Sciences
        DepartmentItem("Pharmacy", "PHARM", "Faculty of Life Science", listOf("Pharma")),
        DepartmentItem("Biotechnology and Genetic Engineering", "BGE", "Faculty of Life Science", listOf("GEB", "Biotech", "Genetic")),
        DepartmentItem("Biochemistry and Molecular Biology", "BMB", "Faculty of Life Science", listOf("Biochem", "Molecular")),
        DepartmentItem("Psychology", "PSY", "Faculty of Life Science", listOf("Psych")),
        DepartmentItem("Botany", "BOT", "Faculty of Life Science", listOf("Plant")),

        // Humanities
        DepartmentItem("English", "ENG", "Faculty of Humanities", listOf("Literature")),
        DepartmentItem("Bangla", "BAN", "Faculty of Humanities", listOf("Bengali")),
        DepartmentItem("History", "HIST", "Faculty of Humanities", listOf("Hist")),

        // Social Sciences
        DepartmentItem("Sociology", "SOC", "Faculty of Social Science", listOf("Social")),
        DepartmentItem("Public Administration", "PAD", "Faculty of Social Science", listOf("PubAdmin", "Administration")),
        DepartmentItem("International Relations", "IR", "Faculty of Social Science", listOf("Relations")),
        DepartmentItem("Economics", "ECON", "Faculty of Social Science", listOf("Econ")),
        DepartmentItem("Political Science", "POL", "Faculty of Social Science", listOf("PoliSci", "Politics")),

        // Business Studies
        DepartmentItem("Management Studies", "MS", "Faculty of Business Studies", listOf("Management", "BBA")),
        DepartmentItem("Accounting and Information Systems", "AIS", "Faculty of Business Studies", listOf("Accounting", "Finance")),
        DepartmentItem("Marketing", "MKT", "Faculty of Business Studies", listOf("Market")),
        DepartmentItem("Finance and Banking", "FB", "Faculty of Business Studies", listOf("Finance", "Banking")),
        DepartmentItem("Tourism and Hospitality Management", "THM", "Faculty of Business Studies", listOf("Tourism", "Hospitality")),

        // Law
        DepartmentItem("Law", "LAW", "Faculty of Law", listOf("LL.B", "LLB")),

        // Agriculture & Veterinary
        DepartmentItem("Agriculture", "AG", "Faculty of Agriculture", listOf("Agri")),
        DepartmentItem("Fisheries and Marine Bioscience", "FMB", "Faculty of Fisheries", listOf("Fisheries", "Marine")),
        DepartmentItem("Animal Science and Veterinary Medicine", "ASVM", "Faculty of Veterinary Medicine", listOf("Vet", "DVM", "Animal"))
    )

    /**
     * Case-insensitive smart search covering acronyms, full names, aliases, and faculties.
     */
    fun search(query: String): List<DepartmentItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return ITEMS

        val startsWithAcronym = mutableListOf<DepartmentItem>()
        val startsWithName = mutableListOf<DepartmentItem>()
        val containsInName = mutableListOf<DepartmentItem>()
        val matchesAliasOrFaculty = mutableListOf<DepartmentItem>()

        for (item in ITEMS) {
            val nameLower = item.name.lowercase(Locale.ROOT)
            val acronymLower = item.acronym.lowercase(Locale.ROOT)
            val facultyLower = item.faculty.lowercase(Locale.ROOT)
            val aliasesLower = item.aliases.map { it.lowercase(Locale.ROOT) }

            when {
                acronymLower == q || acronymLower.startsWith(q) -> startsWithAcronym.add(item)
                nameLower.startsWith(q) -> startsWithName.add(item)
                nameLower.contains(q) -> containsInName.add(item)
                aliasesLower.any { it.contains(q) } || facultyLower.contains(q) -> matchesAliasOrFaculty.add(item)
            }
        }

        return (startsWithAcronym + startsWithName + containsInName + matchesAliasOrFaculty).distinctBy { it.name }
    }

    /**
     * Attaches the searchable bottom-sheet department picker to an EditText / View.
     */
    fun attachPicker(
        view: View,
        fragmentManager: FragmentManager,
        title: String = "Select Department",
        onSelected: (String) -> Unit
    ) {
        if (view is EditText) {
            view.isFocusable = false
            view.isFocusableInTouchMode = false
            view.isCursorVisible = false
        }
        view.isClickable = true
        view.setOnClickListener {
            val current = (view as? EditText)?.text?.toString().orEmpty()
            DepartmentPickerBottomSheet.show(
                fragmentManager = fragmentManager,
                title = title,
                selectedDepartment = current,
                onSelected = { chosen ->
                    if (view is EditText) {
                        view.setText(chosen)
                        view.error = null
                    }
                    onSelected(chosen)
                }
            )
        }
    }
}
