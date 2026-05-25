# 🔒 Focus Lock PRO: Ultimate Self-Discipline Suite 🚀

**Focus Lock PRO** एक प्रीमियम, आधुनिक और उच्च-स्तरीय (Pro-level) एंड्रॉइड एप्लिकेशन है जो आपको डिजिटल व्याकुलता (Digital Distractions) से मुक्त रखने, उत्पादकता (Productivity) बढ़ाने और आपकी दैनिक आदतों (Habits) को ट्रैक करने में मदद करता है। यह ऐप Jetpack Compose, Room Database और आधुनिक Material Design 3 पर आधारित है।

---

## 🎨 Premium Features & Modules (विशेषताएं)

यह एप्लीकेशन सभी महत्वपूर्ण टूल्स को एक ही स्थान (**Unified Workspace Suite**) पर केंद्रित करता है:

### 1. ⏱️ फोकस घड़ी (Focus Timer Workspace)
*   **प्रोग्रेसिव सर्कुलर विज़ुअल टाइमर (Progressive Circular Timer):** काम के शेष समय को दर्शाने वाला एक आकर्षक एनिमेटेड ग्रेडिएंट सर्कुलर प्रोग्रेस बार।
*   **समय-अवधि नियंत्रक (Time Adjustments):** आप अपनी आवश्यकतानुसार फोकस लिमिट को 1 से 120 मिनट तक एडजस्ट कर सकते हैं या 15, 25, 45, या 60 मिनट के क्विक प्रिव्यू पिल्स से तुरंत सेट कर सकते हैं।
*   **कठोर ताला (Strict Bypass Lock Mode):** कठोर फोकस बनाए रखने और बिना पूरा समय हुए टाइमर को बाईपास करने से रोकने के लिए स्ट्रिक्ट मोड।
*   **फ्लोटिंग टाइमर (Floating Badge):** स्क्रीन पर अन्य ऐप्स के ऊपर भी लाइव टाइमर दृश्यमान रहता है ताकि आप ट्रैक रख सकें।

### 2. 📝 कार्य लिस्ट (Advanced Task Board Workspace)
*   **प्राथमिकता फ़िल्टर (Priority Filter Tags):** अपने कार्यों को उच्च (High🔴), मध्यम (Medium🟡), और निम्न (Low🔵) प्राथमिकता के आधार पर व्यवस्थित करें।
*   **श्रेणीकरण (Categorization):** 'Work', 'Study', 'Personal' आदि श्रेणियों के अनुसार कार्य व्यवस्थित करें।
*   **रियल-टाइम प्रोग्रेस बार (Real-Time Progress Bar):** होम स्क्रीन और टास्क बोर्ड पर कार्य पूर्णता दर (Completion Rate) को गतिशील रूप से ट्रैक करें।

### 3. 📅 प्रीमियम आदतें (Habit Tracker Workspace)
*   **इंटेलीजेंट स्क्रॉल कैलेंडर स्ट्रिप (Calendar Strip):** पूरे सप्ताह के दिनों को देखने और आदतों की रिपोर्ट दर्ज करने के लिए होरिज़ॉन्टल मोशन कैलेंडर।
*   **आदत की पूर्णता (Habit Completion & Logs):** आज की आदतों को पूरा चिह्नित करें और वास्तविक समय में अपनी दैनिक आदतों के रिकॉर्ड को अपडेट करें।
*   **दैनिक प्रेरणा और उदाहरण आदतें:** सुबह जल्दी उठना, नियमित व्यायाम और ध्यान जैसी प्री-लोडेड महत्वपूर्ण आदतें।

### 4. 🎵 ट्यून्स सेटिंग्स (Alert & Sound Engine)
*   **विभिन्न अलर्ट ट्यून्स (Custom Warning Tones):** ऐप में 5+ अलग-अलग अलर्ट ट्यून्स दी गयी हैं जिनका उपयोग सावधानी बीप्स और अलर्ट्स में होता है:
    1. CDMA Pip Alert (क्लासिक)
    2. Prompt Beep
    3. High Loud Alert (लचीला)
    4. Prop Beep Alert
    5. Abbr Alert
*   **वाइब्रेशन और साउंड टॉगल:** आप चाहें तो केवल साउंड या कम्पलीट साइलेंस मोड में डिवाइस फीडबैक को कस्टमाइज़ कर सकते हैं।

---

## 🛠️ Technology Stack (तकनीकी विनिर्देश)

*   **UI Framework:** Jetpack Compose (Modern Material Design 3 components)
*   **Architecture:** MVVM (Model-View-ViewModel) paired with Repository Pattern
*   **Local DB:** Room Database for secure and fast offline-first storage
*   **Language:** Kotlin and Coroutines & Flow for asynchronous state streaming
*   **Settings Engine:** Jetpack DataStore Preferences
*   **Background Services:** Background Screen-Time Regulatory and Toast Dispatch Service

---

## 📦 Project Directory Structure 📂

```text
/app/src/main/java/com/example/
│
├── MainActivity.kt                  # मुख्य एक्टिविटी (Unified Workspace Launcher Entry)
├── ScreenTimeService.kt             # बैकग्राउंड सर्विस (लगातार कठोर ताला और अलार्म नियंत्रित करने के लिए)
│
├── data/                            # डेटा लेयर (Room entity and settings Engine)
│   ├── AppDatabase.kt               # Room database configuration (Tasks & Habit logs)
│   ├── HabitRepository.kt           # Habit processing layer
│   ├── SettingsRepository.kt        # Jetpack DataStore based Preference persistence
│   └── UsageStatsHelper.kt          # Screen Time और ऐप्स के स्टैटिस्टिक्स डेटा सहायक
│
└── ui/                              # आधुनिक UI और स्क्रीन्स लेयर
    ├── UnifiedWorkspace.kt          # ऑल-इन-वन प्रो लेवल इंटरफ़ेस (सभी टूल्स एक जगह)
    └── theme/                       # प्रीमियम डार्क स्लेट कलर और टाइपोग्राफी कॉन्फ़िगरेशन
```

---

## 🚀 How to Run the App (रन करने की विधि)

1. इस प्रोजेक्ट को सुरक्षित रूप से अपने **Google AI Studio Workspace** या लोकल **Android Studio** में क्लोन करें।
2. Gradle Dependencies सिंक होने दें (`build.gradle.kts` automatically handles packages).
3. `compile_applet` या 'Run' बटन पर क्लिक करके एप्लिकेशन को लाइव एमुलेटर या अपने डिवाइस पर चलाएं।
4. ऐप के सभी प्रो टूल्स तथा इंस्टाग्राम माध्यम से डेवलपर प्रोफाइल `Jitendra Uno` ([@jitendrauno](https://instagram.com/jitendrauno)) से जुड़ने के लिए हेडर पर दिए एकाउंट आइकॉन का उपयोग करें।

---

## 🌟 Developer Credit (क्रेडिट)

*   **Designed & Developed by:** Jitendra (Instagram ID: [@jitendrauno](https://instagram.com/jitendrauno))
*   **Specialized in:** Advanced Android Development, Jetpack Compose, Premium App Design.
