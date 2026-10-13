# BT Terminal

HC-05 / HC-06 Bluetooth modulleri icin Android kontrol uygulamasi.

## Ozellikler

- **Terminal:** cift yonlu seri monitor. Yazi yaz, gonder, gelen veriyi anlik izle. Satir sonu (`\n`) secenegi.
- **Keypad (tek ekran, secmeli):** `4x4` (`1-9`, `*`, `0`, `#`, `A-D`), `3x3` (`1-9`) veya `Ozel` modu. Basilan tusun uzerindeki karakter Bluetooth uzerinden gonderilir, basimda tik animasyonu.
- **Ozel Butonlar:** keypad ekraninin `Ozel` modunda kendi butonlarini ekle (ad + gonderilecek deger), uzun basarak gonder/sil.
- **Baglanti:** eslesmis cihaz listesinden sec, baglan, tek tusla baglantiyi kes.
  Baglanirken sirayla 3 yontem dener (guvenli SPP, guvensiz SPP, kanal-1);
  HC-05/HC-06 ile uyumsuzluk cikarsa otomatik digerine gecer.
- **Otomatik guncelleme:** acilista sessizce yeni surumu denetler, varsa tek dokunusla
  indirip kurar. Ust bardaki `⟳` ile elle de denetlenebilir.

## Kullanim (HC-05 / HC-06)

1. Oncelikle telefonun **Bluetooth ayarlarindan** modulle esles (varsayilan PIN cogunlukla `1234` veya `0000`).
2. Uygulamada **Baglan** -> listeden modulu sec.
3. Arduino tarafinda `Serial` (HC-05/06 varsayilan 9600 baud) ile oku. Ornek:
   ```cpp
   void setup() { Serial.begin(9600); }
   void loop() {
     if (Serial.available()) {
       char c = Serial.read();
       // c: '1', 'A', '#' ... tus degeri
     }
   }
   ```

## Gelistirme

- Dil: Kotlin, minSdk 26, tek `app` modulu.
- Derleme: `gradle assembleDebug` (yerelde Android SDK gerekir).
- GitHub Actions her `main` push'inda derler; `v*` tag'inde APK'yi otomatik **Release**'e ekler.

## Izinler

- Android 12+: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (calisma aninda istenir).
- Android 6-11: `ACCESS_FINE_LOCATION` (eslesmis liste icin).
