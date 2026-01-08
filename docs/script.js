document.addEventListener('DOMContentLoaded', () => {

    // --- 1. TRANSLATION DATA ---
    const translations = {
        en: {
            nav_features: "Features",
            nav_lyrics: "Lyrics",
            nav_community: "Community",
            nav_faq: "FAQ",
            hero_title_1: "Music Experience",
            hero_title_2: "Redefined.",
            hero_desc: "Metrolist is a lightweight, open-source YouTube Music client. Experience ad-free listening, background playback, and Material Design 3 aesthetics.",
            sec_safe: "100% Safe",
            sec_open: "Open Source",
            btn_download: "Download APK",
            btn_learn: "Learn More",
            features_title: "Why Metrolist?",
            feat_adfree_title: "Ad-Free Experience",
            feat_adfree_desc: "Enjoy your music without interruptions. No video or audio advertisements.",
            feat_bgplay_title: "Background Playback",
            feat_bgplay_desc: "Keep listening while using other applications or when your screen is locked.",
            feat_theme_title: "Adaptive Themes",
            feat_theme_desc: "Dynamic colors based on album art (Material You) or pure black mode.",
            feat_offline_title: "Offline Mode",
            feat_offline_desc: "Download songs and playlists to cache them for offline listening.",
            feat_discord_title: "Discord RPC",
            feat_discord_desc: "Show your friends what you are listening to automatically via Discord.",
            feat_audio_title: "Audio Tools",
            feat_audio_desc: "Pitch control, tempo adjustment, silence skipper, and normalization.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Synchronized",
            lyrics_title_2: "Lyrics Support",
            lyrics_desc: "Metrolist uses advanced open-source libraries to provide the best lyrical experience.",
            lyrics_check_1: "Real-time Sync:",
            lyrics_check_1_desc: "Karaoke style word-by-word synchronization.",
            lyrics_check_2: "Multi-Source:",
            lyrics_check_2_desc: "Fetches data from multiple global databases.",
            comm_trans_title: "Help Translate",
            comm_trans_desc: "Metrolist is available in many languages thanks to volunteers. Join us on Weblate.",
            btn_weblate: "Open Weblate",
            btn_repo: "View Repository",
            comm_code_title: "Contribute Code",
            comm_code_desc: "Developers are welcome! Check out the Issues tab on GitHub to get started.",
            gallery_title: "Interface Gallery",
            faq_title: "Frequently Asked Questions",
            faq_1_q: "Is it safe to use?",
            faq_1_a: "Yes. Metrolist is open-source (GPL-3.0), meaning the code is public and auditable by anyone.",
            faq_2_q: "Why isn't it on the Play Store?",
            faq_2_a: "Google does not allow third-party YouTube clients on the Play Store. You can safely download updates from GitHub.",
            faq_3_q: "How to update the application?",
            faq_3_a: "Download the latest APK from the Releases section and install it over the existing version.",
            faq_4_q: "Can I log in with my Google account?",
            faq_4_a: "Yes, Metrolist supports account login via MicroG or built-in OAuth to synchronize your playlists and library.",
            faq_5_q: "Is there an iOS version available?",
            faq_5_a: "No, Metrolist is currently only available for Android due to system restrictions on third-party clients.",
            footer_tagline: "Open Source Music Client"
        },
        fr: {
            nav_features: "Fonctionnalités",
            nav_lyrics: "Paroles",
            nav_community: "Communauté",
            nav_faq: "FAQ",
            hero_title_1: "L'Expérience Musicale",
            hero_title_2: "Redéfinie.",
            hero_desc: "Metrolist est un client YouTube Music léger et open-source. Profitez d'une écoute sans publicité, en arrière-plan et du Material Design 3.",
            sec_safe: "100% Sécurisé",
            sec_open: "Open Source",
            btn_download: "Télécharger APK",
            btn_learn: "En savoir plus",
            features_title: "Pourquoi choisir Metrolist ?",
            feat_adfree_title: "Sans Publicité",
            feat_adfree_desc: "Écoutez votre musique sans interruptions. Aucune publicité vidéo ou audio.",
            feat_bgplay_title: "Lecture en Arrière-plan",
            feat_bgplay_desc: "Continuez l'écoute en utilisant d'autres applications ou avec l'écran verrouillé.",
            feat_theme_title: "Thèmes Adaptatifs",
            feat_theme_desc: "Couleurs dynamiques basées sur l'album (Material You) ou mode noir pur.",
            feat_offline_title: "Mode Hors-ligne",
            feat_offline_desc: "Téléchargez chansons et playlists pour les écouter sans connexion internet.",
            feat_discord_title: "Discord RPC",
            feat_discord_desc: "Affichez automatiquement votre musique en cours sur votre profil Discord.",
            feat_audio_title: "Outils Audio",
            feat_audio_desc: "Contrôle de tonalité, tempo, saut des silences et normalisation.",
            lyrics_badge: "Propulsé par LRCLIB & Better Lyrics",
            lyrics_title_1: "Paroles",
            lyrics_title_2: "Synchronisées",
            lyrics_desc: "Metrolist utilise des bibliothèques open-source avancées pour offrir la meilleure expérience de paroles.",
            lyrics_check_1: "Synchro temps-réel :",
            lyrics_check_1_desc: "Synchronisation mot par mot style Karaoké.",
            lyrics_check_2: "Multi-Sources :",
            lyrics_check_2_desc: "Récupération depuis plusieurs bases de données mondiales.",
            comm_trans_title: "Aidez à Traduire",
            comm_trans_desc: "Metrolist est disponible en plusieurs langues grâce aux bénévoles. Rejoignez-nous sur Weblate.",
            btn_weblate: "Ouvrir Weblate",
            btn_repo: "Voir le code",
            comm_code_title: "Contribuer au Code",
            comm_code_desc: "Les développeurs sont les bienvenus ! Consultez les tickets sur GitHub pour commencer.",
            gallery_title: "Galerie d'Interface",
            faq_title: "Foire Aux Questions",
            faq_1_q: "Est-ce sécurisé ?",
            faq_1_a: "Oui. Metrolist est open-source (GPL-3.0), ce qui signifie que le code est public et vérifiable par tous.",
            faq_2_q: "Pourquoi l'application n'est pas sur le Play Store ?",
            faq_2_a: "Google interdit les clients tiers YouTube sur son magasin. Vous pouvez télécharger les mises à jour sur GitHub en toute sécurité.",
            faq_3_q: "Comment mettre à jour l'application ?",
            faq_3_a: "Téléchargez le dernier APK depuis la section Releases et installez-le par-dessus la version actuelle.",
            faq_4_q: "Puis-je me connecter à mon compte Google ?",
            faq_4_a: "Oui, Metrolist supporte la connexion via MicroG ou OAuth intégré pour synchroniser vos playlists et votre bibliothèque.",
            faq_5_q: "Existe-t-il une version iOS ?",
            faq_5_a: "Non, Metrolist est actuellement disponible uniquement sur Android en raison des restrictions système sur les clients tiers.",
            footer_tagline: "Client Musique Open Source"
        },
        es: {
            nav_features: "Características",
            nav_lyrics: "Letras",
            nav_community: "Comunidad",
            nav_faq: "FAQ",
            hero_title_1: "Experiencia Musical",
            hero_title_2: "Redefinida.",
            hero_desc: "Metrolist es un cliente de YouTube Music ligero y de código abierto. Disfrute de música sin publicidad y con diseño Material Design 3.",
            sec_safe: "100% Seguro",
            sec_open: "Código Abierto",
            btn_download: "Descargar APK",
            btn_learn: "Más información",
            features_title: "¿Por qué Metrolist?",
            feat_adfree_title: "Sin Publicidad",
            feat_adfree_desc: "Disfrute de su música sin interrupciones. Sin anuncios de vídeo ni de audio.",
            feat_bgplay_title: "Reproducción en Segundo Plano",
            feat_bgplay_desc: "Siga escuchando mientras utiliza otras aplicaciones o con la pantalla bloqueada.",
            feat_theme_title: "Temas Dinámicos",
            feat_theme_desc: "Colores dinámicos basados en la portada (Material You) o modo negro puro.",
            feat_offline_title: "Modo Sin Conexión",
            feat_offline_desc: "Descargue canciones y listas para escucharlas sin conexión a internet.",
            feat_discord_title: "Discord RPC",
            feat_discord_desc: "Muestre automáticamente lo que está escuchando en su perfil de Discord.",
            feat_audio_title: "Herramientas de Audio",
            feat_audio_desc: "Control de tono, ajuste de tempo, omisión de silencios y normalización.",
            lyrics_badge: "Impulsado por LRCLIB & Better Lyrics",
            lyrics_title_1: "Letras",
            lyrics_title_2: "Sincronizadas",
            lyrics_desc: "Metrolist utiliza librerías avanzadas para ofrecer la mejor experiencia con las letras.",
            lyrics_check_1: "Sincronía en tiempo real:",
            lyrics_check_1_desc: "Sincronización palabra por palabra estilo Karaoke.",
            lyrics_check_2: "Multi-Fuente:",
            lyrics_check_2_desc: "Obtención de datos desde múltiples bases de datos.",
            comm_trans_title: "Ayuda con la Traducción",
            comm_trans_desc: "Metrolist está disponible en varios idiomas gracias a voluntarios. Únase a nosotros en Weblate.",
            btn_weblate: "Abrir Weblate",
            btn_repo: "Ver Repositorio",
            comm_code_title: "Contribuir al Código",
            comm_code_desc: "¡Los desarrolladores son bienvenidos! Revise los Issues en GitHub para comenzar.",
            gallery_title: "Galería",
            faq_title: "Preguntas Frecuentes",
            faq_1_q: "¿Es seguro usarlo?",
            faq_1_a: "Sí. Metrolist es de código abierto (GPL-3.0), lo que significa que el código es público y auditable.",
            faq_2_q: "¿Por qué no está en la Play Store?",
            faq_2_a: "Google no permite clientes de terceros de YouTube en su tienda. Puede descargar las actualizaciones desde GitHub.",
            faq_3_q: "¿Cómo actualizar la aplicación?",
            faq_3_a: "Descargue el último APK desde la sección de Releases e instálelo sobre la versión anterior.",
            faq_4_q: "¿Puedo iniciar sesión con mi cuenta de Google?",
            faq_4_a: "Sí, Metrolist admite el inicio de sesión a través de MicroG o OAuth integrado para sincronizar sus listas.",
            faq_5_q: "¿Existe una versión para iOS?",
            faq_5_a: "No, actualmente Metrolist solo está disponible para Android debido a las restricciones del sistema.",
            footer_tagline: "Cliente de Música de Código Abierto"
        },
        it: {
            nav_features: "Funzionalità",
            nav_lyrics: "Testi",
            nav_community: "Comunità",
            nav_faq: "FAQ",
            hero_title_1: "Esperienza Musicale",
            hero_title_2: "Ridefinita.",
            hero_desc: "Metrolist è un client YouTube Music leggero e open-source. Ascolto senza pubblicità e Material Design 3.",
            sec_safe: "100% Sicuro",
            sec_open: "Open Source",
            btn_download: "Scarica APK",
            btn_learn: "Scopri di più",
            features_title: "Perché scegliere Metrolist?",
            feat_adfree_title: "Senza Pubblicità",
            feat_adfree_desc: "Goditi la tua musica senza interruzioni. Niente annunci video o audio.",
            feat_bgplay_title: "Riproduzione in Background",
            feat_bgplay_desc: "Continua ad ascoltare mentre usi altre app o con lo schermo bloccato.",
            feat_theme_title: "Temi Adattivi",
            feat_theme_desc: "Colori dinamici basati sulla copertina (Material You) o modalità nero puro.",
            feat_offline_title: "Modalità Offline",
            feat_offline_desc: "Scarica canzoni e playlist per l'ascolto senza connessione internet.",
            feat_discord_title: "Discord RPC",
            feat_discord_desc: "Mostra automaticamente cosa stai ascoltando sul tuo profilo Discord.",
            feat_audio_title: "Strumenti Audio",
            feat_audio_desc: "Controllo tonalità, tempo, salto silenzi e normalizzazione.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Testi",
            lyrics_title_2: "Sincronizzati",
            lyrics_desc: "Metrolist utilizza librerie open-source per la migliore esperienza dei testi.",
            lyrics_check_1: "Sincronizzazione reale:",
            lyrics_check_1_desc: "Sincronizzazione parola per parola in stile Karaoke.",
            lyrics_check_2: "Multi-Sorgente:",
            lyrics_check_2_desc: "Recupero dati da più database globali.",
            comm_trans_title: "Aiuta a Tradurre",
            comm_trans_desc: "Metrolist è disponibile in molte lingue grazie ai volontari. Unisciti a noi su Weblate.",
            btn_weblate: "Apri Weblate",
            btn_repo: "Vedi Repository",
            comm_code_title: "Contribuisci al Codice",
            comm_code_desc: "Gli sviluppatori sono i benvenuti! Controlla le Issues su GitHub per iniziare.",
            gallery_title: "Galleria",
            faq_title: "Domande Frequenti",
            faq_1_q: "È sicuro da usare?",
            faq_1_a: "Sì. Metrolist è open-source (GPL-3.0), il codice è pubblico e verificabile.",
            faq_2_q: "Perché non è sul Play Store?",
            faq_2_a: "Google non consente client YouTube di terze parti sul Play Store. Scarica gli aggiornamenti da GitHub.",
            faq_3_q: "Come aggiornare l'applicazione?",
            faq_3_a: "Scarica l'ultimo APK dalle Releases e installalo sopra la versione esistente.",
            faq_4_q: "Posso accedere con il mio account Google?",
            faq_4_a: "Sì, Metrolist supporta l'accesso tramite MicroG o OAuth integrato per sincronizzare playlist e libreria.",
            faq_5_q: "Esiste una versione per iOS?",
            faq_5_a: "No, Metrolist è disponibile solo per Android a causa delle restrizioni di sistema.",
            footer_tagline: "Client Musicale Open Source"
        },
        pt: {
            nav_features: "Funcionalidades",
            nav_lyrics: "Letras",
            nav_community: "Comunidade",
            nav_faq: "FAQ",
            hero_title_1: "Experiência Musical",
            hero_title_2: "Redefinida.",
            hero_desc: "Metrolist é um cliente de YouTube Music leve e open-source. Sem anúncios, reprodução em segundo plano e Material Design 3.",
            sec_safe: "100% Seguro",
            sec_open: "Código Aberto",
            btn_download: "Baixar APK",
            btn_learn: "Saiba Mais",
            features_title: "Por que Metrolist?",
            feat_adfree_title: "Sem Publicidade",
            feat_adfree_desc: "Curta sua música sem interrupções. Sem anúncios de vídeo ou áudio.",
            feat_bgplay_title: "Reprodução em Segundo Plano",
            feat_bgplay_desc: "Continue ouvindo enquanto usa outros apps ou com a tela bloqueada.",
            feat_theme_title: "Temas Dinâmicos",
            feat_theme_desc: "Cores baseadas na capa do álbum (Material You) ou modo preto puro.",
            feat_offline_title: "Modo Offline",
            feat_offline_desc: "Baixe músicas para ouvir sem internet e economizar dados.",
            feat_discord_title: "Discord RPC",
            feat_discord_desc: "Mostre o que você está ouvindo automaticamente no seu perfil do Discord.",
            feat_audio_title: "Ferramentas de Áudio",
            feat_audio_desc: "Controle de tom, tempo, pular silêncio e normalização.",
            lyrics_badge: "Powered by LRCLIB & Better Lyrics",
            lyrics_title_1: "Letras",
            lyrics_title_2: "Sincronizadas",
            lyrics_desc: "Metrolist usa bibliotecas avançadas para a melhor experiência de letras.",
            lyrics_check_1: "Sincronia em tempo real:",
            lyrics_check_1_desc: "Sincronização palavra por palavra estilo Karaokê.",
            lyrics_check_2: "Multi-Fonte:",
            lyrics_check_2_desc: "Busca em múltiplos bancos de dados globais.",
            comm_trans_title: "Ajude a Traduzir",
            comm_trans_desc: "O Metrolist está disponível em vários idiomas graças a voluntários. Junte-se a nós no Weblate.",
            btn_weblate: "Abrir Weblate",
            btn_repo: "Ver Repositório",
            comm_code_title: "Contribuir com Código",
            comm_code_desc: "Desenvolvedores são bem-vindos! Veja as Issues no GitHub para começar.",
            gallery_title: "Galeria",
            faq_title: "Perguntas Frequentes",
            faq_1_q: "É seguro utilizar?",
            faq_1_a: "Sim. O Metrolist é open-source (GPL-3.0), o código é público e auditável.",
            faq_2_q: "Por que não está na Play Store?",
            faq_2_a: "O Google proíbe clientes YouTube de terceiros na sua loja. Baixe atualizações pelo GitHub.",
            faq_3_q: "Como atualizar a aplicação?",
            faq_3_a: "Baixe o APK mais recente e instale por cima da versão antiga.",
            faq_4_q: "Posso fazer login com minha conta do Google?",
            faq_4_a: "Sim, o Metrolist suporta login via MicroG ou OAuth integrado para sincronizar playlists e biblioteca.",
            faq_5_q: "Existe uma versão para iOS?",
            faq_5_a: "Não, o Metrolist está disponível apenas para Android devido às restrições do sistema.",
            footer_tagline: "Cliente de Música Open Source"
        }
    };

    // --- 2. Language Logic ---
    const langSelect = document.getElementById('lang-switch');
    
    function updateLanguage(lang) {
        const elements = document.querySelectorAll('[data-i18n]');
        elements.forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (translations[lang] && translations[lang][key]) {
                el.innerText = translations[lang][key];
            }
        });
        if (langSelect) langSelect.value = lang; 
        localStorage.setItem('lang', lang);
    }

    const savedLang = localStorage.getItem('lang');
    const userLang = navigator.language.slice(0, 2);
    const defaultLang = savedLang || (translations[userLang] ? userLang : 'en');
    
    updateLanguage(defaultLang);

    if (langSelect) {
        langSelect.addEventListener('change', (e) => {
            updateLanguage(e.target.value);
        });
    }

    // --- 3. Advanced Theme Management (Auto / Light / Dark) ---
    const themeBtn = document.getElementById('theme-toggle');
    const themeText = document.getElementById('theme-text');
    const themeIcon = themeBtn ? themeBtn.querySelector('i') : null;
    const htmlElement = document.documentElement;

    // Function to get system preference
    const getSystemTheme = () => window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';

    function updateThemeUI(mode, actualTheme) {
        htmlElement.setAttribute('data-theme', actualTheme);
        
        if (themeText) themeText.innerText = mode;

        if (themeIcon) {
            if (mode === 'auto') {
                themeIcon.className = 'fas fa-magic';
            } else {
                themeIcon.className = actualTheme === 'light' ? 'fas fa-sun' : 'fas fa-moon';
            }
        }
    }

    function applyMode(mode) {
        let themeToApply = mode;
        if (mode === 'auto') {
            themeToApply = getSystemTheme();
        }
        
        updateThemeUI(mode, themeToApply);
        localStorage.setItem('theme-mode', mode);
    }

    // Initial Load
    const savedMode = localStorage.getItem('theme-mode') || 'auto';
    applyMode(savedMode);

    // Click Listener: Cycle between Auto -> Light -> Dark
    if (themeBtn) {
        themeBtn.addEventListener('click', () => {
            const currentMode = localStorage.getItem('theme-mode') || 'auto';
            let nextMode;

            if (currentMode === 'auto') nextMode = 'light';
            else if (currentMode === 'light') nextMode = 'dark';
            else nextMode = 'auto';

            applyMode(nextMode);
        });
    }

    // Watch for system changes (only active if mode is 'auto')
    window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', (e) => {
        if (localStorage.getItem('theme-mode') === 'auto') {
            updateThemeUI('auto', e.matches ? 'light' : 'dark');
        }
    });
    
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

    // --- 7. Fetch Version from Metrolist GitHub Releases ---
    const versionBadge = document.getElementById('version-badge');
    if (versionBadge) {
        fetch('https://api.github.com/repos/mostafaalagamy/Metrolist/releases/latest', {
            method: 'GET',
            headers: {
                'Accept': 'application/vnd.github.v3+json'
            }
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            if (data && data.tag_name) {
                const version = data.tag_name;
                const isPrerelease = data.prerelease;
                versionBadge.textContent = `${version} ${isPrerelease ? 'Beta' : 'Stable'}`;
            }
        })
        .catch(error => {
            console.log('Version fetch error:', error);
        });
    }
});