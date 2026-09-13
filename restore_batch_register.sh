sed -i '' '/<com.google.android.material.textfield.TextInputLayout.*id="@+id\/tilDepartment"/i\
\
                <com.google.android.material.textfield.TextInputLayout\
                    android:id="@+id/tilBatch"\
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"\
                    android:layout_width="match_parent"\
                    android:layout_height="wrap_content"\
                    android:layout_marginTop="12dp"\
                    android:hint="Batch (e.g. 52)"\
                    app:boxCornerRadiusBottomEnd="28dp"\
                    app:boxCornerRadiusBottomStart="28dp"\
                    app:boxCornerRadiusTopEnd="28dp"\
                    app:boxCornerRadiusTopStart="28dp"\
                    app:boxStrokeColor="@color/cm_border"\
                    app:boxStrokeWidth="1dp"\
                    app:boxStrokeWidthFocused="1.5dp"\
                    app:hintTextColor="@color/cm_text_disabled">\
\
                    <com.google.android.material.textfield.TextInputEditText\
                        android:id="@+id/etBatch"\
                        android:layout_width="match_parent"\
                        android:layout_height="58dp"\
                        android:gravity="center_vertical"\
                        android:imeOptions="actionNext"\
                        android:inputType="number"\
                        android:textColor="@color/cm_text_primary"\
                        android:textSize="16sp" />\
                </com.google.android.material.textfield.TextInputLayout>\
' app/src/main/res/layout/activity_register.xml
