# Demo məlumatları və işə salma

Bu fayl developer və nümayiş aparıcısı üçün vahid məlumat kitabçasıdır. Aşağıdakı bütün FIN kodları, telefonlar, hesablar, kartlar, sifarişlər və giriş məlumatları uydurmadır və yalnız təqdimat prototipi üçün nəzərdə tutulub. İnterfeys bu ipuclarını avtomatik göstərmir.

## Sürətli nümayiş ssenarisi

| Daxil ediləcək məlumat | Dəyər |
| --- | --- |
| GitHub Pages və `npm run dev` üçün FIN | `ABC1234` |
| `npm run dev:full` üçün FIN | aşağıdakı cədvəldəki 11 koddan hər hansı biri |
| OTP | `123456` |
| Uğurlu test kartı | `4242 4242 4242 4242` |
| İmtina edilən test kartı | `4000 0000 0000 0002` |
| Kartın son istifadə tarixi | `12/30` |
| CVV / CVC | `123` |

FIN və OTP-ni bütöv şəkildə ilk xanaya yapışdırmaq olar. Ödəniş imtinasından sonra kart nömrəsini uğurlu nömrə ilə əvəz edin və CVV-ni yenidən daxil edin: ödəniş göndərildikdə CVV xanası təmizlənir.

## İş rejimləri

| Rejim | Əmr və ya ünvan | Mövcud FIN-lər | Sifarişlərin saxlanması |
| --- | --- | --- | --- |
| GitHub Pages | `https://kar1m0vf.github.io/abb-digital-documents-demo/` | yalnız `ABC1234` | brauzerin `localStorage` yaddaşı |
| Lokal frontend | `npm run dev` | yalnız `ABC1234` | brauzerin `localStorage` yaddaşı |
| Frontend və Java backend | `npm run dev:full` | bütün 11 FIN | fayl əsaslı H2 bazası |
| Tam işəsalmada məcburi avtonom rejim | `http://localhost:4173/?mode=demo` | yalnız `ABC1234` | brauzerin `localStorage` yaddaşı |

Tam işəsalma üçün Node.js 18+ və JDK 21+ tələb olunur. Docker və PostgreSQL lazım deyil. İlk işəsalma Gradle və Maven asılılıqlarını endirir. `Ctrl+C` frontend və backend-i dayandırır.

Tam işəsalmanın faydalı ünvanları:

