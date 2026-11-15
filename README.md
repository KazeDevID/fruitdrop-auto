# 🥝 FruitDrop - Yeetalk Fruit Merge Auto Bot

Aplikasi Android cerdas berbasis **Jetpack Compose** untuk memainkan game **Fruit Merge di Yeetalk** secara otomatis (*Auto-Drop*), real-time, dan presisi tinggi.

---

## 📸 Fitur Utama

- 🔍 **Deteksi Buah Real-Time Berkecepatan Tinggi**:
  - Menggunakan sampling warna HSV ultra-cepat (< 1ms per frame) untuk mendeteksi buah selanjutnya yang muncul di indikator lingkaran Yeetalk.
  - Memfilter warna latar langit dan border agar deteksi tidak pernah salah.
- 🎯 **Aturan Drop Presisi**:
  - **Kiwi Hijau** & **Buah Persik (Peach)**: Otomatis di-drop ke **Paling Kiri** (*Far Left Column*).
  - **Buah yang lebih kecil dari kiwi** (Blueberry ungu, Stroberi merah, Jeruk/Lemon): Otomatis di-drop ke **Paling Kanan** (*Far Right Column*).
  - Aturan arah drop dapat disesuaikan sesuka hati melalui tab *Aturan Buah*.
- 🪟 **Floating Window Overlay & Bubble Mode**:
  - **Panel Kontrol Penuh**: Tombol besar `● ON • AUTO AKTIF` / `○ OFF • AUTO MATI`, indikator buah terdeteksi, counter statistik `Kiri: X • Kanan: Y`, dan tombol toggle.
  - **Minimized Bubble**: Tombol melayang mini untuk kenyamanan saat bermain.
  - Dapat digeser ke posisi mana pun di layar.
- 📍 **Penanda Titik & Kalibrasi Visual ("KIRI" & "KANAN")**:
  - Garis putus-putus dan lingkaran panduan di layar agar pengguna dapat melihat dengan jelas jalur jatuh buah.
  - Lingkaran penanda dapat disentuh dan digeser langsung di atas game untuk menyelaraskan dengan kolom permainan di berbagai resolusi layar.
  - Target ring `SCAN BUAH` yang dapat digeser untuk memastikan area scan tepat sasaran.
- ⚡ **Ultra Fast Tap**:
  - Menggunakan Android Accessibility Service (`dispatchGesture`) dengan durasi klik instan (35ms) tanpa lag atau delay.
  - Dilengkapi pengatur kecepatan slider (100 ms hingga 800 ms).

---

## 🚀 Panduan Penggunaan

1. **Buka Aplikasi FruitDrop**.
2. **Aktifkan Izin Sistem**:
   - Izin Floating Window (*Display over other apps*).
   - Layanan Aksesibilitas *FruitDrop*.
3. Tekan tombol **"Mulai Floating Overlay"** dan setujui izin rekam layar saat diminta.
4. Buka aplikasi **Yeetalk** dan masuk ke room permainan **Fruit Merge**.
5. Pastikan lingkaran **KIRI**, **KANAN**, dan **SCAN BUAH** sejajar dengan kolom game (geser jika perlu).
6. Tekan tombol **"● ON • AUTO AKTIF"** pada panel melayang.
7. Bot akan mendeteksi buah yang muncul dan menjatuhkannya secara otomatis!

---

## 📦 Unduh APK melalui GitHub Actions

Aplikasi ini dilengkapi dengan **GitHub Actions Workflow** otomatis:
1. Buka tab **Actions** di repositori GitHub ini.
2. Pilih workflow **Build & Release FruitDrop APK**.
3. Unduh artifact `FruitDrop-Yeetalk-AutoDrop-APK` yang berisi file `FruitDrop-Auto-v1.0.apk`.
4. Pasang (install) di perangkat Android Anda!
