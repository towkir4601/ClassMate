import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

bad = """        binding.cardSeeFriends.applyClickAnimation {
            (activity as? MainActivity)?.openChildDestination(R.id.nav_profile, R.id.nav_friends)
        }"""
good = """        binding.cardTeacherDirectory.applyClickAnimation {
            val args = android.os.Bundle().apply { putBoolean("isTeacherMode", true) }
            (activity as? MainActivity)?.openChildDestination(R.id.nav_profile, R.id.nav_friends, args)
        }
        
        binding.cardSeeFriends.applyClickAnimation {
            val args = android.os.Bundle().apply { putBoolean("isTeacherMode", false) }
            (activity as? MainActivity)?.openChildDestination(R.id.nav_profile, R.id.nav_friends, args)
        }"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
