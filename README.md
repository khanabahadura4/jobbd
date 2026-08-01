# ExamBD Android App (WebView + New-Post Notification)

## এটা কী

এই ফোল্ডারে **exambd.net** ওয়েবসাইটের একটা সম্পূর্ণ Android Studio প্রজেক্ট (Kotlin) দেওয়া আছে। ফিচার:

- ওয়েবসাইটটা WebView দিয়ে অ্যাপের ভেতরেই খোলে (pull-to-refresh সহ)
- PDF ডাউনলোড লিংকে ক্লিক করলে সিস্টেমের Download Manager দিয়ে ডাউনলোড হয়
- **নতুন পোস্ট নোটিফিকেশন**: প্রতি ১৫ মিনিট পরপর ব্যাকগ্রাউন্ডে (WorkManager দিয়ে) সাইটের RSS ফিড (`https://exambd.net/feed/`) চেক করে। নতুন কোনো পোস্ট এলে ফোন নোটিফিকেশন দেখায়, আর ওই নোটিফিকেশনে ট্যাপ করলে সরাসরি সেই পোস্টটা অ্যাপের ভেতরে খুলে যায়

## ⚠️ জরুরি একটা কথা

আমি (Claude) একটা browser sandbox-এ কাজ করি — এখানে Android SDK/Gradle/emulator ইনস্টল করা নেই এবং Google-এর SDK সার্ভারে নেটওয়ার্ক অ্যাক্সেসও নেই। তাই আমি সরাসরি রেডি একটা `.apk` ফাইল বানিয়ে দিতে পারছি না। যেটা দিতে পারছি সেটা হলো — **সম্পূর্ণ, কাজ-করা সোর্স কোড**, যেটা তুমি নিজের কম্পিউটারে Android Studio দিয়ে ২ মিনিটে APK-তে বিল্ড করে নিতে পারবে (একদম ফ্রি, কোনো টাকা লাগবে না)।

## কীভাবে APK বানাবে (ধাপে ধাপে)

1. [Android Studio](https://developer.android.com/studio) ইনস্টল করো (না থাকলে)।
2. Android Studio খুলে **Open** করো এই `ExamBDApp` ফোল্ডারটা।
3. প্রথমবার খোলার সময় Gradle sync হবে (ইন্টারনেট লাগবে) — একটু সময় নিবে, শেষ হওয়া পর্যন্ত অপেক্ষা করো।
4. সাইনক শেষ হলে উপরের মেনু থেকে: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. বিল্ড শেষ হলে নিচে ডান কোণায় "locate" লিংক আসবে — ওখান থেকে ক্লিক করলে `app/build/outputs/apk/debug/app-debug.apk` ফাইলটা পাবে।
6. এই `.apk` ফাইলটা ফোনে পাঠিয়ে ইনস্টল করলেই অ্যাপ রেডি (ইনস্টলের সময় "Unknown sources" পারমিশন দিতে হতে পারে)।

> Play Store-এ পাবলিশ করতে চাইলে আলাদাভাবে "Generate Signed Bundle/APK" দিয়ে সাইন করা ভার্সন বানাতে হবে — চাইলে সেটাও পরে বলে দিতে পারি।

## বিকল্প উপায়: GitHub Actions দিয়ে (Android Studio ছাড়াই APK বানানো)

Android Studio ইনস্টল করতে না চাইলে GitHub দিয়েও APK বানানো যায় — GitHub-এর সার্ভারই বিল্ড করে দেবে (`.github/workflows/build-apk.yml` ফাইলটা এই প্রজেক্টে দেওয়া আছে)।

1. [github.com](https://github.com)-এ একটা ফ্রি অ্যাকাউন্ট বানাও (না থাকলে)
2. উপরে ডান দিকে **+** আইকনে ক্লিক করে **New repository** বানাও (নাম যা খুশি, যেমন `exambd-app`) — **Public** বা **Private** যেকোনোটা রাখতে পারো
3. রিপোজিটরি বানানোর পর ওই পেজে **"uploading an existing file"** লিংকে ক্লিক করো
4. এই ZIP extract করা **ExamBDApp** ফোল্ডারের **ভেতরের সব ফাইল/ফোল্ডার** (README.md, app, build.gradle.kts, settings.gradle.kts, gradle.properties, .github — সবকিছু) সিলেক্ট করে সেখানে ড্র্যাগ-ড্রপ করো, তারপর **Commit changes** দাও
5. উপরের **Actions** ট্যাবে যাও — দেখবে "Build APK" নামে একটা workflow নিজে থেকেই রান হচ্ছে (২-৪ মিনিট লাগবে)
6. রান শেষ হয়ে সবুজ ✅ চিহ্ন আসলে সেই রানের উপর ক্লিক করো
7. নিচের দিকে **Artifacts** সেকশনে `exambd-debug-apk` নামে একটা ZIP পাবে — সেটা ডাউনলোড করো, ভেতরে `app-debug.apk` থাকবে
8. এই APK ফোনে পাঠিয়ে ইনস্টল করে দাও

> নোট: GitHub-এ আপলোড করলে কোড publicly দেখা যাবে যদি repository "Public" রাখো (private রাখলে শুধু তুমি দেখবে) — কিন্তু এতে কোনো password/secret নেই, শুধু ওয়েবসাইট ওপেন করার কোড, তাই সমস্যা নেই।

## নোটিফিকেশন ফিচার কীভাবে কাজ করে

- অ্যাপ প্রথমবার চালু হলে Android 13+ ডিভাইসে নোটিফিকেশন পারমিশন চাইবে
- `NewPostWorker.kt` প্রতি ১৫ মিনিটে (WorkManager-এর সর্বনিম্ন সীমা এটাই) `https://exambd.net/feed/` RSS ফিড চেক করে, একদম উপরের (মানে সবচেয়ে নতুন) পোস্টের লিংকটা আগেরবারের সাথে তুলনা করে
- লিংক আলাদা হলে ধরে নেয় নতুন পোস্ট এসেছে → নোটিফিকেশন দেখায়
- এটা ব্যাটারি বাঁচাতে ইন্টারনেট কানেকশন থাকলেই কাজ করে এবং ফোন বন্ধ/রিস্টার্ট হলেও WorkManager আবার শিডিউল হয়ে যায়

## ফাইল স্ট্রাকচার

```
ExamBDApp/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/net/exambd/app/
│       │   ├── MainActivity.kt        <- WebView + ডাউনলোড হ্যান্ডলিং
│       │   ├── NewPostWorker.kt       <- ব্যাকগ্রাউন্ডে RSS চেক
│       │   └── NotificationHelper.kt  <- নোটিফিকেশন দেখানো
│       └── res/                        <- layout, icon, string ইত্যাদি
├── build.gradle.kts
└── settings.gradle.kts
```

## চাইলে যেটা কাস্টমাইজ করা যায়

- **App name / icon**: `res/values/strings.xml` আর `res/mipmap-*/ic_launcher.png` বদলে দাও
- **চেক করার সময়**: `NewPostWorker.kt`-তে `15, TimeUnit.MINUTES` বদলাতে পারো (তবে Android নিজেই ১৫ মিনিটের কমে periodic work চালাতে দেয় না)
- **Package name**: `applicationId` (build.gradle.kts) ও `namespace` বদলাতে হবে, সাথে ফোল্ডার স্ট্রাকচারও ম্যাচ করাতে হবে
