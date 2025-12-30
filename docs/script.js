document.addEventListener('DOMContentLoaded', () => {

    // --- 1. TRANSLATION DATA (COMPLET - TOUTES LANGUES) ---
    const translations = {
        en: {
            nav_features: "Features", nav_lyrics: "Lyrics", nav_community: "Community", nav_faq: "FAQ",
            hero_title_1: "Music Experience", hero_title_2: "Redefined.",
            hero_desc: "Metrolist is a lightweight, open-source YouTube Music client. Experience ad-free listening, background playback, and Material Design 3 aesthetics.",
            security_text: "100% Safe & Open Source",
            btn_download: "Download APK", btn_learn: "Learn More",
            features_title: "Why Metrolist?",
            feat_adfree_title: "Ad-Free", feat_adfree_desc: "Enjoy your music without interruptions. No video ads, no audio ads.",
            feat_bgplay_title: "Background Play", feat_bgplay_desc: "Keep listening while using other apps or when your screen is locked.",
            feat_theme_title: "Adaptive Themes", feat_theme_desc: "Dynamic colors based on album art (Material You) or pure black mode.",
            feat_offline_title: "Offline Mode", feat_offline_desc: "Download songs and playlists to cache them for offline listening.",
            feat_discord_title: "Discord RPC", feat_discord_desc: "Show your friends what you're listening to automatically via Discord.",
            feat_audio_title: "Audio Tools", feat_audio_desc: "Pitch control, tempo adjustment, silence skipper, and normalization.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Synchronized", lyrics_title_2: "Lyrics Support",
            lyrics_desc: "Metrolist uses advanced open-source libraries to provide the best lyrical experience.",
            lyrics_check_1: "Real-time Sync:", lyrics_check_1_desc: "Karaoke style word-by-word.",
            lyrics_check_2: "Multi-Source:", lyrics_check_2_desc: "Fetches from multiple databases.",
            comm_trans_title: "Help Translate", comm_trans_desc: "Metrolist is available in many languages thanks to volunteers. Join us on Weblate.",
            btn_weblate: "Open Weblate", btn_repo: "View Repository",
            comm_code_title: "Contribute Code", comm_code_desc: "Developers are welcome! Check out the Issues tab on GitHub to get started.",
            gallery_title: "Interface Gallery", faq_title: "FAQ",
            faq_1_q: "Is it safe to use?", faq_1_a: "Yes. Metrolist is open-source (GPL-3.0), meaning the code is public and auditable.",
            faq_2_q: "Why isn't it on the Play Store?", faq_2_a: "Google does not allow third-party YouTube clients on the Play Store. You can safely download updates from GitHub.",
            faq_3_q: "How to update?", faq_3_a: "Download the latest APK from Releases and install it over the old version.",
            footer_tagline: "Open Source Music Client"
        },
        fr: {
            nav_features: "Fonctionnalités", nav_lyrics: "Paroles", nav_community: "Communauté", nav_faq: "FAQ",
            hero_title_1: "L'Expérience Musicale", hero_title_2: "Redéfinie.",
            hero_desc: "Metrolist est un client YouTube Music léger et open-source. Profitez d'une écoute sans pub, en arrière-plan et du Material Design 3.",
            security_text: "100% Sécurisé & Open Source",
            btn_download: "Télécharger APK", btn_learn: "En savoir plus",
            features_title: "Pourquoi Metrolist ?",
            feat_adfree_title: "Sans Publicité", feat_adfree_desc: "Écoutez votre musique sans interruptions. Aucune pub vidéo ou audio.",
            feat_bgplay_title: "Lecture Arrière-plan", feat_bgplay_desc: "Continuez d'écouter en utilisant d'autres applis ou écran verrouillé.",
            feat_theme_title: "Thèmes Adaptatifs", feat_theme_desc: "Couleurs dynamiques basées sur l'album (Material You) ou mode noir pur.",
            feat_offline_title: "Mode Hors-ligne", feat_offline_desc: "Téléchargez chansons et playlists pour les écouter sans connexion.",
            feat_discord_title: "Discord RPC", feat_discord_desc: "Affichez automatiquement ce que vous écoutez sur votre profil Discord.",
            feat_audio_title: "Outils Audio", feat_audio_desc: "Contrôle de tonalité, tempo, saut des silences et normalisation.",
            lyrics_badge: "Propulsé par LRCLIB & Better Lyrics",
            lyrics_title_1: "Paroles", lyrics_title_2: "Synchronisées",
            lyrics_desc: "Metrolist utilise des bibliothèques open-source avancées pour la meilleure expérience de paroles.",
            lyrics_check_1: "Synchro temps-réel :", lyrics_check_1_desc: "Style Karaoké mot par mot.",
            lyrics_check_2: "Multi-Sources :", lyrics_check_2_desc: "Récupère depuis plusieurs bases de données.",
            comm_trans_title: "Aidez à Traduire", comm_trans_desc: "Metrolist est dispo en plusieurs langues grâce aux bénévoles. Rejoignez-nous sur Weblate.",
            btn_weblate: "Ouvrir Weblate", btn_repo: "Voir le code",
            comm_code_title: "Contribuer au Code", comm_code_desc: "Les développeurs sont les bienvenus ! Consultez les Issues sur GitHub.",
            gallery_title: "Galerie d'Interface", faq_title: "FAQ",
            faq_1_q: "Est-ce sécurisé ?", faq_1_a: "Oui. Metrolist est open-source (GPL-3.0), le code est public et vérifiable.",
            faq_2_q: "Pourquoi pas sur le Play Store ?", faq_2_a: "Google interdit les clients tiers YouTube sur le Store. Téléchargez les mises à jour sur GitHub en toute sécurité.",
            faq_3_q: "How to update?", faq_3_a: "Téléchargez le dernier APK depuis les Releases et installez-le par-dessus l'ancienne version.",
            footer_tagline: "Client Musique Open Source"
        },
        es: {
            nav_features: "Características", nav_lyrics: "Letras", nav_community: "Comunidad", nav_faq: "FAQ",
            hero_title_1: "Experiencia Musical", hero_title_2: "Redefinida.",
            hero_desc: "Metrolist es un cliente ligero de YouTube Music de código abierto. Disfruta sin anuncios y con Material Design 3.",
            security_text: "100% Seguro y Código Abierto",
            btn_download: "Descargar APK", btn_learn: "Saber más",
            features_title: "¿Por qué Metrolist?",
            feat_adfree_title: "Sin Anuncios", feat_adfree_desc: "Disfruta de tu música sin interrupciones. Cero publicidad.",
            feat_bgplay_title: "Reproducción de fondo", feat_bgplay_desc: "Sigue escuchando mientras usas otras apps o con la pantalla bloqueada.",
            feat_theme_title: "Temas Dinámicos", feat_theme_desc: "Colores basados en la portada del álbum (Material You) o modo negro puro.",
            feat_offline_title: "Modo Offline", feat_offline_desc: "Descarga canciones para escuchar sin conexión y ahorrar datos.",
            feat_discord_title: "Discord RPC", feat_discord_desc: "Muestra a tus amigos lo que escuchas automáticamente en Discord.",
            feat_audio_title: "Herramientas de Audio", feat_audio_desc: "Control de tono, tempo, salto de silencios y normalización.",
            lyrics_badge: "Impulsado por LRCLIB & Better Lyrics",
            lyrics_title_1: "Letras", lyrics_title_2: "Sincronizadas",
            lyrics_desc: "Metrolist utiliza librerías avanzadas para la mejor experiencia de letras.",
            lyrics_check_1: "Sincronización real:", lyrics_check_1_desc: "Estilo Karaoke palabra por palabra.",
            lyrics_check_2: "Multi-Fuente:", lyrics_check_2_desc: "Obtiene datos de múltiples bases de datos.",
            comm_trans_title: "Ayuda a Traducir", comm_trans_desc: "Metrolist está disponible en muchos idiomas gracias a voluntarios en Weblate.",
            btn_weblate: "Abrir Weblate", btn_repo: "Ver Repositorio",
            comm_code_title: "Contribuir Código", comm_code_desc: "¡Desarrolladores bienvenidos! Revisa las Issues en GitHub.",
            gallery_title: "Galería", faq_title: "Preguntas Frecuentes",
            faq_1_q: "¿Es seguro?", faq_1_a: "Sí. Metrolist es open-source (GPL-3.0), el código es público y auditable.",
            faq_2_q: "¿Por qué no está en Play Store?", faq_2_a: "Google no permite clientes de terceros de YouTube. Descarga actualizaciones desde GitHub.",
            faq_3_q: "¿Cómo actualizar?", faq_3_a: "Descarga el último APK y instálalo sobre la versión anterior.",
            footer_tagline: "Cliente de Música Open Source"
        },
        it: {
            nav_features: "Funzionalità", nav_lyrics: "Testi", nav_community: "Comunità", nav_faq: "FAQ",
            hero_title_1: "Esperienza Musicale", hero_title_2: "Ridefinita.",
            hero_desc: "Metrolist è un client YouTube Music leggero e open-source. Ascolto senza pubblicità e Material Design 3.",
            security_text: "100% Sicuro e Open Source",
            btn_download: "Scarica APK", btn_learn: "Scopri di più",
            features_title: "Perché Metrolist?",
            feat_adfree_title: "Senza Pubblicità", feat_adfree_desc: "Goditi la tua musica senza interruzioni. Niente pubblicità video o audio.",
            feat_bgplay_title: "Riproduzione Background", feat_bgplay_desc: "Continua ad ascoltare mentre usi altre app o a schermo spento.",
            feat_theme_title: "Temi Adattivi", feat_theme_desc: "Colori dinamici basati sulla copertina dell'album o modalità nero puro.",
            feat_offline_title: "Modalità Offline", feat_offline_desc: "Scarica canzoni e playlist per ascoltarle senza connessione.",
            feat_discord_title: "Discord RPC", feat_discord_desc: "Mostra automaticamente cosa stai ascoltando sul tuo profilo Discord.",
            feat_audio_title: "Strumenti Audio", feat_audio_desc: "Controllo tonalità, tempo, salto silenzi e normalizzazione.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Testi", lyrics_title_2: "Sincronizzati",
            lyrics_desc: "Metrolist usa librerie open-source per la migliore esperienza di testi.",
            lyrics_check_1: "Sync in tempo reale:", lyrics_check_1_desc: "Stile Karaoke parola per parola.",
            lyrics_check_2: "Multi-Sorgente:", lyrics_check_2_desc: "Preleva da database multipli.",
            comm_trans_title: "Aiuta a Tradurre", comm_trans_desc: "Metrolist è disponibile in molte lingue grazie a Weblate.",
            btn_weblate: "Apri Weblate", btn_repo: "Vedi Repository",
            comm_code_title: "Contribuisci al Codice", comm_code_desc: "Sviluppatori benvenuti! Controlla le Issues su GitHub.",
            gallery_title: "Galleria", faq_title: "FAQ",
            faq_1_q: "È sicuro?", faq_1_a: "Sì. Metrolist è open-source (GPL-3.0), il codice è pubblico.",
            faq_2_q: "Perché non è sul Play Store?", faq_2_a: "Google vieta i client YouTube di terze parti. Scarica da GitHub in sicurezza.",
            faq_3_q: "Come aggiornare?", faq_3_a: "Scarica l'ultimo APK e installalo sopra la vecchia versione.",
            footer_tagline: "Client Musicale Open Source"
        },
        pt: {
            nav_features: "Funcionalidades", nav_lyrics: "Letras", nav_community: "Comunidade", nav_faq: "FAQ",
            hero_title_1: "Experiência Musical", hero_title_2: "Redefinida.",
            hero_desc: "Metrolist é um cliente YouTube Music leve e open-source. Sem anúncios, reprodução em segundo plano e Material Design 3.",
            security_text: "100% Seguro e Código Aberto",
            btn_download: "Baixar APK", btn_learn: "Saiba Mais",
            features_title: "Por que Metrolist?",
            feat_adfree_title: "Sem Anúncios", feat_adfree_desc: "Curta sua música sem interrupções. Zero publicidade.",
            feat_bgplay_title: "Reprodução em 2º Plano", feat_bgplay_desc: "Continue ouvindo enquanto usa outros apps ou com tela bloqueada.",
            feat_theme_title: "Temas Dinâmicos", feat_theme_desc: "Cores baseadas na capa do álbum (Material You) ou modo preto puro.",
            feat_offline_title: "Modo Offline", feat_offline_desc: "Baixe músicas para ouvir sem internet e economizar dados.",
            feat_discord_title: "Discord RPC", feat_discord_desc: "Mostre o que você está ouvindo automaticamente no Discord.",
            feat_audio_title: "Ferramentas de Áudio", feat_audio_desc: "Controle de tom, tempo, pular silêncio e normalização.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Letras", lyrics_title_2: "Sincronizadas",
            lyrics_desc: "Metrolist usa bibliotecas avançadas para a melhor experiência de letras.",
            lyrics_check_1: "Sincronia Real:", lyrics_check_1_desc: "Estilo Karaokê palavra por palavra.",
            lyrics_check_2: "Multi-Fonte:", lyrics_check_2_desc: "Busca em múltiplos bancos de dados.",
            comm_trans_title: "Ajude a Traduzir", comm_trans_desc: "Metrolist está disponível em vários idiomas graças ao Weblate.",
            btn_weblate: "Abrir Weblate", btn_repo: "Ver Repositório",
            comm_code_title: "Contribua com Código", comm_code_desc: "Desenvolvedores são bem-vindos! Veja as Issues no GitHub.",
            gallery_title: "Galeria", faq_title: "Perguntas Frecuentes",
            faq_1_q: "É seguro?", faq_1_a: "Sim. Metrolist é open-source (GPL-3.0), o código é auditável.",
            faq_2_q: "Por que não está na Play Store?", faq_2_a: "O Google proíbe clientes YouTube de terceiros. Baixe atualizações pelo GitHub.",
            faq_3_q: "Como atualizar?", faq_3_a: "Baixe o APK mais recente e instale por cima da versão antiga.",
            footer_tagline: "Cliente de Música Open Source"
        }
    };

    // --- 2. Language Logic ---
    const langSwitch = document.getElementById('lang-switch');
    
    function updateLanguage(lang) {
        const elements = document.querySelectorAll('[data-i18n]');
        elements.forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (translations[lang] && translations[lang][key]) {
                el.innerText = translations[lang][key];
            }
        });
        if (langSwitch) langSwitch.value = lang; 
        localStorage.setItem('lang', lang);
    }

    const savedLang = localStorage.getItem('lang');
    const userLang = navigator.language.slice(0, 2);
    const defaultLang = savedLang || (translations[userLang] ? userLang : 'en');
    
    updateLanguage(defaultLang);

    if (langSwitch) {
        langSwitch.addEventListener('change', (e) => {
            updateLanguage(e.target.value);
        });
    }

    // --- 3. Theme Toggle ---
    const themeBtn = document.getElementById('theme-toggle');
    if (themeBtn) {
        const themeIcon = themeBtn.querySelector('i');
        const htmlElement = document.documentElement;
        const savedTheme = localStorage.getItem('theme');
        const systemPrefersLight = window.matchMedia('(prefers-color-scheme: light)').matches;

        if (savedTheme === 'light' || (!savedTheme && systemPrefersLight)) {
            htmlElement.setAttribute('data-theme', 'light');
            if (themeIcon) themeIcon.classList.replace('fa-moon', 'fa-sun');
        }

        themeBtn.addEventListener('click', () => {
            const currentTheme = htmlElement.getAttribute('data-theme');
            if (currentTheme === 'light') {
                htmlElement.setAttribute('data-theme', 'dark');
                if (themeIcon) themeIcon.classList.replace('fa-sun', 'fa-moon');
                localStorage.setItem('theme', 'dark');
            } else {
                htmlElement.setAttribute('data-theme', 'light');
                if (themeIcon) themeIcon.classList.replace('fa-moon', 'fa-sun');
                localStorage.setItem('theme', 'light');
            }
        });
    }

    // --- 4. Mobile Menu ---
    const mobileMenu = document.getElementById('mobile-menu');
    const navLinks = document.querySelector('.nav-links');

    if (mobileMenu) {
        mobileMenu.addEventListener('click', () => navLinks.classList.toggle('active'));
    }

    // --- 5. FAQ Accordion ---
    const accordionHeaders = document.querySelectorAll('.accordion-header');
    accordionHeaders.forEach(header => {
        header.addEventListener('click', () => {
            const content = header.nextElementSibling;
            document.querySelectorAll('.accordion-content').forEach(item => {
                if (item !== content) item.style.maxHeight = null;
            });
            content.style.maxHeight = content.style.maxHeight ? null : content.scrollHeight + "px";
        });
    });

    // --- 6. Smooth Scroll ---
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;
            e.preventDefault();
            if (navLinks) navLinks.classList.remove('active');
            const target = document.querySelector(targetId);
            if (target) target.scrollIntoView({ behavior: 'smooth' });
        });
    });
});