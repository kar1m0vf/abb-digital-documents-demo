# ABB — Rəqəmsal Sənəd Sifarişi

Bu, **HTML, CSS və saf JavaScript** üzərində qurulmuş BDA nümayiş layihəsidir. İnterfeys ilkin `ABB.pdf` prototipi əsasında [Figma-dakı ABB-web](https://www.figma.com/design/gkOJILonA6fhtwvTC8CQdf/ABB-web?node-id=0-1) faylından hazırlanıb. Müştəri interfeysi Azərbaycan, səfirlik paneli isə ingilis dilindədir. Animasiyaların mənbələri, yoxlamalar və dəqiqlik məhdudiyyətləri [MOTION.md](MOTION.md) faylında təsvir olunub.

## İşə salma

**Backend ilə:** Node.js 18+ və JDK 21+ tələb olunur. Aşağıdakı əmri icra edin və **http://localhost:4173** ünvanını açın:

```bash
npm run dev:full
```

Əmr saytı, Spring Boot backend-i və lokal fayl əsaslı H2 bazasını işə salır; Docker tələb olunmur. İlk işəsalma asılılıqları endirir. Hər iki prosesi `Ctrl+C` ilə dayandırmaq olar. Mənbə backend-in on standart FIN-i, əlavə `ABC1234` profili, OTP və kart məlumatları [DEMO_DATA.md](DEMO_DATA.md) faylında verilib. Ətraflı backend qeydləri: [backend/UPSTREAM.md](backend/UPSTREAM.md).

**Backend olmadan avtonom nümayiş:** Node.js 18 və ya daha yeni versiya tələb olunur. Layihə qovluğunda icra edin:

```bash
npm run dev
```

Sonra **http://localhost:4173/#/documents** ünvanını açın. İşə salmaq üçün npm paketlərini ayrıca quraşdırmaq lazım deyil. Alternativ olaraq `index.html` üçün VS Code Live Server istifadə edilə bilər. Hazır statik nümayiş də [GitHub Pages](https://kar1m0vf.github.io/abb-digital-documents-demo/#/documents) üzərində açıqdır.

`index.html` faylını iki dəfə klikləməklə açmayın: mənbə kodu HTTP server tələb edən ES modullarından istifadə edir.

## İki dəqiqədə yoxlama

1. `Hesabdan çıxarış` və ya `Səfirliyə arayış` seçin, sonra `Davam et` düyməsini basın.
2. Səfirliyi və dili seçin. Çıxarış üçün şəxsi istifadə və başqa qurum variantları da mövcuddur.
3. [DEMO_DATA.md](DEMO_DATA.md) faylındakı FIN və OTP-ni daxil edin. Kodu bütöv şəkildə ilk xanaya yapışdırmaq olar.
4. Bir və ya bir neçə hesab/kart seçin. Kredit kartı maketdə nəzərdə tutulduğu kimi seçim üçün bağlıdır.
5. Hər hesab üçün dili və ekvivalent valyutanı ayrıca seçin. Çıxarışda dövr, xüsusi tarix aralığı və əməliyyat növü də seçilə bilər.
6. Sənədi açın və məlumatları yoxladığınızı təsdiqləyin.
7. [DEMO_DATA.md](DEMO_DATA.md) faylındakı kartı, son istifadə tarixini və CVV-ni daxil edib ödəniş düyməsini basın. İmtina ssenarisi üçün həmin fayldakı ikinci kart nömrəsini istifadə edin, sonra uğurlu nömrə ilə ödənişi təkrarlayın.
8. Sifariş `Sifarişlərim` bölməsində görünəcək. Səhifənin aşağısındakı `Səfirlik paneli` keçidini və ya **http://localhost:4173/#/embassy** ünvanını açın.
9. Paneldə uyğun səfirliyi seçin, sifariş nömrəsini tapın və statusu dəyişin. Yeni status müştərinin siyahısında da görünəcək.

## Həyata keçirilən funksiyalar

- Razılaşdırılmış yeddi mərhələ; FIN və OTP ikinci mərhələnin alt ekranlarıdır.
- Hər iki sənəd növü, seçimləri saxlayan geri keçid və məcburi məlumat olmadan irəliləmənin bloklanması.
- FIN/OTP yoxlaması, kodun yapışdırılması, xanalar arasında keçid, Enter/Backspace/ox düymələri, təkrar göndərmə taymeri və səhv OTP limiti.
- Bir neçə məhsulun seçilməsi, balansın gizlədilməsi və hər məhsul üçün ayrıca parametrlər.
- Seçilmiş parametrlər əsasında sənədin yaradılması; demo əməliyyatları dövrə və istiqamətə görə real şəkildə süzülür.
- Sənədə modal pəncərədə baxış, lokal PDF yükləmə və çap. PDF Azərbaycan mətnini dəstəkləyir. Xidmət qeydləri və test ipucları interfeysdən və PDF-dən çıxarılıb; məlumat və məhdudiyyətlər `DEMO_DATA.md` faylındadır.
- Test kartının Luhn alqoritmi, son istifadə tarixi və CVV üzrə yoxlanması; uğurlu və imtina edilmiş ödəniş, yüklənmə vəziyyəti və dublikatlardan qorunma.
- Sifariş və ödəniş tarixçəsi, bildirişlər, hesab və kart səhifələri, demo istifadəçi profili.
- Səfirlik paneli: ad/nömrə üzrə axtarış, status filtrləri, sayğaclar, səhifələmə, baxış və status dəyişikliyi. Göstəricilər məlumatlardan hesablanır.
- Demo sifariş və statuslarının `localStorage`-da saxlanması, eyni origin daxilində tablararası sinxronizasiya. FIN, OTP və kart məlumatları saxlanmır.
- Mobil naviqasiya, adaptiv mərhələ göstəricisi, çevik formalar, üfüqi sürüşdürülən cədvəl, klaviatura fokusları və `reduced-motion` dəstəyi.
- Bütün resurslar lokaldır; Tailwind CDN və xarici şrift yüklənməsi tələb olunmur.

## Struktur

| Fayl | Təyinat |
| --- | --- |
| `index.html` | Ümumi HTML karkası |
| `css/styles.css` | Dizayn tokenləri, komponentlər, mobil qaydalar və çap |
| `css/figma.css` | Figma ölçüləri, tipoqrafika və görünüş |
| `css/motion.css`, `js/motion.js` | Animasiyalar və Figma interaktiv variantları |
| `css/select.css`, `js/select.js` | Sifariş və səfirlik paneli üçün Figma maketinə uyğun açılan siyahılar |
| `js/app.js` | Naviqasiya, hadisələr və mərhələ keçidləri |
| `js/store.js` | Sifariş vəziyyəti və demo məlumatlarının saxlanması |
| `js/data.js` | Sənədlər, tariflər, səfirliklər və test hesabları |
| `js/components.js` | Başlıq, mərhələ göstəricisi, düymələr və ümumi elementlər |
| `js/views/` | Müştəri, sənəd və səfirlik panelinin ayrı ekranları |
| `js/services/api.js` | FIN/OTP/ödəniş üçün avtonom demo adapteri |
| `js/services/transactions.js` | Demo əməliyyatları və süzgəc |
| `js/services/pdf.js` | Lokal PDF yaradılması; kitabxana tələb olduqda yüklənir |
| `js/modal.js` | Modal pəncərələr və bildirişlər |
| `tests/` | Məntiq və ekran generasiyası testləri |

## Qoşulmuş nümayiş backend-i

Tam işəsalmada FIN/OTP, hesabların alınması, sifarişlərin yaradılması və tarixçəsi, həmçinin yeni sifarişlərin status dəyişikliyi server vasitəsilə işləyir. Sifarişlər yenidən başladıqdan sonra `backend/data/` qovluğunda qalır. HTML, CSS, mətnlər və ekranların ardıcıllığı dəyişdirilməyib. PDF və çıxarış əməliyyatları lokal demo məlumatları olaraq qalır.

`js/services/api-client.js` avtonom və ya server adapterini seçir; `backend-api.js` API cavablarını mövcud interfeysin formatına çevirir. Tam işəsalma zamanı **http://localhost:4173/?mode=demo** avtonom rejimi açır. Rejim səhifə yüklənəndə sabitlənir: API nasazlığı lokal “uğurlu” ödəniş yaratmır. GitHub Pages Java işə salmır və orada avtonom demo istifadə olunur.

## Real bank inteqrasiyasına keçid

Hazırkı versiya prototipdir, bank inteqrasiyası deyil. O, SMS göndərmir, pul silmir, sənədləri imzalamır və səfirliyə ötürmür. Demo paneli qəsdən açıqdır və avtorizasiyalı kabinet sayılmır.

`js/services/api.js` adapterini komandanın API sorğuları ilə əvəz edin. Şəxsiyyət və OTP yoxlaması, sessiyalar, hesab və sifarişlərə giriş nəzarəti, tarif və valyuta ekvivalentinin hesablanması, idempotent sifariş yaradılması və statuslar serverdə olmalıdır. Demo hesab və əməliyyatları server məlumatları ilə əvəz edilməlidir. Müştəri tərəfdəki yoxlama server yoxlamasını əvəz etmir.

Ödəniş inteqrasiyasında test xanalarını ödəniş provayderinin və ya bankın forması ilə əvəz edin və yalnız token ötürün. Real PAN/CVV qəbulunu `localStorage`-a və ya bu demo adapterinə keçirməyin. Rəsmi PDF-in verilməsi, imzalanması və çatdırılması serverdə yerinə yetirilməlidir; burada yalnız nümunə yaradılır.

Demo tarifində çıxarış 5 AZN, seçilmiş hesabların sayından asılı olmayaraq birləşdirilmiş arayış isə 10 AZN-dir. Bu yanaşma bir nüsxəli maketə əsaslanır; real inteqrasiya zamanı tarif komanda ilə təsdiqlənməlidir. Valyuta məzənnələri sabit və test məlumatı kimi işarələnib.

## Yoxlamalar və məhdudiyyətlər

```bash
npm test
```

Tarix və kart sərhədləri, OTP və təkrar istifadədən qorunma, paralel ödənişlərin idempotentliyi, imtina sonrası təkrar cəhd, hər iki sənəd növü, bütün mərhələ şablonları, statusların saxlanması və dəyişdirilməsi, əməliyyat süzgəci, mətnin təhlükəsiz göstərilməsi və zədələnmiş saxlanmış məlumatlar yoxlanılıb.

Playwright ilə brauzerdə çıxarış sifarişinin tam axını, ödəniş imtinası və təkrarı, PDF yüklənməsi, sifariş və səfirlik paneli səhifələri, siçan və klaviatura fokusu yoxlanılıb. 1440×1000, 390×844 və 320×740 ölçüləri sınaqdan keçirilib. Çap və digər brauzerlər ayrıca yoxlanmayıb. Ssenari məlumatları [DEMO_DATA.md](DEMO_DATA.md), yoxlama detalları isə [QA.md](QA.md) faylındadır.

## Resurslar

Görünüş `assets/figma/` qovluğundakı lokal Figma ixraclarından istifadə edir: loqotip, kartlar, illüstrasiyalar, FIN ipucları, nişanlar və PNG/SVG emojilər. Onların görünüşü sistem emoji şriftindən asılı deyil. Inter şrifti lisenziyası ilə birlikdə `assets/fonts/` qovluğundadır. Xidmət SVG ikonları Lucide-dır; lisenziya `assets/LUCIDE-LICENSE` faylındadır. PDF ixracı `pdf-lib` kitabxanasının lokal nüsxəsini istifadə edir; lisenziya `vendor/PDF-LIB-LICENSE.md` faylındadır.
