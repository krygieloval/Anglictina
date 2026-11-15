const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validPackPayload(overrides = {}) {
  const now = new Date();
  return {
    title: "Zvířátka",
    description: "Základní anglická zvířátka",
    emoji: "🐾",
    colorIndex: 0,
    enabledExercises: ["MATCHING", "FILL_BLANK", "PRONUNCIATION", "SHOOTER"],
    wordCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validWordPayload(packId, overrides = {}) {
  const now = new Date();
  return {
    packId,
    englishWord: "Cat",
    czechTranslation: "Kočka",
    exampleSentence: "The cat is sleeping.",
    imageBase64: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot list or create packs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("packs").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack1").set(validPackPayload())
  );
});

test("Owner can create, read, and update their own pack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const packRef = aliceDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals");
  await assertSucceeds(packRef.set(validPackPayload()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("packs").get());
  await assertSucceeds(packRef.update({ title: "Domácí zvířátka", updatedAt: new Date() }));
});

test("Cross-user access to packs and words is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const packRef = aliceDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals");
  await assertSucceeds(packRef.set(validPackPayload()));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("packs").get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("packs")
      .doc("pack_animals")
      .update({ title: "Hacked", updatedAt: new Date() })
  );
});

test("Shadow update with ghost field on pack is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const packRef = aliceDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals");
  await assertFails(packRef.set(validPackPayload({ isAdmin: true })));
});

test("Invalid exercise type in pack is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const packRef = aliceDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals");
  await assertFails(packRef.set(validPackPayload({ enabledExercises: ["INVALID_MODE"] })));
});

test("Owner can create word only under existing pack and cannot create orphan word", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const wordsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("packs")
    .doc("pack_animals")
    .collection("words");

  // Orphan word fails because parent pack doesn't exist
  await assertFails(wordsCol.doc("word1").set(validWordPayload("pack_animals")));

  // Create parent pack, then word succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("packs").doc("pack_animals").set(validPackPayload())
  );
  await assertSucceeds(wordsCol.doc("word1").set(validWordPayload("pack_animals")));
});

test("Word stats and exercise stats enforce bounds and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wordStats").doc("word1").set({
      packId: "pack_animals",
      englishWord: "Cat",
      czechTranslation: "Kočka",
      attempts: 3,
      correctCount: 2,
      streak: 1,
      lastPracticedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid: correctCount > attempts
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wordStats").doc("word2").set({
      packId: "pack_animals",
      englishWord: "Dog",
      czechTranslation: "Pes",
      attempts: 1,
      correctCount: 5,
      streak: 1,
      lastPracticedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Exercise stats valid
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("exerciseStats").doc("SHOOTER").set({
      exerciseType: "SHOOTER",
      attempts: 10,
      correctCount: 8,
      sessionsCompleted: 1,
      bestScore: 120,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid exerciseType doc id
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("exerciseStats").doc("HACKED").set({
      exerciseType: "HACKED",
      attempts: 1,
      correctCount: 1,
      sessionsCompleted: 1,
      bestScore: 10,
      createdAt: now,
      updatedAt: now,
    })
  );
});
