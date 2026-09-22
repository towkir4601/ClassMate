import re

# Update QuestionPaper.kt model
with open('app/src/main/java/com/shuaib/classmate/models/QuestionPaper.kt', 'r') as f:
    model_content = f.read()

if 'val batch: String = ""' not in model_content:
    model_content = model_content.replace('val isDeleted: Boolean = false', 'val isDeleted: Boolean = false,\n    val batch: String = ""')
    with open('app/src/main/java/com/shuaib/classmate/models/QuestionPaper.kt', 'w') as f:
        f.write(model_content)

# Update QuestionBankFragment.kt
with open('app/src/main/java/com/shuaib/classmate/fragments/QuestionBankFragment.kt', 'r') as f:
    qb_content = f.read()

if 'private var currentUserBatch = ""' not in qb_content:
    qb_content = qb_content.replace('private var isAdmin = false', 'private var isAdmin = false\n    private var currentUserBatch = ""')

old_auth = '''                    val role = doc.getString("role") ?: "student"
                    val permissions = doc.get("permissions") as? Map<String, Boolean> ?: com.shuaib.classmate.models.User.DEFAULT_PERMISSIONS
                    isAdmin = (role == "superadmin" || role == "admin" || permissions["canUploadPDF"] == true || permissions["canUploadLibrary"] == true)
                    binding.btnUploadQuestion.isVisible = isAdmin'''

new_auth = '''                    val role = doc.getString("role") ?: "student"
                    currentUserBatch = doc.getString("batch") ?: ""
                    val permissions = doc.get("permissions") as? Map<String, Boolean> ?: com.shuaib.classmate.models.User.DEFAULT_PERMISSIONS
                    isAdmin = (role == "superadmin" || role == "admin")
                    val canUpload = isAdmin || permissions["canUploadPDF"] == true || permissions["canUploadLibrary"] == true
                    binding.btnUploadQuestion.isVisible = canUpload'''
qb_content = qb_content.replace(old_auth, new_auth)

old_map = '''                            downloadCount = doc.getLong("downloadCount") ?: 0L,
                            timestamp = doc.getTimestamp("timestamp"),
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )'''

new_map = '''                            downloadCount = doc.getLong("downloadCount") ?: 0L,
                            timestamp = doc.getTimestamp("timestamp"),
                            isDeleted = doc.getBoolean("isDeleted") ?: false,
                            batch = doc.getString("batch") ?: ""
                        )'''
qb_content = qb_content.replace(old_map, new_map)

old_sort = '''                allPapers = snapshot.documents.mapNotNull { doc ->'''
new_sort = '''                allPapers = snapshot.documents.mapNotNull { doc ->'''
# We need to filter the papers! Wait, mapNotNull handles it, let's just append the filter after it.
old_full = '''                    } catch (e: Exception) {
                        null
                    }
                }
                
                binding.shimmerView.stopShimmer()'''
new_full = '''                    } catch (e: Exception) {
                        null
                    }
                }.filter { isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch }
                
                binding.shimmerView.stopShimmer()'''

qb_content = qb_content.replace(old_full, new_full)

with open('app/src/main/java/com/shuaib/classmate/fragments/QuestionBankFragment.kt', 'w') as f:
    f.write(qb_content)
print("Patched QuestionBankFragment")
