import re

with open('app/src/main/res/layout/fragment_profile.xml', 'r') as f:
    content = f.read()

bad = """                <!-- Edit Icon Indicator -->
                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:layout_marginTop="16dp"
                    android:layout_marginEnd="16dp"
                    android:src="@drawable/ic_chevron_right"
                    app:tint="@color/cm_text_disabled"
                    app:layout_constraintTop_toTopOf="parent"
                    app:layout_constraintEnd_toEndOf="parent"
                    android:importantForAccessibility="no" />"""
good = ""
content = content.replace(bad, good)
with open('app/src/main/res/layout/fragment_profile.xml', 'w') as f:
    f.write(content)
