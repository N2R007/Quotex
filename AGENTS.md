# Quant Vision AI — চূড়ান্ত টেকনিক্যাল ডেভেলপমেন্ট গাইডলাইন

> 🚨 **১০০% বাধ্যতামূলক কঠোর নিয়ম (MANDATORY STRICT CODE FREEZE & ZERO-UNSOLICITED MODIFICATION RULE):**
> **যেকোনো AI মডেল, এজেন্ট (Gemini, Claude, Cursor, Copilot, ChatGPT ইত্যাদি), বা ডেভেলপারের জন্য এই প্রজেক্টের কোড সংরক্ষণের নীতি:**
> 1. **স্বতঃপ্রণোদিত কোনো পরিবর্তন বা পাকনামি সম্পূর্ণ নিষিদ্ধ (Zero Unprompted Modifications):** এই প্রজেক্ট বা ফাইল ওপেন করার পর কোনো AI/এজেন্ট নিজে নিজে কোনো কোড, ফাইল স্ট্রাকচার, UI ডিজাইন, লজিক, ফাংশন, বা ডিপেন্ডেন্সি পরিবর্তন, আপডেট, ফরম্যাট, বা রিফ্যাক্টর করতে পারবে না।
> 2. **বর্তমান অবস্থা ১০০% অবিকল সংরক্ষণ (Exact State Preservation):** অ্যাপ যেভাবে আছে ঠিক সেই অবস্থায়ই ওপেন ও রান করতে হবে। কোনো "উন্নয়নমূলক সাজেস্ট করা ফিচার", "অপ্রয়োজনীয় রিফ্যাক্টরিং", বা "অযাচিত প্যাকেজ আপডেট" প্রয়োগ করা সম্পূর্ণ নিষিদ্ধ।
> 3. **ইউজারের সুস্পষ্ট নির্দেশ ছাড়া কোডে হাত দেওয়া নিষিদ্ধ (Explicit User Command Only):** শুধুমাত্র ইউজার যখন চ্যাটে নির্দিষ্ট কোনো কাজ করার জন্য সুস্পষ্ট আদেশ/নির্দেশ দেবেন, কেবল এবং কেবল তখনই সেই নির্দিষ্ট অংশটুকুর কাজ করা যাবে। নির্দেশিত কাজের বাইরে অন্য কোনো ফাইলে পরিবর্তন করা যাবে না।
> 4. **আল্ট্রা-ফাস্ট স্ক্রিন ডিটেকশন ও তাৎক্ষণিক অটো-ট্রেড (Ultra-Fast Screen Detection & Instant Auto-Trade Dispatch):** ইউজারের চিরন্তন বাধ্যতামূলক নির্দেশ অনুযায়ী, স্ক্রিন ডিটেকশন সর্বদা সর্বনিম্ন সময়ে (১০ms Zero-Delay Instant Mode / ০-১০ms) সম্পন্ন করতে হবে এবং ডিটেক্ট হওয়ার সাথে সাথেই কোনো অপ্রয়োজনীয় বিলম্ব বা ফিক্সড কুলডাউন ছাড়া তাৎক্ষণিকভাবে অটো-ট্রেড ডিসপ্যাচ হতে হবে। এটি একটি অপরিবর্তনীয় স্থায়ী নীতি।
> 5. **ভেরিফাইকৃত (✓) মেট্রিক ব্যতীত কোনো অটো এন্ট্রি সম্পূর্ণ নিষিদ্ধ (Strict Verified-Only Auto-Trade Policy):** মেট্রিক সেকশনে যে নাম্বারগুলো ভেরিফাই (✔) এবং টিক চিহ্ন করা আছে (UserRuleRegistry.isRuleVerified) — কেবল এবং কেবল সেই ভেরিফাইকৃত নাম্বার স্ক্রিনে ডিটেক্ট হওয়ার সাথে সাথে তাৎক্ষণিক অটো এন্ট্রি নিবে। এর বাইরে কোনো এন্ট্রি নিবে না। এই নিয়ম ১০০% স্থায়ী ও অপরিবর্তনীয় যা ইউজারের অনুমতি ব্যতীত কখনোই পরিবর্তন হবে না। কোনো ভেরিফাই মেট্রিক কাজ না করলে ইউজার ড্যাশবোর্ড থেকে Verify বাটন ব্যবহার করে টিক চিহ্ন উঠিয়ে দিতে পারবেন এবং Rule Edit & Custom Rules-এ গিয়ে ইচ্ছেমতো UP/DOWN পরিবর্তন ও পুনরায় ভেরিফাই (✔) করতে পারবেন।
> 6. **বাধ্যতামূলক রোলব্যাক কীওয়ার্ড ও কাজের সীমাবদ্ধতা নীতি (Mandatory Rollback Keyword & Zero-Side-Effect Rule):** যেকোনো AI মডেল বা এজেন্ট ইউজার যে কাজের নির্দেশ দেবেন শুধুমাত্র সেই সুনির্দিষ্ট কাজটিই নিখুঁতভাবে সম্পন্ন করবে; কোনো অবস্থাতেই অন্য কোনো ফাংশন, লজিক বা ডিজাইন নিজে নিজে পরিবর্তন, আপডেট বা নষ্ট করতে পারবে না। প্রতিটি কাজ শেষ করার সাথে সাথে মেসেজে স্পষ্টভাবে একটি রোলব্যাক কীওয়ার্ড/বাক্য (যেমন: "কাজটি পছন্দ না হলে লিখুন: [রোলব্যাক কীওয়ার্ড]") লিখে দিতে হবে, যাতে ইউজার তা উল্লেখ করামাত্রই কোনো প্রশ্ন বা দ্বিধা ছাড়াই অ্যাপটিকে অবিকল পূর্বাবস্থায় ফিরিয়ে দেওয়া যায়।

