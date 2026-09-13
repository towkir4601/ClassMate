import re

with open('app/src/main/res/layout/activity_create_poll.xml', 'r') as f:
    content = f.read()

bad = """            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>
    </ScrollView>"""

good = """            </com.google.android.material.card.MaterialCardView>

            <!-- Target Batch Picker -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="8dp"
                android:text="Target Audience"
                android:textColor="@color/cm_text_secondary"
                android:textSize="14sp" />

            <com.google.android.material.button.MaterialButtonToggleGroup
                android:id="@+id/toggleTarget"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:singleSelection="true"
                app:checkedButton="@+id/btnTargetAll">

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnTargetAll"
                    style="?attr/materialButtonOutlinedStyle"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Public (All)"
                    android:textColor="@color/cm_text_primary" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnTargetBatch"
                    style="?attr/materialButtonOutlinedStyle"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="My Batch"
                    android:textColor="@color/cm_text_primary" />
            </com.google.android.material.button.MaterialButtonToggleGroup>

        </LinearLayout>
    </ScrollView>"""

content = content.replace(bad, good)

with open('app/src/main/res/layout/activity_create_poll.xml', 'w') as f:
    f.write(content)
