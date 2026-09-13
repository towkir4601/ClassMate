import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_lib_base = """        "timestamp",
        "downloadCount",
        "isDeleted"
      ])
    }"""

good_lib_base = """        "timestamp",
        "downloadCount",
        "isDeleted",
        "batch"
      ])
    }"""

bad_qb_base = """        "timestamp",
        "downloadCount",
        "isDeleted"
      ])
    }"""

good_qb_base = """        "timestamp",
        "downloadCount",
        "isDeleted",
        "batch"
      ])
    }"""

content = content.replace(bad_lib_base, good_lib_base)
content = content.replace(bad_qb_base, good_qb_base)

with open('firestore.rules', 'w') as f:
    f.write(content)
