import re

with open('app/src/main/res/layout/activity_admin_panel.xml', 'r') as f:
    content = f.read()

# Add cardPendingApprovals before cardManageUsers
bad = """            <LinearLayout
                android:id="@+id/cardManageUsers\""""
good = """            <LinearLayout
                android:id="@+id/cardPendingApprovals"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:background="@drawable/bg_card_primary"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="16dp"
                android:visibility="gone">

                <FrameLayout
                    android:layout_width="50dp"
                    android:layout_height="50dp"
                    android:background="@drawable/bg_icon_container">
                    <ImageView
                        android:layout_width="24dp"
                        android:layout_height="24dp"
                        android:layout_gravity="center"
                        android:src="@drawable/ic_chevron_right"
                        android:tint="@color/cm_on_primary" />
                </FrameLayout>

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="16dp"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Pending Approvals"
                        android:textColor="@color/cm_text_primary"
                        android:textSize="16sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Approve new student/teacher registrations"
                        android:textColor="@color/cm_text_secondary"
                        android:textSize="12sp" />
                </LinearLayout>

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_chevron_right"
                    android:tint="@color/cm_primary" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/cardManageUsers\""""
content = content.replace(bad, good)

with open('app/src/main/res/layout/activity_admin_panel.xml', 'w') as f:
    f.write(content)