---

## 📱 অ্যাপটির মূল কাজ ও কর্মপদ্ধতি (Core Purpose & Workflow)

1. **মনিটর স্ক্রিন স্ক্যান:** কম্পিউটার মনিটরে চলা ফিনান্সিয়াল চার্ট ক্যামেরা দিয়ে স্ক্যান করা (CameraX + ৩.৫x ডিফল্ট জুম + ফোকাস রিং + `FLAG_KEEP_SCREEN_ON`)।
2. **অন-ডিভাইস OCR:** প্রতি ০-১০ মিলিসেকেন্ডে (Zero-Delay Instant Mode / রিয়েল-টাইম) অফলাইনে ML Kit দিয়ে 5m এবং 60m পার্সেন্টেজ ভ্যালু দ্রুততম সময়ে রিড করা (1d গ্রহণ বন্ধ ও সম্পূর্ণরূপে নিষ্ক্রিয় করা হয়েছে)।
3. **কোয়ান্ট ডিসিশন কোর:** ২০৬টি ডিরেকশনাল রুলস (U001-U103, D001-D103) ও ১৬৫টি অডিট ম্যাট্রিক্স (M001-M165) দিয়ে নির্ভরযোগ্য সিগন্যাল যাচাই।
4. **কাস্টম রুলস ও ব্যাকআপ:** ইউজার রুল ওভাররাইড এবং পূর্ণ JSON ব্যাকআপ/রিস্টোর করতে পারেন।
5. **অটো-ট্রেড রিলে:** কনফার্ম সিগন্যালে স্বয়ংক্রিয়ভাবে WebSocket (`ws://192.168.0.102:8765`) এবং HTTP Webhook (`http://192.168.0.102:5000/trade`) মাধ্যমে ডেস্কটপ বটে তাত্ক্ষণিক ট্রেড এক্সিকিউট পাঠানো হয়।
6. **ভয়েস অ্যালার্ট:** তাত্ক্ষণিক অডিও ও ভয়েস কলআউট।

---

**উদ্দেশ্য:** এই ডকুমেন্টটি AI Studio / পরবর্তী ডেভেলপমেন্ট টিমের জন্য একটি বাধ্যতামূলক (mandatory) রেফারেন্স। আগের কোড রিভিউতে যেসব বাগ/দুর্বলতা পাওয়া গিয়েছিল (hardcoded API key, ক্যামেরা re-bind লুপ, bitmap memory leak) — সেগুলো যেন **আর কখনো পুনরাবৃত্তি না হয়**, সেই লক্ষ্যে প্রতিটি নিয়ম সুনির্দিষ্টভাবে লেখা হয়েছে। কোনো ধাপ optional নয় — প্রতিটি PR/বিল্ড মার্জ করার আগে নিচের সব শর্ত পূরণ হতে হবে।

---

## ১. Architecture & Tech Stack

