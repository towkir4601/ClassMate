# Add a concurrent classes container to the hero card XML
with open('app/src/main/res/layout/fragment_timetable.xml', 'r') as f:
    xml = f.read()

# Add the concurrent classes container after the progress bar, before the collapsible details
old_section = '''                    <!-- Collapsible Details Container -->
                    <LinearLayout
                        android:id="@+id/heroDetailsContainer"'''

new_section = '''                    <!-- Concurrent Classes Summary -->
                    <LinearLayout
                        android:id="@+id/concurrentClassesContainer"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:layout_marginTop="12dp"
                        android:visibility="gone">

                        <View
                            android:layout_width="match_parent"
                            android:layout_height="1dp"
                            android:background="#20FFFFFF"
                            android:layout_marginBottom="10dp" />

                        <TextView
                            android:id="@+id/tvConcurrentLabel"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Also happening now:"
                            android:textColor="#99FFFFFF"
                            android:textSize="11sp"
                            android:textStyle="bold"
                            android:letterSpacing="0.06"
                            android:layout_marginBottom="8dp" />

                        <LinearLayout
                            android:id="@+id/concurrentClassesList"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical" />

                    </LinearLayout>

                    <!-- Collapsible Details Container -->
                    <LinearLayout
                        android:id="@+id/heroDetailsContainer"'''

xml = xml.replace(old_section, new_section)

with open('app/src/main/res/layout/fragment_timetable.xml', 'w') as f:
    f.write(xml)
print("Patched XML with concurrent classes container")
