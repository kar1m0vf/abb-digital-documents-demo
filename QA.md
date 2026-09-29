# ABB BDA yoxlaması

Etalon: sifariş ekranları, köməkçi pəncərələr və səfirlik paneli olan bir böyük vərəqdən ibarət təqdim edilmiş `ABB.pdf`. Mənbə kodu: istifadəçinin göndərdiyi HTML və JavaScript. İlkin Figma faylına giriş olmayıb; istifadəçinin icazə verdiyi PDF-dən istifadə edilib.

## Avtomatik yoxlananlar

- JS modullarının sintaksisi və lokal resurs keçidləri.
- Hər iki sənəd növü və bütün mərhələlər üçün HTML generasiyası.
- 31 martdan 28 fevrala keçid daxil olmaqla tarix aralıqlarının hesablanması.
- Test kartlarının və ay sərhədində son istifadə tarixinin yoxlanması.
- Səhv OTP, uğurlu təsdiq və təkrar istifadənin rədd edilməsi.
- Paralel sifariş göndərişlərinin idempotentliyi və imtina edilmiş ödənişdən sonra təkrar cəhd.
- Sifarişin saxlanması, dublikatların olmaması, səfirlik panelində göstərilməsi və status dəyişikliyi.
- Nəticəsiz axtarış, sərbəst mətnin təhlükəsiz göstərilməsi və zədələnmiş JSON.
- Demo əməliyyatlarının növ və dövr üzrə süzülməsi.

## Mənbə koda əsasən etalona uyğunluq

| Element | Həll |
| --- | --- |
| Palitra | Açıq soyuq fon, ağ səthlər, dolğun mavi və tamamlanmış mərhələlərin yaşıl rəngi qorunub |
| Kompozisiya | Başlıq, naviqasiya zənciri, səhifə başlığı, mərhələ göstəricisi və forma; əsas konteyner 1156 px |
| Sənəd kartları | Geniş ekranda iki sütun, qiymət və radio düyməsi; mobil versiyada bir sütun |
| Hesablar və detallar | Kart/hesab qrupları, balans idarəsi və hər seçim üçün ayrıca parametr bloku |
| Baxış və ödəniş | Miniatür, ipucu, baxış, təsdiq, yekun və test kartı xanaları |
| Səfirlik paneli | Tünd yan panel, axtarış, dörd sayğac, tablar, cədvəl və statuslar |
| Resurslar | PDF-dən real şəkillər və Lucide xidmət ikonları |
| Tipoqrafika | Əlçatan olmayan ilkin veb şrift əvəzinə sistem şrift dəsti |

Bu, struktur və kod müqayisəsidir, **render edilmiş saytın vizual müqayisə hesabatı deyil**.

## Şüurlu təkmilləşdirmələr

- Təkrarlanan və uyğun gəlməyən mərhələ adları düzəldilib: sənəd → alıcı/səfirlik → hesablar → detallar → yoxlama → ödəniş → təsdiq.
- Mövcud JS-də ilk sənəd bağlı idi. Hər iki qol həyata keçirilib; çıxarış üçün şəxsi istifadə və başqa alıcı əlavə olunub.
- OTP-dən sonrakı ssenari artıq dövrə düşmür.
- Kart tarixinin sabit DD/MM görünüşü düzgün MM/YY formatı ilə əvəz edilib; interfeysdə AA/İİ göstərilir.
- Azərbaycan mətnindəki aşkar yazı xətaları düzəldilib və iki sənədin təsvirləri fərqləndirilib.
- Layihə sahibinin tələbi ilə xidmət qeydləri interfeysdən və PDF-dən çıxarılıb. Yoxlama dəyərləri və texniki məhdudiyyətlər `DEMO_DATA.md` faylına keçirilib.
- İşləməyən dekorativ sayğac və səhifələr hesablanan məlumatlar və real səhifələmə ilə əvəz edilib.
- Mövcud olmayan mobil versiya üçün menyu, yığcam göstərici, sətir keçidləri və cədvəl sürüşdürməsi əlavə olunub.

## İnterfeys yeniləndikdən sonra brauzer yoxlaması

19 sentyabr 2026: lokal server və Playwright; 1440×1000, 390×844 və 320×740 ölçüləri.

- Avtomatik fokus zamanı başlıqlar çərçivəsiz göstərilir.
- Klik zamanı düymə və kartlarda fokus çərçivəsi yaranmır; Tab görünən fokusu saxlayır.
- Klaviaturadan siçana keçid və modal pəncərə bağlandıqdan sonra fokusun geri qayıtması yoxlanılıb.
- Böyük Britaniya və Azərbaycan bayraqları lokal SVG-dir, geniş və dar ekranda yüklənir.
- Yoxlanmış dil seçim formalarında 320 px enində səhifənin üfüqi daşması yoxdur.
- Çıxarış sifarişi tam keçilib: FIN → OTP → hesab seçimi → detallar → baxış → ödəniş imtinası → uğurlu təkrar → təsdiq → sifariş siyahısı → səfirlik paneli.
- Məlumatlar `DEMO_DATA.md` üzrə əl ilə daxil edilir; xidmət ipucları və avtomatik doldurma düymələri çıxarılıb.
- PDF `ABB-document.pdf` kimi yüklənir; PDF kitabxanası ilə faylın oxunaqlılığı, səhifə sayı və metadatada xidmət qeydlərinin olmaması yoxlanılıb. Önizləmə mətni DOM-da yoxlanılıb.
- Tətbiq konsolunda xəta və xəbərdarlıq yoxdur.
- Hazırkı 18 avtomatik test keçir; onlar əlavə olaraq şablonlarda xidmət mətninin olmamasını və köhnə sifariş nömrələrinin statuslarla uyğunluğunu yoxlayır.

Çap, digər brauzerlər və ilkin PDF maketinə dəqiq vizual uyğunluq ayrıca yoxlanmayıb. Real bank inteqrasiyaları yoxdur; məhdudiyyətlər `DEMO_DATA.md` faylında təsvir olunub.
