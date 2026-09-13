import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt', 'r') as f:
    content = f.read()

bad = """                address = doc.getString("address") ?: "",
                role = doc.getString("role") ?: "student",
                approved = doc.getBoolean("approved") ?: false,"""
good = """                address = doc.getString("address") ?: "",
                fatherName = doc.getString("fatherName") ?: "",
                motherName = doc.getString("motherName") ?: "",
                role = doc.getString("role") ?: "student",
                approved = doc.getBoolean("approved") ?: false,"""
content = content.replace(bad, good)

bad_bind = """        binding.tvDepartment.text = field("Department", targetUser.department)
        binding.tvPhone.text = field("Phone", targetUser.phone)
        binding.tvBloodGroup.text = field("Blood Group", targetUser.bloodGroup)
        binding.tvDistrict.text = field("Home District", targetUser.homeDistrict)
        binding.tvAddress.text = field("Address", targetUser.address)
    }"""
good_bind = """        binding.tvDepartment.text = field("Department", targetUser.department)
        binding.tvPhone.text = field("Phone", targetUser.phone)
        binding.tvBloodGroup.text = field("Blood Group", targetUser.bloodGroup)
        binding.tvDistrict.text = field("Home District", targetUser.homeDistrict)
        binding.tvAddress.text = field("Address", targetUser.address)
        
        if (currentUser.role == "superadmin") {
            binding.layoutPrivateData.visibility = android.view.View.VISIBLE
            binding.tvFatherName.text = field("Father's Name", targetUser.fatherName)
            binding.tvMotherName.text = field("Mother's Name", targetUser.motherName)
        } else {
            binding.layoutPrivateData.visibility = android.view.View.GONE
        }
    }"""
content = content.replace(bad_bind, good_bind)

with open('app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt', 'w') as f:
    f.write(content)
