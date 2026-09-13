import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

activity = """        <activity
            android:name=".activities.EditProfileActivity"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.ClassMate" />
            
        <activity
            android:name=".activities.PdfUploadActivity\""""
            
if ".activities.EditProfileActivity" not in content:
    content = content.replace('<activity\n            android:name=".activities.PdfUploadActivity"', activity)
    with open('app/src/main/AndroidManifest.xml', 'w') as f:
        f.write(content)
