// Follow Me Planner: Face ID and an encrypted copy of Simone's families.
//
// The Planner is a public page with no server, so nothing can stop someone
// opening it. What this does: on each phone, the Planner is set up once with
// a setup code (only its fingerprint is here) and a passkey (Face ID or Touch
// ID). The families and templates are kept encrypted (AES-GCM) with a key
// that only that passkey can give back (the WebAuthn PRF extension), so
// without Face ID on that phone they can't be read, even from a copy of the
// browser's storage. The key lives only in memory while the Planner is open,
// and is dropped after five minutes in the background.
//
// window.fmtVault:
//   status()            "new" (not set up here) | "locked" | "open" | "unsupported"
//   setup(code, seed)   check the code, make the passkey, start the vault with seed (JSON object of entries)
//                       → "ok" | "bad-code" | "unsupported" | "cancelled" | "error"
//   unlock()            Face ID → "ok" | "cancelled" | "error"
//   get(key) / put(key, value|null) / keys(prefix)   the entries, while open
//   lock(), reset()     drop the key / erase this phone's Planner
//   onLock(fn)          called when it locks itself
(function () {
  const CODE = { salt: "GR+oSdPkVuZa1FjTDmwu0A==", hash: "zOMQ9a4wqMOBWnqrD4cDMOmW/SmXGZxwAQtiDk09u4E=", iterations: 310000 };
  // { v, cred, salt, iv, data } (base64), all safe to keep in the open. Not "fmt." (that prefix is
  // the old one-key-per-entry storage, which the app moves into its record and clears).
  const STORE = "fmt-planner-vault";
  const LOCK_AFTER_MS = 5 * 60 * 1000;
  const enc = new TextEncoder(), dec = new TextDecoder();
  const b64 = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf)));
  const unb64 = (s) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0));
  const rand = (n) => crypto.getRandomValues(new Uint8Array(n));
  const load = () => { try { return JSON.parse(localStorage.getItem(STORE) || "null"); } catch (e) { return null; } };
  const save = (v) => { try { localStorage.setItem(STORE, JSON.stringify(v)); } catch (e) {} };

  let key = null, entries = null, lockFns = [], hiddenAt = 0;

  const supported = () => !!(window.PublicKeyCredential && navigator.credentials && crypto.subtle);

  async function codeOk(code) {
    const norm = String(code || "").toUpperCase().replace(/[^A-Z0-9]/g, "").replace(/(.{4})(?=.)/g, "$1-");
    const bits = await crypto.subtle.deriveBits(
      { name: "PBKDF2", hash: "SHA-256", salt: unb64(CODE.salt), iterations: CODE.iterations },
      await crypto.subtle.importKey("raw", enc.encode(norm), "PBKDF2", false, ["deriveBits"]), 256);
    return b64(bits) === CODE.hash;
  }

  // The passkey's PRF output, made into an AES-GCM key (never leaves memory).
  async function prfKey(credId, salt) {
    const got = await navigator.credentials.get({ publicKey: {
      challenge: rand(32), rpId: location.hostname, userVerification: "required",
      allowCredentials: [{ type: "public-key", id: unb64(credId) }],
      extensions: { prf: { eval: { first: unb64(salt) } } },
    } });
    const out = got && got.getClientExtensionResults().prf;
    if (!out || !out.results || !out.results.first) throw new Error("no-prf");
    const base = await crypto.subtle.importKey("raw", out.results.first, "HKDF", false, ["deriveKey"]);
    return crypto.subtle.deriveKey({ name: "HKDF", hash: "SHA-256", salt: unb64(salt), info: enc.encode("follow-me-planner vault") },
      base, { name: "AES-GCM", length: 256 }, false, ["encrypt", "decrypt"]);
  }

  let writing = Promise.resolve();
  function persist() {
    if (!key || !entries) return;
    const snapshot = JSON.stringify(entries), k = key;
    writing = writing.then(async () => {
      const v = load(); if (!v) return;
      const iv = rand(12);
      v.iv = b64(iv);
      v.data = b64(await crypto.subtle.encrypt({ name: "AES-GCM", iv }, k, enc.encode(snapshot)));
      save(v);
    }).catch(() => {});
  }

  function lock() {
    key = null; entries = null;
    lockFns.forEach((f) => { try { f(); } catch (e) {} });
  }

  document.addEventListener("visibilitychange", () => {
    if (document.hidden) hiddenAt = Date.now();
    else if (key && hiddenAt && Date.now() - hiddenAt > LOCK_AFTER_MS) lock();
  });

  window.fmtVault = {
    status() { return !supported() ? "unsupported" : !load() ? "new" : key ? "open" : "locked"; },
    async setup(code, seed) {
      try {
        if (!supported()) return "unsupported";
        if (!(await codeOk(code))) return "bad-code";
        const salt = b64(rand(32));
        const cred = await navigator.credentials.create({ publicKey: {
          challenge: rand(32), rp: { id: location.hostname, name: "Follow Me Planner" },
          user: { id: rand(16), name: "Follow Me Planner", displayName: "Follow Me Planner" },
          pubKeyCredParams: [{ type: "public-key", alg: -7 }, { type: "public-key", alg: -257 }],
          authenticatorSelection: { authenticatorAttachment: "platform", residentKey: "required", userVerification: "required" },
          extensions: { prf: { eval: { first: unb64(salt) } } },
        } });
        if (!cred) return "cancelled";
        const p = cred.getClientExtensionResults().prf;
        if (p && p.enabled === false) return "unsupported";
        const credId = b64(cred.rawId);
        const k = await prfKey(credId, salt); // Face ID again: some phones only give the key on a sign-in
        save({ v: 1, cred: credId, salt });
        key = k; entries = (seed && typeof seed === "object") ? seed : {};
        persist(); await writing;
        return "ok";
      } catch (e) {
        if (String(e && e.message) === "no-prf") return "unsupported";
        return e && e.name === "NotAllowedError" ? "cancelled" : "error";
      }
    },
    async unlock() {
      const v = load(); if (!v) return "error";
      try {
        const k = await prfKey(v.cred, v.salt);
        entries = v.data ? JSON.parse(dec.decode(await crypto.subtle.decrypt({ name: "AES-GCM", iv: unb64(v.iv) }, k, unb64(v.data)))) : {};
        key = k;
        return "ok";
      } catch (e) {
        return e && e.name === "NotAllowedError" ? "cancelled" : "error";
      }
    },
    get(k) { return entries && Object.prototype.hasOwnProperty.call(entries, k) ? entries[k] : null; },
    put(k, value) { if (!entries) return; if (value == null) delete entries[k]; else entries[k] = String(value); persist(); },
    keys(prefix) { return entries ? Object.keys(entries).filter((k) => k.indexOf(prefix) === 0) : []; },
    lock,
    reset() { try { localStorage.removeItem(STORE); } catch (e) {} lock(); },
    onLock(fn) { lockFns.push(fn); },
  };
})();