- müştəri interfeysi: `http://localhost:4173/#/documents`;
- səfirlik paneli: `http://localhost:4173/#/embassy`;
- backend sağlamlıq yoxlaması: `http://127.0.0.1:8080/api/v1/demo/health`;
- Swagger UI: `http://127.0.0.1:8080/swagger-ui/index.html`;
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`.

Portları `PORT` və `BACKEND_PORT` dəyişənləri ilə dəyişmək olar. Frontend-ə şəbəkədən giriş üçün `HOST=0.0.0.0` təyin edin və ya `127.0.0.1:4173` qarşısında Nginx/Caddy yerləşdirin. Təqdimat profilində Java backend `127.0.0.1` ünvanında qalır və brauzer ona frontend-in daxili proksisi vasitəsilə müraciət edir.

## Bütün 11 aktiv FIN

`ABC1234` hər iki rejimdə işləyir. Digər on FIN yalnız `npm run dev:full` ilə işləyir.

| № | FIN | Müştəri | İnterfeysdə telefon | Backend məhsulları |
| ---: | --- | --- | --- | --- |
| 1 | `ABC1234` | Aydan Əhədova | `+994 50 *** ** 00` | 7 təqdimat məhsulu; aşağıdakı cədvələ baxın |
| 2 | `5D7X9Q2` | Aydan Ahadova | `+994 50 *** ** 82` | Visa · 4581 və hesab · 2156, AZN 12500.50; Mastercard · 7788 və hesab · 2157, USD 3400.00 |
| 3 | `6F8A1B3` | Elvin Məmmədov | `+994 55 *** ** 80` | Visa · 3321 və əmanət · 2158, AZN 820.75 |
| 4 | `2K9C4D7` | Nigar Əliyeva | `+994 70 *** ** 14` | Mastercard · 9014 və hesab · 2159, EUR 2150.00 |
| 5 | `7H2E5F1` | Rəşad Quliyev | `+994 50 *** ** 41` | Visa · 6642 və hesab · 2160, AZN -300.25 |
| 6 | `3J6T8G4` | Leyla Hüseynova | `+994 55 *** ** 50` | Visa · 4950 və əmanət · 2161, AZN 9999.99 |
| 7 | `8K4P6S2` | Tural Hüseynov | `+994 50 *** ** 11` | Visa · 2163 və əmanət · 2163, AZN 4520.10 |
| 8 | `9M2R7T4` | Samir Nəsirov | `+994 50 *** ** 22` | Visa · 2164 və hesab · 2164, AZN 3100.00 |
| 9 | `4N6Q9W1` | Fidan Abbasova | `+994 55 *** ** 55` | Visa · 2165 və hesab · 2165, AZN 7800.55 |
| 10 | `1L5H3J8` | Orxan Qəhrəmanov | `+994 70 *** ** 77` | Visa · 2166 və hesab · 2166, AZN 990.25 |
| 11 | `7Z2C5V9` | Günay Məmmədli | `+994 51 *** ** 00` | Visa · 2167 və əmanət · 2167, AZN 6050.40 |

Mənbə seed məlumatlarındakı kart tarixləri: 4581 — `05/26`, 7788 — `09/25`, 3321 — `11/26`, 9014 — `03/26`, 6642 — `07/25`, 4950 və 2163–2167 kartları — `12/30`. Bu tarixlər məhsul metadatasıdır və sənəd ödənişi formasında istifadə edilmir. Ödəniş üçün “Sürətli nümayiş ssenarisi” bölməsindəki ayrıca test nömrələrindən istifadə edin.

## ABC1234 profilinin məhsulları

| Daxili ID | Məhsul | Son rəqəmlər | Valyuta | Balans | Mövcudluq |
| --- | --- | ---: | --- | ---: | --- |
| `visa-azn` | Tam Visa | 7575 | AZN | 2450.80 | mövcuddur |
| `master-azn` | Tam Mastercard | 4581 | AZN | 680.25 | mövcuddur |
| `visa-usd` | Visa USD | 9032 | USD | 1200.00 | mövcuddur |
| `credit` | Kredit kartı | 1084 | AZN | -350.00 | maketə əsasən seçim bağlıdır |
| `account-azn` | Cari hesab | 2156 | AZN | 5230.50 | mövcuddur |
| `account-usd` | Cari hesab | 3860 | USD | 3400.00 | mövcuddur |
| `account-eur` | Cari hesab | 6241 | EUR | 1850.00 | mövcuddur |

Kart və hesablar daxil olmaqla ümumilikdə ən çox üç məhsul seçilə bilər. Balanslar başlanğıcda gizlidir. Profilin hesab nömrələri `AZ•• •••• •••• •••• •••• NNNN` maskası ilə göstərilir.

## OTP və giriş xətaları

### Avtonom rejim

- düzgün OTP: `123456`;
- səhv OTP nümunəsi: `000000`;
- etibarlılıq müddəti: 5 dəqiqə;
- ən çox 5 səhv cəhd;
- təkrar göndərmə 60 saniyədən sonra mümkündür;
- uğurla istifadə edilmiş OTP təkrar istifadə oluna bilməz.

### Java backend

- bütün 11 FIN üçün düzgün OTP: `123456`;
- etibarlılıq müddəti: 63 saniyə;
- demo versiyada server tərəfli səhv cəhd sayğacı yoxdur;
- yeni OTP sorğusu əvvəlki kodu əvəz edir;
- uğurla istifadə edilmiş OTP təkrar istifadə oluna bilməz;
- müştəri tokeni 1 saat qüvvədədir və yalnız tabın yaddaşında saxlanır.

Kodun vaxtı bitərsə, `Yenidən göndər` düyməsini basın və yenidən `123456` istifadə edin.

## Ödəniş

| Ssenari | Nömrə | Tarix | CVV |
| --- | --- | --- | --- |
| Uğurlu ödəniş | `4242 4242 4242 4242` | `12/30` | `123` |
| İmtina edilən ödəniş | `4000 0000 0000 0002` | `12/30` | `123` |

Forma Luhn alqoritmini, qüvvədə olan son istifadə tarixini və üçrəqəmli CVV-ni yoxlayır. Java backend-ə PAN/CVV deyil, yalnız demo yoxlamasının nəticəsi ötürülür. Real pul silinmir.

## Səfirlik paneli və portal API

Hazırkı interfeysin paneli avtorizasiyasız (demo rejimi) və ya real portal girişi ilə açılır:

```text
http://localhost:4173/#/embassy
```

`npm run dev` ilə panel dərhal açılır. `npm run dev:full` (presentation profili) real portal API-nə qoşulur və giriş forması göstərir; məlumatlar yalnız giriş etmiş səfirliyə aid olur.

Mənbə portal API-ni birbaşa yoxlamaq üçün backend-də demo hesabı var:

| Sahə | Dəyər |
| --- | --- |
| İstifadəçi adı | `admin@italy` |
| Şifrə | `demo1234` |
| Rol | `ADMIN` |
| Səfirlik | İtaliya |

Altı hesab da mövcuddur: `admin@france`, `admin@usa`, `admin@germany`, `admin@spain`, `admin@uk` — eyni şifrə ilə.

Seed/API-də altı səfirlik var: İtaliya, Fransa, ABŞ, Almaniya, İspaniya və Böyük Britaniya. İstifadəçi interfeysində İtaliya, Fransa, ABŞ, Almaniya və Böyük Britaniya mövcuddur; İspaniya məlumatlarda saxlanılıb, lakin seçimdə gizlədilib.

## Sənədlər və hesablamalar

- çıxarış `statement`: **5 AZN**;
- arayış `reference`: **10 AZN**;
- komissiya: **0 AZN**;
- seçilmiş məhsulların sayından asılı olmayaraq bir birləşdirilmiş nüsxə;
- sənəd dilləri: Azərbaycan və ingilis;
- çıxarışın təyinatı: səfirlik, şəxsi istifadə və ya başqa qurum;
- dövrlər: 1, 3, 6, 12 ay və ya xüsusi tarix aralığı;
- əməliyyatlar: hamısı, mədaxil və ya məxaric;
- sənəddə sabit məzənnələr: 1 USD = **1.7 AZN**, 1 EUR = **1.9 AZN**, 1 GBP = **2.2 AZN**;
- yeni sifariş nömrəsi: `AR-İL-XXXXXXXX`;
- interfeys statusları: `pending`, `completed`, `rejected`.

## Çıxarışın demo əməliyyatları

Bütün məhsullar üçün eyni əməliyyat dəsti istifadə olunur. Məbləğlər seçilmiş məhsulun valyutası ilə göstərilir, tarixlər sənəd tarixindən hesablanır.

| Neçə gün əvvəl | Növ | Məbləğ | Təsvir |
| ---: | --- | ---: | --- |
| 5 | məxaric | 42.50 | kartla alış |
| 12 | mədaxil | 1500.00 | əməkhaqqı |
| 22 | məxaric | 85.00 | kommunal ödəniş |
| 45 | mədaxil | 300.00 | hesaba köçürmə |
| 80 | məxaric | 124.90 | kartla alış |
| 140 | mədaxil | 500.00 | hesaba köçürmə |
| 220 | məxaric | 65.00 | xidmət ödənişi |
| 320 | mədaxil | 200.00 | hesaba köçürmə |

## Səfirlik panelinin ilkin qeydləri

Demo rejimdə ilkin sətirlər İtaliyaya aiddir, ingilis dilindən və demo məhsulundan istifadə edir. Presentation rejimində eyni sətirlər real portal API-dən gəlir (`AR-2026-…` nömrələri ilə), tam işəsalmada isə yeni server sifarişləri bu siyahıya əlavə olunur.

| Nömrə | Müştəri | Sənəd | Status | Tarix |
| --- | --- | --- | --- | --- |
| `AR-2024-000512` | Aydan Əhədova | arayış | completed | 2024-05-31 |
| `AR-2024-000489` | Elvin Məmmədov | çıxarış | pending | 2024-05-28 |
| `AR-2024-000471` | Tural Hüseynov | arayış | pending | 2024-05-25 |
| `AR-2024-000125` | Nigar Əliyeva | arayış | completed | 2024-05-28 |
| `AR-2024-000124` | Rəşad Quliyev | çıxarış | rejected | 2024-05-27 |
| `AR-2024-000118` | Leyla Həsənova | arayış | completed | 2024-05-26 |
| `AR-2024-000103` | Samir Nəsirov | çıxarış | pending | 2024-05-24 |
| `AR-2024-000098` | Fidan Abbasova | arayış | completed | 2024-05-23 |
| `AR-2024-000091` | Orxan Qəhrəmanov | arayış | rejected | 2024-05-22 |
| `AR-2024-000087` | Günay Məmmədli | arayış | pending | 2024-05-21 |

## Məlumatların saxlanması və sıfırlanması

### Avtonom rejim

Sifariş və statuslar cari origin-in `localStorage` yaddaşında saxlanır:

- `abb-bda-demo-orders-v1`;
- `abb-bda-demo-statuses-v1`.

Avtonom nümayişi tam sıfırlamaq üçün brauzerdə sayt məlumatlarını silin. FIN, OTP və kart rekvizitləri `localStorage`-a yazılmır. Avtorizasiya və tamamlanmamış sifariş səhifə yeniləndikdə sıfırlanır.

### Tam işəsalma

Yeni sifariş və statuslar burada saxlanır:

```text
backend/data/presentation.mv.db
```

Onlar müxtəlif brauzerlərdən əlçatandır və yenidən başladıqdan sonra qalır. Tam sıfırlama üçün serverləri dayandırın və `backend/data/presentation*` fayllarını silin; növbəti işəsalmada sxem və seed məlumatları yenidən yaradılacaq.

## Nümayiş məhdudiyyətləri

- SMS göndərilmir;
- real bank API-ləri qoşulmayıb;
- pul silinmir;
- PDF rəsmi bank sənədi deyil, nümunədir;
- elektron imza və sənədin səfirliyə çatdırılması yerinə yetirilmir;
- təqdimat frontend-inin paneli qəsdən girişsiz açıqdır;
- hazırkı profil real məlumatlarla açıq istifadəyə deyil, lokal və ya qapalı nümayişə hesablanıb.

`index.html` faylında `noindex, nofollow` var, lokal Node serveri isə əlavə olaraq `X-Robots-Tag` qaytarır. Bu, indekslənməni məhdudlaşdırır, lakin qapalı nümayiş üçün şifrəni və ya şəbəkə məhdudiyyətini əvəz etmir.