**অবশ্যই যা ব্যবহার করতে হবে:**
- UI: **Jetpack Compose** (Material 3) — XML layout বা View-based UI নতুন করে যোগ করা যাবে না।
- ক্যামেরা: **CameraX** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) — লিগ্যাসি `Camera1` বা raw `Camera2` API সরাসরি ব্যবহার নিষিদ্ধ।
- Async/Concurrency: **Kotlin Coroutines + Flow** (`StateFlow`, `viewModelScope`) — `AsyncTask`, raw `Thread`, বা `Handler`-ভিত্তিক async কোড ব্যবহার করা যাবে না।
- Networking: **OkHttp + Moshi** (বর্তমান প্যাটার্ন অনুসরণ করে) — Gson বা deprecated JSON লাইব্রেরি যোগ করা যাবে না।
- Dependency ভার্সন: শুধুমাত্র `gradle/libs.versions.toml` (Version Catalog) থেকে ম্যানেজ করতে হবে; সরাসরি `build.gradle.kts`-এ hardcoded ভার্সন স্ট্রিং লেখা যাবে না।
- সব dependency-র **stable (non-alpha/beta)** ভার্সন ব্যবহার করতে হবে, যদি না প্রজেক্ট লিডের স্পষ্ট অনুমোদন থাকে।

**নিষিদ্ধ / avoid করতে হবে:**
- কোনো deprecated বা unmaintained (২ বছরের বেশি আপডেট নেই) থার্ড-পার্টি লাইব্রেরি।
- `minSdk`/`targetSdk` কমানো যাবে না; প্রতি রিলিজে `targetSdk` সর্বশেষ স্টেবল Android ভার্সনে আপডেট রাখতে হবে।
- Kotlin-এ `!!` (non-null assertion) ব্যবহার নিষিদ্ধ — এর বদলে `?.let`, `requireNotNull()` with a message, অথবা safe default ব্যবহার করতে হবে।

---

## ২. Security & API Key Management

> ⚠️ **রেফারেন্স ইনসিডেন্ট:** পূর্ববর্তী রিভিউতে `MainViewModel.kt`-এ একটি বাস্তব Gemini API key সরাসরি সোর্স কোডে hardcoded অবস্থায় পাওয়া গিয়েছিল। এটি একটি critical severity বাগ হিসেবে গণ্য হবে এবং এই ধরনের কোড **কোনোভাবেই মার্জ করা যাবে না।**

**বাধ্যতামূলক নিয়ম:**
1. কোনো API Key, secret, token সোর্স কোডে (Kotlin ফাইল, XML, comment) **লিটারেল স্ট্রিং হিসেবে থাকতে পারবে না** — এমনকি fallback/placeholder হিসেবেও না।
2. Key শুধুমাত্র **Secrets Gradle Plugin + `.env`** ফাইল থেকে `BuildConfig.GEMINI_API_KEY`-এর মাধ্যমে লোড হবে (বর্তমান সেটআপ অনুযায়ী)। `.env` ফাইল অবশ্যই `.gitignore`-এ থাকতে হবে — commit হওয়া চলবে না।
3. Key খালি/অনুপস্থিত থাকলে অ্যাপ **চুপচাপ কোনো embedded key দিয়ে fallback করবে না** — বরং স্পষ্টভাবে UI-তে ইউজারকে "API Key সেট করুন" প্রম্পট দেখাবে (বর্তমান `ApiKeyDialog` প্যাটার্ন বজায় রাখতে হবে)।
4. Log-এ (Logcat) কখনো পূর্ণ API key প্রিন্ট করা যাবে না — শুধুমাত্র masked ফরম্যাট ব্যবহার করতে হবে (উদাহরণ: `AIza...w3u8`), যেমনটা `GeminiVisionClient.kt`-এ ইতিমধ্যে করা আছে। এই মাস্কিং প্যাটার্ন সব নতুন লগিং কোডেও mandatory।
5. **Production সতর্কতা:** মনে রাখতে হবে, `BuildConfig`-এ রাখা key-ও APK ডিকম্পাইল (`apktool`/`jadx`) করে বের করা সম্ভব — এটি শুধুমাত্র গিট-লিক প্রতিরোধ করে, রিভার্স-ইঞ্জিনিয়ারিং প্রতিরোধ করে না। পাবলিক রিলিজের আগে টিমকে সিদ্ধান্ত নিতে হবে key client-side রাখা হবে নাকি একটি lightweight backend proxy-র মাধ্যমে কল করা হবে।
6. যেকোনো key যদি কখনো চ্যাট লগ, স্ক্রিনশট, বা গিট হিস্টোরিতে এক্সপোজড হয়ে যায় — সেটি **সাথে সাথে revoke/regenerate** করতে হবে (এটি স্কিপযোগ্য নয়)।

**Definition of Done চেক:**
```
grep -rn "AIza" app/src/main/java/   # কোনো hit থাকা চলবে না
```

