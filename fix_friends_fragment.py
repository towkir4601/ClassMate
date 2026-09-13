import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad = """        } else {
            binding.tvHeaderTitle.text = "Student Directory"
        }
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
        }"""
good = """        } else {
            binding.tvHeaderTitle.text = "Student Directory"
        }"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
