import re

with open('app/src/main/res/layout/activity_user_management.xml', 'r') as f:
    content = f.read()

bad = """                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:paddingBottom="24dp">"""
good = """                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:paddingBottom="24dp">
                    
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/tilBatchFilter"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginHorizontal="16dp"
                        android:layout_marginTop="16dp"
                        android:hint="Filter by Batch"
                        app:boxStrokeColor="@color/cm_primary"
                        app:hintTextColor="@color/cm_primary">
                        <AutoCompleteTextView
                            android:id="@+id/dropdownBatchFilter"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="none"
                            android:textColor="@color/cm_text_primary" />
                    </com.google.android.material.textfield.TextInputLayout>"""
content = content.replace(bad, good)

with open('app/src/main/res/layout/activity_user_management.xml', 'w') as f:
    f.write(content)
