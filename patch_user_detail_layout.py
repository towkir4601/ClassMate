import re

with open('app/src/main/res/layout/activity_user_detail.xml', 'r') as f:
    content = f.read()

bad = """                <!-- Address -->
                <TextView
                    android:id="@+id/tvAddress"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="24dp"
                    android:textColor="@color/cm_text_primary"
                    android:textSize="16sp" />"""
good = """                <!-- Address -->
                <TextView
                    android:id="@+id/tvAddress"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="16dp"
                    android:textColor="@color/cm_text_primary"
                    android:textSize="16sp" />
                    
                <!-- Private Data (Super Admin Only) -->
                <LinearLayout
                    android:id="@+id/layoutPrivateData"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="gone">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Private Documents (Super Admin View)"
                        android:textColor="@color/cm_accent"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp"/>
                    <TextView
                        android:id="@+id/tvFatherName"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="8dp"
                        android:textColor="@color/cm_text_primary"
                        android:textSize="16sp" />
                    <TextView
                        android:id="@+id/tvMotherName"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="24dp"
                        android:textColor="@color/cm_text_primary"
                        android:textSize="16sp" />
                </LinearLayout>"""
content = content.replace(bad, good)

with open('app/src/main/res/layout/activity_user_detail.xml', 'w') as f:
    f.write(content)