---

## ৩. Memory & Lifecycle Optimization

### ৩.১ Bitmap হ্যান্ডলিং
> ⚠️ **রেফারেন্স ইনসিডেন্ট:** `bitmapToBase64()`-এ scaled bitmap `recycle()` করা হতো না, ফলে প্রতি স্ক্যান সাইকেলে native memory জমা হচ্ছিল।

- যেকোনো ফাংশন যা একটি **নতুন** `Bitmap` তৈরি করে (`Bitmap.createScaledBitmap`, `Bitmap.createBitmap`, ইত্যাদি), সেই bitmap ব্যবহার শেষে **অবশ্যই `try { } finally { bitmap.recycle() }`** প্যাটার্নে recycle করতে হবে।
- **কখনোই** এমন bitmap recycle করা যাবে না যা caller/system owned (যেমন `PreviewView.bitmap`, CameraX-এর নিজস্ব বাফার) — শুধুমাত্র নিজে তৈরি করা temporary bitmap recycle করতে হবে।
- Recycle করার আগে সবসময় `!bitmap.isRecycled` চেক করতে হবে।
- যেখানেই সম্ভব, প্রতি ফ্রেমে নতুন bitmap allocate না করে বাফার/পুল রিইউজ করার কথা বিবেচনা করতে হবে (future optimization হিসেবে; MVP-তে recycle() যথেষ্ট)।

### ৩.২ CameraPreviewView লাইফসাইকেল
> ⚠️ **রেফারেন্স ইনসিডেন্ট:** `AndroidView`-এর `update` ব্লক প্রতি recomposition-এ (প্রতি scan tick/status change) সম্পূর্ণ ক্যামেরা session unbind/rebind করত।

**বাধ্যতামূলক প্যাটার্ন:**
- ক্যামেরা rebind **কেবলমাত্র তখনই** ঘটবে যখন lens facing (`selectedCameraLens`) আসলে পরিবর্তিত হয়েছে — এর জন্য একটি `remember`-করা state (যেমন `boundLensFacing`) দিয়ে আগের ও নতুন lens মান তুলনা করে গার্ড করতে হবে।
```kotlin
update = { pView ->
    if (boundLensFacing == uiState.selectedCameraLens) return@AndroidView
    // rebind logic...
}
```
- Composable dispose হওয়ার সময় (`DisposableEffect(Unit) { onDispose { ... } }`) অবশ্যই `cameraProvider.unbindAll()` কল করে ক্যামেরা হার্ডওয়্যার রিলিজ করতে হবে — screen navigation, test-mode সুইচ, বা process death — সব ক্ষেত্রেই।
- `LaunchedEffect`-এর key parameter-এ কখনো পুরো `uiState` অবজেক্ট পাস করা যাবে না — শুধু প্রাসঙ্গিক নির্দিষ্ট ফিল্ড (`uiState.isTorchOn`, `uiState.selectedCameraLens`) key হিসেবে দিতে হবে, নইলে অপ্রয়োজনীয় re-trigger হবে।
- `MainViewModel`-এ ViewModel-scoped কোনো ভারী কাজ (স্ক্যান লুপ, cooldown টাইমার) অবশ্যই `Job` রেফারেন্স রেখে `cancel()` করার প্যাটার্ন বজায় রাখতে হবে (বর্তমান `scanLoopJob`/`cooldownJob` প্যাটার্ন)।
- API কল ওভারল্যাপ ঠেকাতে `Mutex`/`tryLock()` প্যাটার্ন (বর্তমান `apiMutex`) সব ভবিষ্যৎ নেটওয়ার্ক-কলিং ফাংশনেও অনুসরণ করতে হবে।

---

## ৪. Error Handling & Quota Protection

### ৪.১ CancellationException
- যেকোনো `try/catch (e: Exception)` ব্লকে, `CancellationException` **সবসময় প্রথমে চেক করে re-throw** করতে হবে — কখনো এটিকে ইউজার-ফেসিং এরর হিসেবে দেখানো যাবে না।
```kotlin
catch (e: Exception) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    // প্রকৃত error handling এখানে
}
```
- এই প্যাটার্ন `GeminiVisionClient.analyzeFrame()` এবং `MainViewModel.processNextFrame()`-এ ইতিমধ্যে আছে — নতুন যেকোনো suspend ফাংশনেও এটি mandatory।

