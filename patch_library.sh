#!/bin/bash

# Update SubjectList logic in setupSections
sed -i '' 's/labSubjects = allSubjects.filter { it.name.trim().lowercase().endsWith("lab") }/labSubjects = allSubjects.filter { it.type == "lab" }/g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt
sed -i '' 's/otherSubjects = allSubjects.filter {/otherSubjects = allSubjects.filter { it.type == "other" }/g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt
sed -i '' 's/val name = it.name.trim().lowercase()//g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt
sed -i '' 's/name.contains("other") || name.contains("viva")//g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt
sed -i '' 's/regularSubjects = allSubjects.filter { subject ->/regularSubjects = allSubjects.filter { it.type == "regular" }/g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt
sed -i '' 's/subject !in labSubjects && subject !in otherSubjects//g' app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt

