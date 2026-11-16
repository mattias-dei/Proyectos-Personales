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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSettingsPayload(uid, serverTimestamp) {
  return {
    userId: uid,
    tempoBpm: 120,
    volumePercent: 80,
    lightsEnabled: true,
    lightsPattern: "twinkle",
    festiveSoundsEnabled: true,
    darkWinterMode: true,
    autoOffMinutes: 30,
    notifEnabled: true,
    notifTitle: "Hora de villancicos",
    notifMessage: "Enciende tu ESP32 navideño",
    notifHour: 20,
    notifMinute: 0,
    preferredDeviceName: "ESP32_Navidad",
    preferredDeviceAddress: "AA:BB:CC:DD:EE:FF",
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

function validMelodyPayload(uid, serverTimestamp) {
  return {
    userId: uid,
    title: "Noche de Paz Custom",
    artistOrSource: "Archivo RTTTL",
    defaultBpm: 110,
    frequencies: [392, 440, 392, 330],
    durationsMs: [400, 200, 400, 600],
    sourceFileName: "noche_paz.rtttl",
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

function validPlaylistPayload(uid, serverTimestamp) {
  return {
    userId: uid,
    title: "Clásicos Navideños",
    platform: "spotify",
    url: "https://open.spotify.com/playlist/37i9dQZF1DX0Yxoavh5qJV",
    description: "Villancicos acogedores para noches de invierno",
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("melodies").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
});

test("Owner can create, update, and read their own settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("default");
  await assertSucceeds(docRef.set(validSettingsPayload(ALICE_UID, ts)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(docRef.update({ tempoBpm: 140, updatedAt: new Date() }));
});

test("Cross-user access to settings, melodies, and playlists is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("default")
      .set(validSettingsPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("melodies").doc("mel_1")
      .set(validMelodyPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1")
      .set(validPlaylistPayload(ALICE_UID, ts))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("settings").doc("default").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("melodies").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").delete());
});

test("Shadow update with ghost field and invalid BPM or mismatched arrays are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();

  const shadowSettings = { ...validSettingsPayload(ALICE_UID, ts), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("default").set(shadowSettings)
  );

  const badTempo = { ...validSettingsPayload(ALICE_UID, ts), tempoBpm: 999 };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("default").set(badTempo)
  );

  const mismatchedNotes = { ...validMelodyPayload(ALICE_UID, ts), durationsMs: [400] };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("melodies").doc("mel_bad").set(mismatchedNotes)
  );

  const badUrl = { ...validPlaylistPayload(ALICE_UID, ts), url: "javascript:alert(1)" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_bad").set(badUrl)
  );
});

test("Owner can list, create, update, and delete melodies and playlists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();

  const melRef = aliceDb.collection("users").doc(ALICE_UID).collection("melodies").doc("mel_ok");
  await assertSucceeds(melRef.set(validMelodyPayload(ALICE_UID, ts)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("melodies").get());
  await assertSucceeds(melRef.update({ title: "Noche de Paz v2", updatedAt: new Date() }));
  await assertSucceeds(melRef.delete());

  const plRef = aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_ok");
  await assertSucceeds(plRef.set(validPlaylistPayload(ALICE_UID, ts)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertSucceeds(plRef.update({ title: "Navidad Chill", updatedAt: new Date() }));
  await assertSucceeds(plRef.delete());
});
