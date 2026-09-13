import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

# Add previous variables to track state
bad_class = """    private var favoriteSubjects = emptyList<Subject>()
    private var favoritePdfIdsSet = emptySet<String>()
    private var pdfCounts = emptyMap<String, Int>()"""
good_class = """    private var favoriteSubjects = emptyList<Subject>()
    private var favoritePdfIdsSet = emptySet<String>()
    private var pdfCounts = emptyMap<String, Int>()
    private var lastFetchedSubjects = emptyList<String>()
    private var lastFetchedPdfIds = emptyList<String>()"""
content = content.replace(bad_class, good_class)

# Update fetchSavedResources to skip redundant fetches
bad_fetch_saved = """    private fun fetchSavedResources(favoriteNames: List<String>, favoritePdfIds: List<String>) {
        favoritePdfIdsSet = favoritePdfIds.toSet()

        val hasSubjects = favoriteNames.isNotEmpty()
        val hasPdfs = favoritePdfIds.isNotEmpty()"""
good_fetch_saved = """    private fun fetchSavedResources(favoriteNames: List<String>, favoritePdfIds: List<String>) {
        favoritePdfIdsSet = favoritePdfIds.toSet()

        val hasSubjects = favoriteNames.isNotEmpty()
        val hasPdfs = favoritePdfIds.isNotEmpty()
        
        val subjectsChanged = favoriteNames != lastFetchedSubjects
        val pdfsChanged = favoritePdfIds != lastFetchedPdfIds
        
        lastFetchedSubjects = favoriteNames
        lastFetchedPdfIds = favoritePdfIds"""
content = content.replace(bad_fetch_saved, good_fetch_saved)

bad_fetch_subs = """        // Fetch Subjects
        if (hasSubjects) {
            favoriteSubjects = SubjectList.subjects.filter { it.name in favoriteNames }
            if (favoriteSubjects.isNotEmpty() && !hasPdfs) {
                updateSubjectCountsOnly()
            }
        }

        // Fetch PDFs
        if (hasPdfs) {
            fetchFavoritePdfs(favoritePdfIds)
        }"""
good_fetch_subs = """        // Fetch Subjects
        if (hasSubjects) {
            favoriteSubjects = SubjectList.subjects.filter { it.name in favoriteNames }
            if (favoriteSubjects.isNotEmpty() && !hasPdfs && subjectsChanged) {
                updateSubjectCountsOnly()
            } else if (!subjectsChanged && !pdfsChanged) {
                subjectAdapter.updateList(favoriteSubjects, pdfCounts)
            }
        }

        // Fetch PDFs
        if (hasPdfs && (pdfsChanged || subjectsChanged)) {
            fetchFavoritePdfs(favoritePdfIds)
        }"""
content = content.replace(bad_fetch_subs, good_fetch_subs)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
