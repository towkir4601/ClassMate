import re

with open('app/src/main/res/layout/fragment_profile.xml', 'r') as f:
    content = f.read()

bad = """                    android:text="See Friends"
                    android:textColor="@color/cm_text_primary"
                    android:textSize="16sp"
                    android:textStyle="bold" />"""
good = """                    android:text="Student Directory"
                    android:textColor="@color/cm_text_primary"
                    android:textSize="16sp"
                    android:textStyle="bold" />"""
content = content.replace(bad, good)
with open('app/src/main/res/layout/fragment_profile.xml', 'w') as f:
    f.write(content)
