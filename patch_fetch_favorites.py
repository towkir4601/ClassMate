import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

old_str = """        }

        fetchUserFavorites()
    }"""

new_str = """        }

        currentUser?.let { fetchUserFavorites(it.favoriteSubjects, favoritePdfIdsSet.toList()) }
    }"""

content = content.replace(old_str, new_str)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
