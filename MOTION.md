# ABB-web animasiyaları

Mənbə: [Figma-da ABB-web](https://www.figma.com/design/Fcxn9wR4Z6WKKOHGDcWeku/ABB-web).
Keçidlər və variantlar 22 sentyabr 2026-cı ildə Plugin API `reactions` vasitəsilə oxunub.
Bu ekranlar üçün `get_motion_context` boş massiv qaytarıb: effektlər Motion əsas kadrları ilə deyil, prototip qarşılıqlı əlaqələri ilə təyin edilib.

| Effekt | Figma düyünləri | Həyata keçirilmə |
| --- | --- | --- |
| Cari mərhələnin halosu | 26:218 ↔ 26:217 | Şəffaflıq 0 ↔ 0.2, 1 ms gecikmə, hər istiqamətdə 400 ms ease-in-out; tam dövr 802 ms |
| Sənəd seçimi | 71:1228 → 46:1326 | 200 ms ease-in-out; kartlar DOM-da saxlanılır |
| Ekvivalentin açılması | 157:830 → 157:829 | 200 ms ease-out, məzmunun hündürlüyü və keçirici; təkrar klik cari hündürlükdən davam edir |
| FİN nədir? | 636:3568 → 636:3567 | Üzərinə gətirdikdə mavi mətn və işarənin 24 px-dən 26 px-ə böyüməsi; keçid mənbədə olduğu kimi anidir |
| Naviqasiya | 6:98, 6:34 və qonşu variantlar | 60% şəffaflığa malik #e6f1ff fon, ani |
| CVV daxil edilərkən kart | 351:630 ↔ 351:629 | Ön/arxa tərəfin mənbə şəkilləri, CVV sahəsinə fokuslandıqda dəyişmə və nömrə/müddət sahəsinə fokuslandıqda geri qayıtma; 1022.093773 ms |
| Beş status sətri | 419:1126, 433:909, 433:966, 433:1039, 442:1016 | 300 / 1500 / 2700 / 3900 / 5100 ms sonra başlanır, ardınca 300 ms intervalla dairə, başlıq, alt yazı və işarə dəyişir |

## Dəqiqlik məhdudiyyətləri

- Kart üçün API `GENTLE` yay adını və müddəti ötürüb, lakin fiziki parametrləri verməyib. Təxmini mass=1, stiffness=100, damping=15 yayı istifadə olunub. Onun əyrisinin 1:1 uyğunluğu təsdiqlənməyib. Bu, uydurulmuş 3D fırlanma olmadan şəkillərin hamar dəyişməsidir.
- Statuslarda Figma çox sərt fərdi yaylar təyin edir: mass=1, stiffness=108000000, damping=12000, müddət təxminən 1.24 ms-dir (bir keçid təxminən 1.228 ms). Saytda gecikmələr və vəziyyətlərin demək olar ki, ani dəyişməsi qorunub. Kadrdan daha qısa bu rəng keçidlərinin aralıq yay əyrisi təkrarlanmır.
- Statusların mətnləri, son rəngləri və işarələri nümayiş məlumatlarına uyğundur. Animasiya sənədi imzalanmış və ya səfirlik tərəfindən qəbul edilmiş elan etmir və saxlanmış sifarişi dəyişmir.
- Səhifələrin və modal pəncərələrin görünmə effektləri əlavə edilməyib: mənbə əlaqələrində belə keçidlərin əksəriyyəti anidir.

## Yoxlama

Playwright / Chromium: halonun tam dövrü 0/201/401/602/802 ms nöqtələrində yoxlanıb və 0/0.1/0.2/0.1/0 şəffaflıq nəticəsi alınıb. Açılmanın aralıq hündürlüyü, hər iki istiqamətdə sürətli keçid, kart tərəfinin dəyişməsi, 6100 ms-ə qədər bütün beş sətrin ardıcıllığı, arayış üçün tam sifariş, 320 px-də üfüqi daşmanın və JavaScript xətalarının olmaması yoxlanıb.

`prefers-reduced-motion` CSS və JS hərəkətlərini söndürür. İş zamanı parametr dəyişdirildikdə cari animasiyalar son vəziyyətinə çatdırılır; sahələr və statuslar əlçatan qalır. Səhifədən çıxarkən animasiyalar ləğv edilir.

Fayllar: `css/motion.css`, `js/motion.js`. Yeni asılılıq yoxdur. Kartın arxa tərəfi 351:591 düyünündən `assets/figma/payment-card-back.png` faylında lokal saxlanılıb.
