import re

with open('app/src/main/res/layout/activity_register.xml', 'r') as f:
    content = f.read()

bad = r"""                <com\.google\.android\.material\.textfield\.TextInputLayout
                    android:id="\@\+id/tilBatch".*?<\/com\.google\.android\.material\.textfield\.TextInputLayout>"""
content = re.sub(bad, "", content, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_register.xml', 'w') as f:
    f.write(content)
