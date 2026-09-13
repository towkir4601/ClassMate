import re

with open('app/src/main/res/layout/dialog_add_period.xml', 'r') as f:
    content = f.read()

bad = """    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Room Number (Optional)"
        android:textColorHint="@color/cm_text_secondary"
        app:boxStrokeColor="@color/cm_primary"
        app:hintTextColor="@color/cm_primary"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etRoom"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:textColor="@color/cm_text_primary" />
    </com.google.android.material.textfield.TextInputLayout>"""
good = """    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Room Number (Optional)"
        android:textColorHint="@color/cm_text_secondary"
        app:boxStrokeColor="@color/cm_primary"
        app:hintTextColor="@color/cm_primary"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etRoom"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:textColor="@color/cm_text_primary" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Target Batch"
        android:textColorHint="@color/cm_text_secondary"
        app:boxStrokeColor="@color/cm_primary"
        app:hintTextColor="@color/cm_primary"
        android:layout_marginBottom="16dp">
        <AutoCompleteTextView
            android:id="@+id/etBatch"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="none"
            android:textColor="@color/cm_text_primary" />
    </com.google.android.material.textfield.TextInputLayout>"""
content = content.replace(bad, good)
with open('app/src/main/res/layout/dialog_add_period.xml', 'w') as f:
    f.write(content)
