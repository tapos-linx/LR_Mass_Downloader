const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const DATABASE_ID = "ai-studio-android-lrmassdo-f22660f7-854b-46c2-9414-df82c9c7b5bc";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- User Profile Tests ---

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@test.com",
    displayName: "Alice",
    createdAt: new Date(),
    lastSignInAt: new Date()
  }));
});

test("Authenticated user: cannot create another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").doc(BOB_UID).set({
    userId: BOB_UID,
    email: "bob@test.com",
    createdAt: new Date(),
    lastSignInAt: new Date()
  }));
});

// --- Saved Records Tests ---

test("Authenticated user: can create and read their own saved records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const recordRef = aliceDb.collection("users").doc(ALICE_UID).collection("saved_records").doc("record_1");
  
  await assertSucceeds(recordRef.set({
    userId: ALICE_UID,
    mouzaId: "mouza_1",
    mouzaNameEn: "Mouza One",
    downloadedAt: new Date(),
    khatianCount: 10,
    recordType: "RS",
    downloadFormat: "PDF"
  }));
  
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("saved_records").get());
});

test("Authenticated user: cannot read another user's saved records", async () => {
  // Seed data for Bob
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore(DATABASE_ID).collection("users").doc(BOB_UID).collection("saved_records").doc("bob_record").set({
      userId: BOB_UID,
      mouzaId: "mouza_2",
      mouzaNameEn: "Bob Mouza",
      downloadedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("saved_records").doc("bob_record").get());
});

// --- Download Tasks Tests ---

test("Authenticated user: can manage their own download tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("download_tasks").doc("task_1");
  
  await assertSucceeds(taskRef.set({
    userId: ALICE_UID,
    mouzaId: "mouza_1",
    status: "PENDING",
    timestamp: new Date(),
    progress: 0,
    downloadFormat: "PDF"
  }));
  
  await assertSucceeds(taskRef.update({
    status: "DOWNLOADING",
    progress: 50
  }));
  
  await assertSucceeds(taskRef.delete());
});
