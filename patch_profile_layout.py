import re

with open('app/src/main/res/layout/fragment_profile.xml', 'r') as f:
    content = f.read()

btn_edit = """        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnEditProfile"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:text="Edit Profile"
            android:textColor="@color/cm_on_primary"
            android:textSize="15sp"
            android:textStyle="bold"
            app:cornerRadius="16dp"
            app:backgroundTint="@color/cm_primary"
            android:layout_marginHorizontal="8dp"
            android:layout_marginBottom="12dp"/>
"""

content = content.replace("        <!-- 7. Logout Button", btn_edit + "\n        <!-- 7. Logout Button")

with open('app/src/main/res/layout/fragment_profile.xml', 'w') as f:
    f.write(content)

