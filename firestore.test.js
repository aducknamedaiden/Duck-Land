const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_pond_01";
const BOB_UID = "bob_pond_02";
const CHARLIE_UID = "charlie_pond_03";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPalette(overrides = {}) {
  return {
    bodyColor: "sunny_yellow",
    billColor: "coral_orange",
    hat: "sprout",
    eyewear: "shades",
    accessory: "bowtie",
    pondBg: "mint_mist",
    ...overrides,
  };
}

function ts(offsetMs = -1000) {
  return new Date(Date.now() + offsetMs);
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

test("Unauthenticated users are denied on all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("wallets").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("ducks").where("ownerId", "==", ALICE_UID).get()
  );
  await assertFails(
    unauth.collection("marketplace_listings").where("status", "==", "active").get()
  );
  await assertFails(
    unauth.collection("trade_offers").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauth.collection("weekly_events").where("weekNumber", ">=", 1).get()
  );
});

test("Collector profile & wallet initialization, isolation, and anti-tamper rules", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = ts(-2000);

  // Valid profile create (mirrors FirestoreCollectorRepository.ensureInitialized)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Quack",
      bio: "Collecting happy ducks",
      avatarConfig: validPalette(),
      duckCount: 0,
      tradesCompleted: 0,
      banned: false,
      banReason: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject ghost field, pre-banned creation, & list scraping for non-admin
  await assertFails(alice.collection("users").get());
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      avatarConfig: validPalette(),
      duckCount: 99, // must initialize at 0
      tradesCompleted: 0,
      banned: false,
      banReason: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Wallet must initialize at 250 coins and is strictly owner-only
  await assertSucceeds(
    alice.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bob.collection("wallets").doc(ALICE_UID).get());
  await assertFails(
    alice.collection("wallets").doc(ALICE_UID).update({
      coins: 99999,
      lastClaimType: "none",
      updatedAt: ts(-500),
    })
  );

  // Daily pond gift (+75 coins) succeeds
  await assertSucceeds(
    alice.collection("wallets").doc(ALICE_UID).update({
      coins: 325,
      lastClaimId: "",
      lastClaimType: "daily",
      lastDailyClaimAt: ts(-500),
      updatedAt: ts(-500),
    })
  );
});

