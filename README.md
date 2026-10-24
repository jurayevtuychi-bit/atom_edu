# Atom fizikasi — Android ilovasi

https://www.tabiiy-fanlar.uz/ saytini ilova ichida ochadigan WebView ilovasi.

Sayt menyulari ilovaning yuqorisida kichik tugmalar qatori ko'rinishida turadi: Bosh sahifa, Tadqiqotchi, Kreativ dars, Virtual amaliyot, Motiv, Bilimni baholash, Amaliy mashg'ulot. "Tadqiqotchi" va "Virtual amaliyot" bosilganda ularning ichki bo'limlari (Darslik, Video darslik, Viktorina va h.k.) ikkinchi qatorda paydo bo'ladi. Joriy sahifaga mos tugma to'q rangda ajralib turadi. Qatorlarni barmoq bilan chapga-o'ngga surish mumkin.

Boshqa imkoniyatlar: yuklanish chizig'i, "orqaga" tugmasi, internet yo'q bo'lganda "Qayta urinish" ekrani, videolar uchun to'liq ekran rejimi, sayt tashqarisidagi havolalarni brauzerda ochish.

## APK olish — 1-usul: GitHub orqali

1. github.com da yangi repozitoriy yarating.
2. Shu papkadagi barcha fayllarni (`.github` papkasi bilan birga) repozitoriyga yuklang.
3. **Actions** bo'limida "Build APK" jarayoni o'zi ishga tushadi (3–6 daqiqa). Ishga tushmasa, **Run workflow** tugmasini bosing.
4. Tugagach, jarayon sahifasining pastidagi **Artifacts** bo'limidan `atom-fizikasi-apk` ni yuklab oling. Ichida `app-debug.apk` bor.

## APK olish — 2-usul: Android Studio

1. Shu papkani **Open** qiling va Gradle sinxronlanishini kuting.
2. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. APK `app/build/outputs/apk/debug/app-debug.apk` manzilida bo'ladi.

## Telefonga o'rnatish

`app-debug.apk` ni telefonga yuboring va oching. "Noma'lum manbalardan o'rnatish" so'ralsa, ruxsat bering. Talab: Android 8.0 va undan yuqori.

## Sozlash

- Menyu tugmalari: `MainActivity.java` ichidagi `MENU` ro'yxati (nomi va sayt ichidagi yo'li). Saytga yangi bo'lim qo'shsangiz, shu yerga ham qo'shing.
- Tugma o'lchami: `makeChip` ichidagi `setTextSize(..., 12)` va `setPadding(...)`.
- Ranglar: `res/values/colors.xml`. Ilova nomi: `res/values/strings.xml`.
- Ilova ichida ochiladigan domenlar: `INTERNAL_HOSTS`. Boshqa domenlar brauzerda ochiladi.
- Ikonka: `res/drawable/ic_launcher_foreground.xml`.
