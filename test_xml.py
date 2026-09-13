import xml.etree.ElementTree as ET

try:
    tree = ET.parse('app/src/main/res/layout/activity_post_notice.xml')
    print("XML is valid.")
except ET.ParseError as e:
    print(f"XML parse error: {e}")
