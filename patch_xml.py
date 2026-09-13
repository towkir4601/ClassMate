with open("app/src/main/res/layout/activity_edit_profile.xml", "r") as f:
    content = f.read()

old_block = """            </com.google.android.material.textfield.TextInputLayout>


            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:hint="Phone Number\""""

new_block = """            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:hint="Department (e.g. CSE)"
                app:boxBackgroundColor="@color/cm_surface"
                app:boxCornerRadiusBottomEnd="16dp"
                app:boxCornerRadiusBottomStart="16dp"
                app:boxCornerRadiusTopEnd="16dp"
                app:boxCornerRadiusTopStart="16dp"
                app:boxStrokeColor="@color/cm_border"
                app:hintTextColor="@color/cm_primary">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etDepartment"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="text"
                    android:textColor="@color/cm_text_primary" />
            </com.google.android.material.textfield.TextInputLayout>


            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:hint="Phone Number\""""

content = content.replace(old_block, new_block)

with open("app/src/main/res/layout/activity_edit_profile.xml", "w") as f:
    f.write(content)