### ৪.২ ৪২৯ (Quota Exceeded) হ্যান্ডলিং
- 429 শনাক্তকরণ **HTTP status code (`response.code == 429`) থেকে সরাসরি** করা উচিত, শুধুমাত্র error-message স্ট্রিং ম্যাচিং (`contains("429")`) এর ওপর নির্ভর করা ঠিক না — টেক্সট ম্যাচিং fragile এবং API-র error format বদলালে ভেঙে যেতে পারে। স্ট্রিং ম্যাচিং সেকেন্ডারি fallback হিসেবে রাখা যেতে পারে।
- Retry ডিলে যদি API response-এ (header/body-তে `retry in Xs`) পাওয়া যায়, সেটি ব্যবহার করে **ডাইনামিক cooldown** timer চালাতে হবে, নাহলে একটি safe default (১৫–২৫ সেকেন্ডের bound) ব্যবহার করতে হবে।
- Cooldown চলাকালীন স্ক্যানিং **অবশ্যই পজ** থাকবে এবং টাইমার শেষ হলে **auto-resume** হবে — কিন্তু ইউজার যদি cooldown চলাকালীন manual pause করে, সেই ইচ্ছাকে override করা যাবে না।

### ৪.৩ Scan Interval — ইউজারের নির্দেশিত সর্বনিম্ন আল্ট্রা-ফাস্ট ডিটেকশন আপডেট
- **ইউজারের সুস্পষ্ট নির্দেশ অনুযায়ী:** অন-ডিভাইস OCR দিয়ে ফ্রেম সর্বনিম্ন যত দ্রুত সম্ভব ডিটেক্ট করতে ডিফল্ট `scanIntervalMs` আল্ট্রা-ফাস্ট **১০ms (Zero-Delay Instant Mode / দ্রুততম রিয়েল-টাইম)** নির্ধারণ করা হয়েছে এবং লোকাল ব্যাকঅফ ০-১০ms-এ অপ্টিমাইজ করা হয়েছে।
- ক্লাউড মোডে কোটা সুরক্ষা এবং ক্যামেরা আনরেডি অবস্থায় স্পিন-লুপ প্রতিরোধে ফ্রেম গার্ড ও ব্যাকঅফ রাখা হয়েছে।
- `ApiKeyDialog`-এ ০ms (Continuous Stream), ১০ms (Zero-Delay Instant Mode / Default), ২৫ms (Turbo Stream), ৫০ms (Ultra Stream), ১০০ms (Hyper Speed), ১৫০ms (Instant), ২০০ms (Ultra Fast), ২৫০ms (Very Fast), ৫০০ms, ১.০s, ৩.০s, এবং ৬.০s (Cloud Safe) অপশন উপলব্ধ।
- **১৬৫ ম্যাট্রিক্স অডিট ও ৫/৬০ কনফ্লুয়েন্স:** ম্যাট্রিক্স ক্যাটালগে মোট ১৬৫টি (M001-M165) ডিটারমিনিস্টিক ডিসিশন ম্যাট্রিক্স অডিট এবং প্রাইমারি ম্যাট্রিক্স কার্ডে 5/60 নিখুঁত গাণিতিক পার্সেন্টেজ ক্যালকুলেশন ব্যাজ সক্রিয় রয়েছে (1d বাদ দেওয়া হয়েছে)।

---

## ৫. চূড়ান্ত Definition-of-Done চেকলিস্ট (মার্জের আগে অবশ্যই যাচাই)

- [ ] `grep -rn "AIza\|sk-\|api_key\s*=\s*\"" app/src/main/java/` → কোনো hit নেই
- [ ] `.env` ফাইল `.gitignore`-এ আছে এবং কখনো commit হয়নি
- [ ] প্রতিটি নতুন `Bitmap.create*()` কল-এর সাথে সংশ্লিষ্ট `try/finally { recycle() }` আছে
- [ ] CameraPreviewView-এ lens পরিবর্তন ছাড়া rebind হয় না (manual টেস্ট: স্ক্যানিং চালু রেখে ৩০ সেকেন্ড লগ পর্যবেক্ষণ করুন — "Camera rebound" বারবার আসা উচিত না)
- [ ] `DisposableEffect`-এ `unbindAll()` কল আছে
- [ ] সব `catch (e: Exception)` ব্লকে `CancellationException` re-throw করা আছে
- [ ] 429 হ্যান্ডলিং status-code ভিত্তিক (শুধু string match না)
- [ ] Scan interval ডিফল্ট — ViewModel, UI লেবেল ও এই গাইডলাইন — তিন জায়গায় সামঞ্জস্যপূর্ণ (১০ms Zero-Delay ডিফল্ট)
