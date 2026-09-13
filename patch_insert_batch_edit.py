import re

with open('app/src/main/res/layout/activity_edit_profile.xml', 'r') as f:
    content = f.read()

good = """                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Batch (e.g. 14)"
                    android:layout_marginBottom="12dp"
                    app:boxCornerRadiusBottomEnd="16dp"
                    app:boxCornerRadiusBottomStart="16dp"
                    app:boxCornerRadiusTopEnd="16dp"
                    app:boxCornerRadiusTopStart="16dp"
                    app:boxStrokeColor="@color/cm_border"
                    app:hintTextColor="@color/cm_primary">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etBatch"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="number"
                        android:textColor="@color/cm_text_primary" />
                </com.google.android.material.textfield.TextInputLayout>

                <!-- Phone -->"""

content = content.replace("""                <!-- Phone -->""", good)

with open('app/src/main/res/layout/activity_edit_profile.xml', 'w') as f:
    f.write(content)
