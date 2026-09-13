const firebase = require("@firebase/rules-unit-testing");
const fs = require("fs");

async function run() {
  const projectId = "classmate-test";
  const rules = fs.readFileSync("firestore.rules", "utf8");
  
  const testEnv = await firebase.initializeTestEnvironment({
    projectId: projectId,
    firestore: { rules: rules }
  });

  // Admin user
  const adminDb = testEnv.authenticatedContext("admin_uid").firestore();
  
  // Set up superadmin user doc
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("admin_uid").set({
      uid: "admin_uid",
      role: "teacher",
      permissions: { canManageUsers: true }
    });
    
    // Set up dummy user to delete
    await db.collection("users").doc("dummy_uid").set({
      uid: "dummy_uid",
      role: "student"
    });
  });

  try {
    await adminDb.collection("users").doc("dummy_uid").delete();
    console.log("Delete SUCCEEDED");
  } catch (e) {
    console.log("Delete FAILED: " + e);
  }
  
  await testEnv.cleanup();
}
run();
