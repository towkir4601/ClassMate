import re

with open('app/src/main/res/layout/activity_edit_profile.xml', 'r') as f:
    content = f.read()

bad = """            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <!-- Phone -->"""
good = """            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <!-- Name -->
                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Full Name"
                    android:layout_marginBottom="12dp">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etName"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textPersonName"
                        android:textColor="@color/cm_text_primary" />
                </com.google.android.material.textfield.TextInputLayout>

                <!-- Student ID -->
                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Student ID"
                    android:layout_marginBottom="12dp">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etStudentId"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="text"
                        android:textColor="@color/cm_text_primary" />
                </com.google.android.material.textfield.TextInputLayout>

                <!-- Phone -->"""
content = content.replace(bad, good)

with open('app/src/main/res/layout/activity_edit_profile.xml', 'w') as f:
    f.write(content)
