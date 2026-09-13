sed -i '' '/android:orientation="vertical">/a\
\
            <!-- Name -->\
            <com.google.android.material.textfield.TextInputLayout\
                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"\
                android:layout_width="match_parent"\
                android:layout_height="wrap_content"\
                android:hint="Full Name"\
                android:layout_marginBottom="12dp"\
                app:boxCornerRadiusBottomEnd="16dp"\
                app:boxCornerRadiusBottomStart="16dp"\
                app:boxCornerRadiusTopEnd="16dp"\
                app:boxCornerRadiusTopStart="16dp"\
                app:boxStrokeColor="@color/cm_border"\
                app:hintTextColor="@color/cm_primary">\
                <com.google.android.material.textfield.TextInputEditText\
                    android:id="@+id/etName"\
                    android:layout_width="match_parent"\
                    android:layout_height="wrap_content"\
                    android:inputType="textPersonName"\
                    android:textColor="@color/cm_text_primary" />\
            </com.google.android.material.textfield.TextInputLayout>\
\
            <!-- Student ID -->\
            <com.google.android.material.textfield.TextInputLayout\
                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"\
                android:layout_width="match_parent"\
                android:layout_height="wrap_content"\
                android:hint="Student ID"\
                android:layout_marginBottom="12dp"\
                app:boxCornerRadiusBottomEnd="16dp"\
                app:boxCornerRadiusBottomStart="16dp"\
                app:boxCornerRadiusTopEnd="16dp"\
                app:boxCornerRadiusTopStart="16dp"\
                app:boxStrokeColor="@color/cm_border"\
                app:hintTextColor="@color/cm_primary">\
                <com.google.android.material.textfield.TextInputEditText\
                    android:id="@+id/etStudentId"\
                    android:layout_width="match_parent"\
                    android:layout_height="wrap_content"\
                    android:inputType="text"\
                    android:textColor="@color/cm_text_primary" />\
            </com.google.android.material.textfield.TextInputLayout>\
' app/src/main/res/layout/activity_edit_profile.xml
