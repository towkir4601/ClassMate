import re

with open('app/src/main/res/layout/dialog_add_period.xml', 'r') as f:
    content = f.read()

checkbox = """
    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/cbTemporary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="This is a temporary class for today only"
        android:textColor="@color/cm_text_primary" />
</LinearLayout>
"""

content = content.replace("</LinearLayout>\n\n</LinearLayout>", "</LinearLayout>\n" + checkbox)

with open('app/src/main/res/layout/dialog_add_period.xml', 'w') as f:
    f.write(content)

print("Updated dialog_add_period.xml")
