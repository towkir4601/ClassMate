import re

with open('app/src/main/res/layout/dialog_add_period.xml', 'r') as f:
    content = f.read()

# Replace the opening LinearLayout with ScrollView and a nested LinearLayout
bad_start = """<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="@color/cm_surface">"""
good_start = """<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/cm_surface">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">"""
content = content.replace(bad_start, good_start)

# Replace the closing LinearLayout with LinearLayout and ScrollView
bad_end = """    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/cbTemporary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="This is a temporary class for today only"
        android:textColor="@color/cm_text_primary" />
</LinearLayout>"""
good_end = """    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/cbTemporary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="This is a temporary class for today only"
        android:textColor="@color/cm_text_primary" />
    </LinearLayout>
</ScrollView>"""
content = content.replace(bad_end, good_end)

with open('app/src/main/res/layout/dialog_add_period.xml', 'w') as f:
    f.write(content)