test("Ducks creation, query alignment, listing, atomic marketplace buy, and seller coin claim", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = ts(-10000);

  // Seed Alice and Bob wallets
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("wallets").doc(BOB_UID).set({
      userId: BOB_UID,
      coins: 400,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Alice creates a duck (no `id` field inside map)
  await assertSucceeds(
    alice.collection("ducks").doc("duck_1").set({
      ownerId: ALICE_UID,
      speciesId: "sunny_classic",
      name: "Sunny Classic",
      rarity: "common",
      serialNumber: 101,
      palette: validPalette(),
      listed: false,
      listingId: "",
      lastTradeId: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unfiltered ducks list query fails; owner-scoped query succeeds
  await assertFails(alice.collection("ducks").get());
  await assertSucceeds(
    alice.collection("ducks").where("ownerId", "==", ALICE_UID).get()
  );

  // Alice lists duck_1 on marketplace_listings atomically
  const listBatch = alice.batch();
  listBatch.set(alice.collection("marketplace_listings").doc("list_1"), {
    sellerId: ALICE_UID,
    sellerName: "Alice Quack",
    sellerAvatar: validPalette(),
    buyerId: "",
    duckId: "duck_1",
    duckName: "Sunny Classic",
    duckSpeciesId: "sunny_classic",
    duckRarity: "common",
    duckSerialNumber: 101,
    duckPalette: validPalette(),
    askingCoins: 120,
    wantsTrade: true,
    desiredRarity: "rare",
    status: "active",
    createdAt: ts(-5000),
    updatedAt: ts(-5000),
  });
  listBatch.update(alice.collection("ducks").doc("duck_1"), {
    listed: true,
    listingId: "list_1",
    updatedAt: ts(-5000),
  });
  await assertSucceeds(listBatch.commit());

  // Bob buys list_1 atomically (listing -> sold, duck -> bob, bob wallet -> 280, claim created, notification created)
  const buyBatch = bob.batch();
  buyBatch.update(bob.collection("marketplace_listings").doc("list_1"), {
    status: "sold",
    buyerId: BOB_UID,
    updatedAt: ts(-1000),
  });
  buyBatch.update(bob.collection("ducks").doc("duck_1"), {
    ownerId: BOB_UID,
    listed: false,
    listingId: "",
    updatedAt: ts(-1000),
  });
  buyBatch.update(bob.collection("wallets").doc(BOB_UID), {
    coins: 280,
    lastClaimId: "",
    lastClaimType: "none",
    updatedAt: ts(-1000),
  });
  buyBatch.set(bob.collection("coin_claims").doc("sale_list_1"), {
    recipientId: ALICE_UID,
    sourceId: "list_1",
    sourceType: "listing_sale",
    amount: 120,
    claimed: false,
    createdAt: ts(-1000),
    updatedAt: ts(-1000),
  });
  buyBatch.set(
    bob.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_sale_1"),
    {
      userId: ALICE_UID,
      type: "listing_sold",
      title: "Sunny Classic sold!",
      body: "Bob adopted Sunny Classic for 120 Pond Coins.",
      referenceId: "list_1",
      read: false,
      createdAt: ts(-1000),
    }
  );
  await assertSucceeds(buyBatch.commit());

  // Alice claims her escrowed 120 coins
  const claimBatch = alice.batch();
  claimBatch.update(alice.collection("coin_claims").doc("sale_list_1"), {
    claimed: true,
    updatedAt: ts(-500),
  });
  claimBatch.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 370,
    lastClaimId: "sale_list_1",
    lastClaimType: "sale",
    updatedAt: ts(-500),
  });
  await assertSucceeds(claimBatch.commit());
});

test("Peer-to-peer atomic duck trade swap and participant privacy", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = ts(-10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("wallets").doc(BOB_UID).set({
      userId: BOB_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("ducks").doc("alice_duck").set({
      ownerId: ALICE_UID,
      speciesId: "mint_sprout",
      name: "Mint Sprout",
      rarity: "common",
      serialNumber: 204,
      palette: validPalette(),
      listed: false,
      listingId: "",
      lastTradeId: "",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("ducks").doc("bob_duck").set({
      ownerId: BOB_UID,
      speciesId: "sailor_quack",
      name: "Captain Splash",
      rarity: "rare",
      serialNumber: 308,
      palette: validPalette({ hat: "sailor" }),
      listed: false,
      listingId: "",
      lastTradeId: "",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Alice proposes trade + 50 bonus coins
  const offerBatch = alice.batch();
  offerBatch.set(alice.collection("trade_offers").doc("trade_1"), {
    senderId: ALICE_UID,
    senderName: "Alice Quack",
    recipientId: BOB_UID,
    recipientName: "Bob",
    participantIds: [ALICE_UID, BOB_UID],
    listingId: "",
    offeredDuckId: "alice_duck",
    offeredDuckName: "Mint Sprout",
    offeredDuckRarity: "common",
    offeredDuckPalette: validPalette(),
    requestedDuckId: "bob_duck",
    requestedDuckName: "Captain Splash",
    requestedDuckRarity: "rare",
    requestedDuckPalette: validPalette({ hat: "sailor" }),
    offeredCoins: 50,
    note: "Love that sailor hat!",
    status: "pending",
    createdAt: ts(-2000),
    updatedAt: ts(-2000),
  });
  offerBatch.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 200,
    lastClaimId: "",
    lastClaimType: "none",
    updatedAt: ts(-2000),
  });
  await assertSucceeds(offerBatch.commit());

  // Charlie cannot read Alice & Bob's private trade offer
  await assertFails(charlie.collection("trade_offers").doc("trade_1").get());

  // Bob accepts trade atomically (ducks swap owners + Bob gets +50 coins)
  const acceptBatch = bob.batch();
  acceptBatch.update(bob.collection("trade_offers").doc("trade_1"), {
    status: "accepted",
    updatedAt: ts(-500),
  });
  acceptBatch.update(bob.collection("ducks").doc("alice_duck"), {
    ownerId: BOB_UID,
    listed: false,
    listingId: "",
    lastTradeId: "trade_1",
    updatedAt: ts(-500),
  });
  acceptBatch.update(bob.collection("ducks").doc("bob_duck"), {
    ownerId: ALICE_UID,
    listed: false,
    listingId: "",
    lastTradeId: "trade_1",
    updatedAt: ts(-500),
  });
  acceptBatch.update(bob.collection("wallets").doc(BOB_UID), {
    coins: 300,
    lastClaimId: "trade_1",
    lastClaimType: "trade_accept",
    updatedAt: ts(-500),
  });
  await assertSucceeds(acceptBatch.commit());
});

test("Weekly Pond Showcase submission, modular avatar save, and atomic +15/+25 bonus grape voting", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = ts(-10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Quack",
      bio: "Showcase queen",
      avatarConfig: validPalette({ billStyle: "chirp", wingStyle: "angel", eyeStyle: "starry" }),
      duckCount: 1,
      tradesCompleted: 0,
      banned: false,
      banReason: "",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("wallets").doc(BOB_UID).set({
      userId: BOB_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("weekly_events").doc("week_golden_splash").set({
      title: "Golden Splash Gala",
      themeTag: "Week 47",
      description: "Golden pond showcase",
      bonusRarity: "legendary",
      active: true,
      weekNumber: 47,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("ducks").doc("alice_duck").set({
      ownerId: ALICE_UID,
      speciesId: "golden_monarch",
      name: "King Ducky",
      rarity: "legendary",
      serialNumber: 777,
      palette: validPalette({ hat: "crown", wingStyle: "angel" }),
      listed: false,
      listingId: "",
      lastTradeId: "",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Alice updates her modular avatar parts & saves in Firestore
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      avatarConfig: validPalette({ billStyle: "wide", wingStyle: "flappy", eyeStyle: "wink" }),
      updatedAt: ts(-3000),
    })
  );

  // Alice submits entry
  await assertSucceeds(
    alice
      .collection("weekly_events")
      .doc("week_golden_splash")
      .collection("entries")
      .doc("entry_1")
      .set({
        eventId: "week_golden_splash",
        userId: ALICE_UID,
        ownerDisplayName: "Alice Quack",
        ownerAvatar: validPalette({ billStyle: "wide", wingStyle: "flappy", eyeStyle: "wink" }),
        duckId: "alice_duck",
        duckName: "King Ducky",
        duckSpeciesId: "golden_monarch",
        duckRarity: "legendary",
        duckPalette: validPalette({ hat: "crown", wingStyle: "angel" }),
        caption: "Long live the pond!",
        voteCount: 0,
        createdAt: ts(-2000),
        updatedAt: ts(-2000),
      })
  );

  // Bob votes once atomically: +1 voteCount, +15 Grapes to Bob, +25 Grapes claim & notification for Alice
  const voteBatch = bob.batch();
  const entryRef = bob
    .collection("weekly_events")
    .doc("week_golden_splash")
    .collection("entries")
    .doc("entry_1");
  voteBatch.set(entryRef.collection("votes").doc(BOB_UID), {
    voterId: BOB_UID,
    entryId: "entry_1",
    createdAt: ts(-1000),
  });
  voteBatch.update(entryRef, {
    voteCount: 1,
    updatedAt: ts(-1000),
  });
  voteBatch.update(bob.collection("wallets").doc(BOB_UID), {
    coins: 265,
    lastClaimId: "week_golden_splash__entry_1",
    lastClaimType: "showcase_vote",
    updatedAt: ts(-1000),
  });
  voteBatch.set(bob.collection("coin_claims").doc(`vote_entry_1_${BOB_UID}`), {
    recipientId: ALICE_UID,
    sourceId: "entry_1",
    sourceType: "showcase_vote_reward",
    eventId: "week_golden_splash",
    amount: 25,
    claimed: false,
    createdAt: ts(-1000),
    updatedAt: ts(-1000),
  });
  voteBatch.set(
    bob.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_vote_1"),
    {
      userId: ALICE_UID,
      type: "showcase_vote",
      title: "King Ducky got a Showcase vote!",
      body: "Bob voted for King Ducky (+25 Bonus Grapes).",
      referenceId: "week_golden_splash",
      read: false,
      createdAt: ts(-1000),
    }
  );
  await assertSucceeds(voteBatch.commit());

  // Alice claims her +25 Bonus Grapes
  const ownerClaimBatch = alice.batch();
  ownerClaimBatch.update(alice.collection("coin_claims").doc(`vote_entry_1_${BOB_UID}`), {
    claimed: true,
    updatedAt: ts(-700),
  });
  ownerClaimBatch.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 275,
    lastClaimId: `vote_entry_1_${BOB_UID}`,
    lastClaimType: "sale",
    updatedAt: ts(-700),
  });
  await assertSucceeds(ownerClaimBatch.commit());

  // Bob cannot vote a second time or inflate voteCount without vote doc
  await assertFails(
    bob
      .collection("weekly_events")
      .doc("week_golden_splash")
      .collection("entries")
      .doc("entry_1")
      .update({
        voteCount: 99,
        updatedAt: ts(-500),
      })
  );
});

test("Promo codes (BETA, QUACK100) and *OWNERDUCK#1* admin powers", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = ts(-5000);

  // Seed Alice & Bob profiles and wallets
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Quack",
      bio: "Owner aspirant",
      avatarConfig: validPalette(),
      duckCount: 0,
      tradesCompleted: 0,
      banned: false,
      banReason: "",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Duck",
      bio: "Collector",
      avatarConfig: validPalette(),
      duckCount: 0,
      tradesCompleted: 0,
      banned: false,
      banReason: "",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("wallets").doc(BOB_UID).set({
      userId: BOB_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: now,
      updatedAt: now,
    });
  });

  // 1. Alice redeems BETA promo duck atomically
  const betaBatch = alice.batch();
  betaBatch.set(
    alice.collection("users").doc(ALICE_UID).collection("redeemed_codes").doc("BETA"),
    {
      userId: ALICE_UID,
      code: "BETA",
      rewardSummary: "Epic Beta Test Lotus unlocked!",
      redeemedAt: ts(-1000),
    }
  );
  betaBatch.set(alice.collection("ducks").doc("promo_beta_1"), {
    ownerId: ALICE_UID,
    speciesId: "sakura_float",
    name: "Beta Test Lotus",
    rarity: "epic",
    serialNumber: 777,
    palette: validPalette(),
    listed: false,
    listingId: "",
    lastTradeId: "",
    createdAt: ts(-1000),
    updatedAt: ts(-1000),
  });
  await assertSucceeds(betaBatch.commit());

  // 2. Alice redeems QUACK100 atomically (+100 coins), but forged +9999 fails
  const badCoinBatch = alice.batch();
  badCoinBatch.set(
    alice.collection("users").doc(ALICE_UID).collection("redeemed_codes").doc("QUACK100"),
    {
      userId: ALICE_UID,
      code: "QUACK100",
      rewardSummary: "+100 Pond Coins added!",
      redeemedAt: ts(-900),
    }
  );
  badCoinBatch.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 9999,
    lastClaimId: "QUACK100",
    lastClaimType: "promo",
    updatedAt: ts(-900),
  });
  await assertFails(badCoinBatch.commit());

  const coinBatch = alice.batch();
  coinBatch.set(
    alice.collection("users").doc(ALICE_UID).collection("redeemed_codes").doc("QUACK100"),
    {
      userId: ALICE_UID,
      code: "QUACK100",
      rewardSummary: "+100 Pond Coins added!",
      redeemedAt: ts(-800),
    }
  );
  coinBatch.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 350,
    lastClaimId: "QUACK100",
    lastClaimType: "promo",
    updatedAt: ts(-800),
  });
  await assertSucceeds(coinBatch.commit());

  // 3. Wrong ownerCode or cross-user admin unlock fails; valid *OWNERDUCK#1* succeeds
  await assertFails(
    alice.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      ownerCode: "WRONG_CODE",
      grantedAt: ts(-700),
    })
  );
  await assertFails(
    bob.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      ownerCode: "*OWNERDUCK#1*",
      grantedAt: ts(-700),
    })
  );
  await assertSucceeds(
    alice.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      ownerCode: "*OWNERDUCK#1*",
      grantedAt: ts(-700),
    })
  );

  // 4. Now as Owner, Alice can list users, gift Bob a duck & coins, ban Bob, and notify Bob
  await assertSucceeds(alice.collection("users").get());
  await assertSucceeds(
    alice.collection("ducks").doc("owner_gift_bob").set({
      ownerId: BOB_UID,
      speciesId: "golden_monarch",
      name: "King Ducky",
      rarity: "legendary",
      serialNumber: 1,
      palette: validPalette({ hat: "crown" }),
      listed: false,
      listingId: "",
      lastTradeId: "",
      createdAt: ts(-500),
      updatedAt: ts(-500),
    })
  );
  await assertSucceeds(
    alice.collection("wallets").doc(BOB_UID).update({
      coins: 50250,
      lastClaimId: "admin_grant_1",
      lastClaimType: "admin_grant",
      updatedAt: ts(-500),
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(BOB_UID).update({
      banned: true,
      banReason: "Quacking too loudly",
      updatedAt: ts(-500),
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(BOB_UID).collection("notifications").doc("notif_owner_1").set({
      userId: BOB_UID,
      type: "admin_announcement",
      title: "Owner Gift Received!",
      body: "You received King Ducky!",
      referenceId: "owner_gift_bob",
      read: false,
      createdAt: ts(-500),
    })
  );

  // 5. Bob CANNOT unban himself
  await assertFails(
    bob.collection("users").doc(BOB_UID).update({
      banned: false,
      banReason: "",
      updatedAt: ts(-100),
    })
  );
});

test("Daily missions persistence, anti-forge validation, and atomic Grape reward claim", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = ts(-4000);
  const dateKey = "2026-11-20";
  const visitDocId = `${dateKey}_visit_pond`;
  const tradeDocId = `${dateKey}_trade_duck`;

  // Seed Alice wallet
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coins: 250,
      lastClaimId: "",
      lastClaimType: "none",
      createdAt: now,
      updatedAt: now,
    });
  });

  // 1. Alice initializes visit_pond (progress 1/1) & trade_duck (progress 0/1)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(visitDocId).set({
      userId: ALICE_UID,
      missionId: "visit_pond",
      dateKey,
      title: "Visit the Pond",
      description: "Splash into your pond collection today",
      progress: 1,
      target: 1,
      rewardCoins: 40,
      claimed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(tradeDocId).set({
      userId: ALICE_UID,
      missionId: "trade_duck",
      dateKey,
      title: "Trade One Duck",
      description: "Send a trade offer or accept a swap in the Marketplace",
      progress: 0,
      target: 1,
      rewardCoins: 100,
      claimed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Forging rewardCoins or cross-user read/write must fail
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(`${dateKey}_hatch_egg`).set({
      userId: ALICE_UID,
      missionId: "hatch_egg",
      dateKey,
      title: "Hatch a Duck Egg",
      description: "Hatch any egg",
      progress: 1,
      target: 1,
      rewardCoins: 999, // invalid! must be 60
      claimed: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("daily_missions").doc(visitDocId).get()
  );

  // 3. Cannot claim wallet reward before trade_duck progress reaches target (0 < 1)
  const prematureClaim = alice.batch();
  prematureClaim.update(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(tradeDocId),
    { claimed: true, updatedAt: ts(-1000) }
  );
  prematureClaim.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 350,
    lastClaimId: tradeDocId,
    lastClaimType: "mission",
    updatedAt: ts(-1000),
  });
  await assertFails(prematureClaim.commit());

  // 4. Advance trade_duck progress to 1/1
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(tradeDocId).update({
      progress: 1,
      updatedAt: ts(-1500),
    })
  );

  // 5. Claiming wrong coin delta fails (+500 instead of +40 for visit_pond)
  const forgedDeltaClaim = alice.batch();
  forgedDeltaClaim.update(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(visitDocId),
    { claimed: true, updatedAt: ts(-1000) }
  );
  forgedDeltaClaim.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 750,
    lastClaimId: visitDocId,
    lastClaimType: "mission",
    updatedAt: ts(-1000),
  });
  await assertFails(forgedDeltaClaim.commit());

  // 6. Valid atomic claim for visit_pond (+40 Grapes) succeeds
  const validClaim = alice.batch();
  validClaim.update(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(visitDocId),
    { claimed: true, updatedAt: ts(-900) }
  );
  validClaim.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 290,
    lastClaimId: visitDocId,
    lastClaimType: "mission",
    updatedAt: ts(-900),
  });
  await assertSucceeds(validClaim.commit());

  // 7. Double-claim fails
  const doubleClaim = alice.batch();
  doubleClaim.update(
    alice.collection("users").doc(ALICE_UID).collection("daily_missions").doc(visitDocId),
    { claimed: true, updatedAt: ts(-500) }
  );
  doubleClaim.update(alice.collection("wallets").doc(ALICE_UID), {
    coins: 330,
    lastClaimId: visitDocId,
    lastClaimType: "mission",
    updatedAt: ts(-500),
  });
  await assertFails(doubleClaim.commit());
});

