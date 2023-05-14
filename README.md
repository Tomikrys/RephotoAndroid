# Mobilní aplikace Rephoto pro android
https://github.com/Tomikrys/RephotoAndroid
## Postup zprovoznění

Nainstalovat Android studio, při vývoji byla využita verze 2021.1.1 spolu s~Android SDK verze 31.

Vytvořit Emulátor.


Přidat do proměnných prostředí systému windows proměnnou OPENCV_ANDROID s cestou ke složce RephotoAndroid\OpenCV-android-sdk-4.5.3.

    OPENCV_ANDROID = C:\folder_structure\RephotoAndroid\OpenCV-android-sdk-4.5.3 

Importovat aplikaci do vývojového prostředí Android Studio.
Synchronizovat projket Gradle.

## Struktura projektu
Pro přehlednost jsou vypsány pouze důležité soubory a složky. Nejdůležitější položky jsou popsány tučně.


```
    RephotoAndroid\
    ├─ .git\                                                - složka s historií vývoje aplikace
    ├─ app\src\main\                                        
    │  ├─ assets\                                           - soubory, která projekt využívá (font)
    │  ├─ cpp\                                              - nativní vrstva
    │  │  ├─ CameraCalibrator.c/.h                        - kalibrace kamery
    │  │  ├─ CMakeLists.txt                               - soubor s pravidly pro překlad, nalinkování OpenCV do nativní vrstvy
    │  │  ├─ errorNIETO.c/.h                             
    │  │  ├─ Line.c/.h
    │  │  ├─ lmmin.c/.h                                   - LevenbergMarquardtLeastSquaresFitting
    │  │  ├─ Main.c/.h                                    - hlavní řízení nativní vrstvy
    │  │  ├─ ModelRegistration.c/.h                       - funkce pro nahrání dat
    │  │  ├─ MSAC.c/.h                            
    │  │  ├─ PnPProblem.c/.h
    │  │  ├─ RobustMatcher.c/.h                           - výpočet odhadu pozice fotoaparátu vůči odhadu pozice historického fotografa
    │  │  └─ Utils.c/.h                                   - pomocné funkce
    │  │  
    │  ├─ java\com\vyw\rephotoandroid
    │  │  ├─ model\                                         - datové třídy
    │  │  │  ├─ api\                                        - třídy pro komunikaci s API přes knihovnu Retrofit
    │  │  │  │  ├─ LoginResponse.java
    │  │  │  │  ├─ OneLoginResponse.java
    │  │  │  │  ├─ Status.java
    │  │  │  │  ├─ UploadFile.java
    │  │  │  │  ├─ UserLogin.java
    │  │  │  │  └─ UserLogout.java
    │  │  │  │  
    │  │  │  ├─ Configuration.java                          - Rozhranní pro načítání proměnných ze SharedPreferences
    │  │  │  ├─ File.java
    │  │  │  ├─ GalleryItem.java
    │  │  │  ├─ ListGalleryItems.java
    │  │  │  ├─ ListPlace.java
    │  │  │  ├─ Photo.java
    │  │  │  ├─ Place.java
    │  │  │  └─ User.java
    │  │  │
    │  │  ├─ smartNavigation\                               - navigace pomocí šipek
    │  │  │  ├─ RegisterPoints.java                         - OBSLUHA ZADÁVÁNÍ KLÍČOVÝCH BODŮ
    │  │  │  └─ SmartNavigation.java                        - OBSLUHA CHYTRÉ NAVIGACE
    │  │  │
    │  │  ├─ ApiInterface.java                                  
    │  │  ├─ GalleryAdapter.java                            
    │  │  ├─ GalleryMainActivity.java                       - HLAVNÍ ACTIVITA APLIKACE
    │  │  ├─ GalleryScreenUtils.java                         
    │  │  ├─ GallerySlideShowFragment.java                  - obsluha detailu místa
    │  │  ├─ GallerySlideShowPagerAdapter.java               
    │  │  ├─ GalleryStripAdapter.java                       - obsluha pásku fotografií v detailu místa
    │  │  ├─ GalleryUtils.java                              - FUNKCE SDÍLENÉ V APLIKACI, NAČÍTÁNÍ Z API
    │  │  ├─ ImageFunctions.java                            - sdílené funkce zpracování obrázků
    │  │  ├─ OpenCVNative.java                              - propojení s nativní vrstvou
    │  │  ├─ PreferenceActivity.java                        - aktivita obluhující nastavení
    │  │  ├─ PreferenceFragment.java                        
    │  │  ├─ RetrofitClient.java                            - třída klienta pro komunikaci s API
    │  │  ├─ SimpleNavigation.java                          - AKTIVITA NAVÁDĚNÍ UŽIVATELE
    │  │  ├─ TermsAndConditions.java                      
    │  │  └─ UploadPhoto.java                               - aktivita uložení a nahrání refotografie na web
    │  │  
    │  ├─ jniLibs\                                          - soubory knihovny OpenCV
    │  ├─ res\
    │  │  ├─ drawable\                                      - ikony a obrázky použité v aplikace
    │  │  ├─ layout\                                        - definice "frontendu" aplikace
    │  │  │  ├─ activity_settings.xml                       - rámec pro vytvoření nastavení v PreferenceActivity                         
    │  │  │  ├─ gallery_activity_main.xml                   - HLAVNÍ OBRAZOVKA APLIKACE, galerie míst
    │  │  │  ├─ gallery_custom_row_gallery_item.xml         - jedno místo v galerii
    │  │  │  ├─ gallery_custom_row_gallery_strip_item.xml   - řádek míst v galerii
    │  │  │  ├─ gallery_fragment_slide_show.xml             - detail místa
    │  │  │  ├─ gallery_pager_item.xml                      - hlavní obrázek v detailu místa
    │  │  │  ├─ login_dialog.xml                           
    │  │  │  ├─ nav_header.xml                              - záhralví draweru
    │  │  │  ├─ select_points.xml                           - volba klíčových bodů
    │  │  │  ├─ simple_navigation.xml                       - OBRAZOVKA JEDNODUCHÉHO POŘIZOVÁNÍ REFOTOGRAFIE
    │  │  │  ├─ smart_navigation.xml                        - OBRAZOVKA CHYTRÉHO POŘIZOVÁNÍ REFOTOGRAFIE
    │  │  │  ├─ terms_and_conditions.xml                    - podmínky použití aplikace
    │  │  │  └─ upload_photo.xml                            - STRÁNKA S KONTROLOU POŘÍZENÉ REFOTOGRAFI A NÁSLEDNÝM NAHRÁNÍM NA WEB NEBO ULOŽENÍM       
    │  │  │
    │  │  ├─ menu\                                          - položky menu
    │  │  ├─ mipmap\                                        - ikony apliakce             
    │  │  ├─ values\                                         
    │  │  │  ├─ colors.xml                                  - barevné schéma aplikace
    │  │  │  ├─ strings.xml                                 - překlady textů
    │  │  │  ├─ styles.xml                        
    │  │  │  └─ themes.xml                     
    │  │  │ 
    │  │  └─ xml\
    │  │     └─ network_security_config.xml                 - NASTAVENÍ DOMÉNY, SE KTEROU MŮŽE APLIKACE KOMUNIKOVAT               
    │  │
    │  └─ AndroidManifest.xml                               - SOUBOR DEFINUJÍCÍ PRÁVA APLIKACE, KOŘENOVÝ SOUBOR PROJEKTU
    │
    ├─ app\release\                                         - GENEROVANÉ SOUBORY APK
    ├─ gradle\wrapper
    │  └─ gradle-wrapper.properities
    │
    ├─ OpenCV-android-sdk-4.5.3\                            - sestavená knihovna OpenCV, kterou využívá projekt
    │  └─ skd\build
    │     └─ build.gradle                                   - skript pro sestavení knihovny OpenCV pro uživatelskou vrstvu
    │
    ├─ build.gradle                                         - skript pro sestavení projektu 
    ├─ README.md
    └─ settings.gradle                                      - soubor s nastavením sestavení projektu
```