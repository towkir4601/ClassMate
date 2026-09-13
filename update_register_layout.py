import re

with open('app/src/main/res/layout/activity_register.xml', 'r') as f:
    content = f.read()

# Add RadioGroup for Student/Teacher at the top
radio_group = """
            <RadioGroup
                android:id="@+id/rgRole"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="16dp"
                android:gravity="center">
                
                <RadioButton
                    android:id="@+id/rbStudent"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Student"
                    android:checked="true"
                    android:textColor="@color/cm_text_primary" />
                    
                <RadioButton
                    android:id="@+id/rbTeacher"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="24dp"
                    android:text="Teacher"
                    android:textColor="@color/cm_text_primary" />
            </RadioGroup>
"""

# Insert RadioGroup before tvStepTitle
content = content.replace('<TextView\n                android:id="@+id/tvStepTitle"', radio_group + '\n            <TextView\n                android:id="@+id/tvStepTitle"')

# Generate new Input fields
def make_input(id_name, hint, input_type="text"):
    return f"""
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/til{id_name}"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="{hint}"
                    app:boxCornerRadiusBottomEnd="28dp"
                    app:boxCornerRadiusBottomStart="28dp"
                    app:boxCornerRadiusTopEnd="28dp"
                    app:boxCornerRadiusTopStart="28dp"
                    app:boxStrokeColor="@color/cm_border"
                    app:boxStrokeWidth="1dp"
                    app:boxStrokeWidthFocused="1.5dp"
                    app:hintTextColor="@color/cm_text_disabled">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et{id_name}"
                        android:layout_width="match_parent"
                        android:layout_height="58dp"
                        android:gravity="center_vertical"
                        android:imeOptions="actionNext"
                        android:inputType="{input_type}"
                        android:singleLine="true"
                        android:textColor="@color/cm_text_primary"
                        android:textSize="16sp" />
                </com.google.android.material.textfield.TextInputLayout>
"""

new_fields = (
    make_input("Department", "Department (e.g. CSE)") +
    make_input("WhatsApp", "WhatsApp Number", "phone") +
    make_input("Phone", "Phone Number", "phone") +
    make_input("Batch", "Batch (e.g. 52nd)") +
    make_input("FatherName", "Father's Name (Optional)") +
    make_input("MotherName", "Mother's Name (Optional)") +
    make_input("PresentAddress", "Present Address (Optional)") +
    make_input("PermanentAddress", "Permanent Address (Optional)") +
    make_input("District", "Home District (Optional)") +
    make_input("BloodGroup", "Blood Group (Optional)")
)

# Insert new fields after tilStudentId in stepProfile
target = "</com.google.android.material.textfield.TextInputLayout>"
pos = content.find(target, content.find('android:id="@+id/tilStudentId"'))
if pos != -1:
    pos += len(target)
    content = content[:pos] + new_fields + content[pos:]

with open('app/src/main/res/layout/activity_register.xml', 'w') as f:
    f.write(content)

print("Updated activity_register.xml")
