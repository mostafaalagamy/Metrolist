// Translation API Configuration
const TRANSLATION_API = 'https://api.mymemory.translated.net/get';
const LANGUAGES = {
    'auto': 'اكتشاف تلقائي',
    'ar': 'العربية',
    'en': 'الإنجليزية',
    'fr': 'الفرنسية',
    'es': 'الإسبانية',
    'de': 'الألمانية',
    'it': 'الإيطالية',
    'pt': 'البرتغالية',
    'ru': 'الروسية',
    'zh': 'الصينية',
    'ja': 'اليابانية',
    'ko': 'الكورية',
    'tr': 'التركية',
    'hi': 'الهندية',
    'ur': 'الأردية'
};

// State Management
let currentSourceLang = 'en';
let currentTargetLang = 'ar';
let translationHistory = JSON.parse(localStorage.getItem('translationHistory')) || [];

// DOM Elements
const sourceText = document.getElementById('sourceText');
const targetText = document.getElementById('targetText');
const translateBtn = document.getElementById('translateBtn');
const swapBtn = document.getElementById('swapLangs');
const sourceLangBtn = document.getElementById('sourceLangBtn');
const targetLangBtn = document.getElementById('targetLangBtn');
const sourceLangDropdown = document.getElementById('sourceLangDropdown');
const targetLangDropdown = document.getElementById('targetLangDropdown');
const loadingOverlay = document.getElementById('loadingOverlay');
const toast = document.getElementById('toast');
const themeToggle = document.getElementById('themeToggle');
const charCount = document.querySelector('.char-count');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initializeEventListeners();
    loadTheme();
    updateCharCount();
    renderHistory();
});

// Event Listeners
function initializeEventListeners() {
    // Translation
    translateBtn.addEventListener('click', translateText);
    sourceText.addEventListener('input', () => {
        updateCharCount();
        // Auto-translate after typing stops
        clearTimeout(window.autoTranslateTimer);
        window.autoTranslateTimer = setTimeout(() => {
            if (sourceText.value.trim()) {
                translateText();
            }
        }, 1000);
    });

    // Language Selection
    sourceLangBtn.addEventListener('click', () => toggleDropdown(sourceLangDropdown));
    targetLangBtn.addEventListener('click', () => toggleDropdown(targetLangDropdown));
    
    // Language Options
    document.querySelectorAll('#sourceLangDropdown .lang-option').forEach(option => {
        option.addEventListener('click', (e) => selectLanguage(e, 'source'));
    });
    
    document.querySelectorAll('#targetLangDropdown .lang-option').forEach(option => {
        option.addEventListener('click', (e) => selectLanguage(e, 'target'));
    });

    // Swap Languages
    swapBtn.addEventListener('click', swapLanguages);

    // Action Buttons
    document.getElementById('clearSource').addEventListener('click', clearSource);
    document.getElementById('copyTranslation').addEventListener('click', copyTranslation);
    document.getElementById('pasteBtn').addEventListener('click', pasteText);
    document.getElementById('voiceInput').addEventListener('click', startVoiceInput);
    document.getElementById('speakTranslation').addEventListener('click', speakTranslation);
    document.getElementById('shareTranslation').addEventListener('click', shareTranslation);
    document.getElementById('saveTranslation').addEventListener('click', saveTranslation);
    document.getElementById('uploadFile').addEventListener('click', uploadFile);

    // Quick Phrases
    document.querySelectorAll('.phrase-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const text = e.currentTarget.getAttribute('data-text');
            sourceText.value = text;
            updateCharCount();
            translateText();
        });
    });

    // Theme Toggle
    themeToggle.addEventListener('click', toggleTheme);

    // Clear History
    document.querySelector('.clear-history-btn')?.addEventListener('click', clearHistory);

    // Close dropdowns when clicking outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.lang-selector')) {
            document.querySelectorAll('.lang-dropdown').forEach(dropdown => {
                dropdown.classList.remove('active');
            });
        }
    });

    // Language Search
    document.querySelectorAll('.lang-search-input').forEach(input => {
        input.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            const langList = e.target.closest('.lang-dropdown').querySelector('.lang-list');
            const options = langList.querySelectorAll('.lang-option');
            
            options.forEach(option => {
                const langName = option.textContent.toLowerCase();
                if (langName.includes(searchTerm)) {
                    option.style.display = 'flex';
                } else {
                    option.style.display = 'none';
                }
            });
        });
    });
}

// Translation Function
async function translateText() {
    const text = sourceText.value.trim();
    if (!text) return;

    showLoading(true);
    
    try {
        // Using MyMemory Translation API (free tier)
        const langPair = `${currentSourceLang}|${currentTargetLang}`;
        const url = `${TRANSLATION_API}?q=${encodeURIComponent(text)}&langpair=${langPair}`;
        
        const response = await fetch(url);
        const data = await response.json();
        
        if (data.responseStatus === 200) {
            const translatedText = data.responseData.translatedText;
            displayTranslation(translatedText);
            
            // Add to history
            addToHistory(text, translatedText, currentSourceLang, currentTargetLang);
        } else {
            // Fallback to mock translation for demo
            const mockTranslation = getMockTranslation(text, currentSourceLang, currentTargetLang);
            displayTranslation(mockTranslation);
            addToHistory(text, mockTranslation, currentSourceLang, currentTargetLang);
        }
    } catch (error) {
        console.error('Translation error:', error);
        // Use mock translation as fallback
        const mockTranslation = getMockTranslation(text, currentSourceLang, currentTargetLang);
        displayTranslation(mockTranslation);
    } finally {
        showLoading(false);
    }
}

// Mock Translation Function (for demo purposes)
function getMockTranslation(text, sourceLang, targetLang) {
    const translations = {
        'Hello': 'مرحبا',
        'How are you?': 'كيف حالك؟',
        'Thank you': 'شكراً',
        'Good morning': 'صباح الخير',
        'Good evening': 'مساء الخير',
        'Welcome': 'أهلاً وسهلاً',
        'Goodbye': 'وداعاً',
        'Please': 'من فضلك',
        'Sorry': 'آسف',
        'Yes': 'نعم',
        'No': 'لا',
        'Hello, how are you?': 'مرحباً، كيف حالك؟',
        'Thank you very much': 'شكراً جزيلاً',
        'Can you help me?': 'هل يمكنك مساعدتي؟',
        'Where is the nearest restaurant?': 'أين أقرب مطعم؟',
        'How much does this cost?': 'كم سعر هذا؟',
        'I need directions': 'أحتاج إلى الاتجاهات'
    };

    // If exact translation exists, return it
    if (translations[text]) {
        return translations[text];
    }

    // Otherwise, return a generic translated message
    if (targetLang === 'ar') {
        return `ترجمة: ${text}`;
    } else {
        return `Translation: ${text}`;
    }
}

// Display Translation
function displayTranslation(text) {
    targetText.innerHTML = `<p>${text}</p>`;
}

// Language Selection
function selectLanguage(e, type) {
    const option = e.currentTarget;
    const lang = option.getAttribute('data-lang');
    const flag = option.getAttribute('data-flag');
    const name = option.querySelector('span:last-child').textContent;
    
    if (type === 'source') {
        currentSourceLang = lang;
        sourceLangBtn.querySelector('.lang-flag').textContent = flag;
        sourceLangBtn.querySelector('.lang-name').textContent = name;
        
        // Update active state
        document.querySelectorAll('#sourceLangDropdown .lang-option').forEach(opt => {
            opt.classList.remove('active');
        });
        option.classList.add('active');
        
        sourceLangDropdown.classList.remove('active');
    } else {
        currentTargetLang = lang;
        targetLangBtn.querySelector('.lang-flag').textContent = flag;
        targetLangBtn.querySelector('.lang-name').textContent = name;
        
        // Update active state
        document.querySelectorAll('#targetLangDropdown .lang-option').forEach(opt => {
            opt.classList.remove('active');
        });
        option.classList.add('active');
        
        targetLangDropdown.classList.remove('active');
    }
    
    // Auto-translate if there's text
    if (sourceText.value.trim()) {
        translateText();
    }
}

// Toggle Dropdown
function toggleDropdown(dropdown) {
    dropdown.classList.toggle('active');
    
    // Close other dropdowns
    document.querySelectorAll('.lang-dropdown').forEach(d => {
        if (d !== dropdown) {
            d.classList.remove('active');
        }
    });
}

// Swap Languages
function swapLanguages() {
    // Don't swap if source is auto-detect
    if (currentSourceLang === 'auto') return;
    
    // Swap language codes
    [currentSourceLang, currentTargetLang] = [currentTargetLang, currentSourceLang];
    
    // Swap UI elements
    const sourceFlag = sourceLangBtn.querySelector('.lang-flag').textContent;
    const sourceName = sourceLangBtn.querySelector('.lang-name').textContent;
    const targetFlag = targetLangBtn.querySelector('.lang-flag').textContent;
    const targetName = targetLangBtn.querySelector('.lang-name').textContent;
    
    sourceLangBtn.querySelector('.lang-flag').textContent = targetFlag;
    sourceLangBtn.querySelector('.lang-name').textContent = targetName;
    targetLangBtn.querySelector('.lang-flag').textContent = sourceFlag;
    targetLangBtn.querySelector('.lang-name').textContent = sourceName;
    
    // Swap text content
    const sourceValue = sourceText.value;
    const targetValue = targetText.textContent.trim();
    
    if (targetValue && targetValue !== 'الترجمة ستظهر هنا') {
        sourceText.value = targetValue;
        updateCharCount();
        translateText();
    }
}

// Action Functions
function clearSource() {
    sourceText.value = '';
    targetText.innerHTML = `
        <div class="placeholder-text">
            <i class="fas fa-language"></i>
            <p>الترجمة ستظهر هنا</p>
        </div>
    `;
    updateCharCount();
}

function copyTranslation() {
    const text = targetText.textContent.trim();
    if (text && text !== 'الترجمة ستظهر هنا') {
        navigator.clipboard.writeText(text).then(() => {
            showToast('تم النسخ بنجاح!');
        });
    }
}

function pasteText() {
    navigator.clipboard.readText().then(text => {
        sourceText.value = text;
        updateCharCount();
        translateText();
    });
}

function startVoiceInput() {
    if (!('webkitSpeechRecognition' in window || 'SpeechRecognition' in window)) {
        showToast('المتصفح لا يدعم الإدخال الصوتي');
        return;
    }
    
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    
    recognition.lang = currentSourceLang === 'auto' ? 'en-US' : currentSourceLang;
    recognition.continuous = false;
    recognition.interimResults = false;
    
    recognition.onstart = () => {
        document.getElementById('voiceInput').classList.add('recording');
        showToast('جاري الاستماع...');
    };
    
    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        sourceText.value = transcript;
        updateCharCount();
        translateText();
    };
    
    recognition.onerror = () => {
        showToast('حدث خطأ في التسجيل الصوتي');
    };
    
    recognition.onend = () => {
        document.getElementById('voiceInput').classList.remove('recording');
    };
    
    recognition.start();
}

function speakTranslation() {
    const text = targetText.textContent.trim();
    if (text && text !== 'الترجمة ستظهر هنا') {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = currentTargetLang;
        speechSynthesis.speak(utterance);
    }
}

function shareTranslation() {
    const text = targetText.textContent.trim();
    if (text && text !== 'الترجمة ستظهر هنا') {
        if (navigator.share) {
            navigator.share({
                title: 'ترجمة',
                text: text
            });
        } else {
            navigator.clipboard.writeText(text).then(() => {
                showToast('تم نسخ النص للمشاركة');
            });
        }
    }
}

function saveTranslation() {
    const source = sourceText.value.trim();
    const target = targetText.textContent.trim();
    
    if (source && target && target !== 'الترجمة ستظهر هنا') {
        const saved = JSON.parse(localStorage.getItem('savedTranslations')) || [];
        saved.unshift({
            source,
            target,
            sourceLang: currentSourceLang,
            targetLang: currentTargetLang,
            timestamp: Date.now()
        });
        localStorage.setItem('savedTranslations', JSON.stringify(saved.slice(0, 50)));
        showToast('تم حفظ الترجمة');
    }
}

function uploadFile() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.txt,.doc,.docx';
    input.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                sourceText.value = e.target.result;
                updateCharCount();
                translateText();
            };
            reader.readAsText(file);
        }
    };
    input.click();
}

// History Functions
function addToHistory(source, target, sourceLang, targetLang) {
    const historyItem = {
        source,
        target,
        sourceLang,
        targetLang,
        timestamp: Date.now()
    };
    
    translationHistory.unshift(historyItem);
    translationHistory = translationHistory.slice(0, 20); // Keep only last 20 items
    localStorage.setItem('translationHistory', JSON.stringify(translationHistory));
    renderHistory();
}

function renderHistory() {
    const historyList = document.querySelector('.history-list');
    if (!historyList) return;
    
    if (translationHistory.length === 0) {
        historyList.innerHTML = '<p style="text-align: center; color: var(--text-light);">لا يوجد سجل</p>';
        return;
    }
    
    historyList.innerHTML = translationHistory.map(item => `
        <div class="history-item">
            <div class="history-langs">
                <span class="lang-badge">${item.sourceLang.toUpperCase()}</span>
                <i class="fas fa-arrow-right"></i>
                <span class="lang-badge">${item.targetLang.toUpperCase()}</span>
            </div>
            <div class="history-content">
                <p class="history-source">${item.source}</p>
                <p class="history-target">${item.target}</p>
            </div>
            <div class="history-actions">
                <button class="history-action-btn" onclick="copyHistoryItem('${item.target.replace(/'/g, "\\'")}')">
                    <i class="fas fa-copy"></i>
                </button>
                <button class="history-action-btn" onclick="loadHistoryItem('${item.source.replace(/'/g, "\\'")}')">
                    <i class="fas fa-redo"></i>
                </button>
            </div>
        </div>
    `).join('');
}

function clearHistory() {
    if (confirm('هل أنت متأكد من حذف السجل؟')) {
        translationHistory = [];
        localStorage.removeItem('translationHistory');
        renderHistory();
        showToast('تم مسح السجل');
    }
}

window.copyHistoryItem = function(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('تم النسخ');
    });
};

window.loadHistoryItem = function(text) {
    sourceText.value = text;
    updateCharCount();
    translateText();
};

// UI Helper Functions
function updateCharCount() {
    const count = sourceText.value.length;
    charCount.textContent = `${count} / 5000`;
    
    if (count > 4500) {
        charCount.style.color = 'var(--error-color)';
    } else if (count > 4000) {
        charCount.style.color = 'var(--warning-color)';
    } else {
        charCount.style.color = 'var(--text-light)';
    }
}

function showLoading(show) {
    if (show) {
        loadingOverlay.classList.add('active');
    } else {
        loadingOverlay.classList.remove('active');
    }
}

function showToast(message) {
    const toastMessage = toast.querySelector('.toast-message');
    toastMessage.textContent = message;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// Theme Functions
function toggleTheme() {
    const body = document.body;
    const isDark = body.classList.toggle('dark-theme');
    
    const icon = themeToggle.querySelector('i');
    icon.className = isDark ? 'fas fa-sun' : 'fas fa-moon';
    
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
}

function loadTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-theme');
        themeToggle.querySelector('i').className = 'fas fa-sun';
    }
}

// Keyboard Shortcuts
document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + Enter to translate
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        translateText();
    }
    
    // Ctrl/Cmd + Shift + C to copy translation
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'C') {
        copyTranslation();
    }
    
    // Ctrl/Cmd + Shift + S to swap languages
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'S') {
        swapLanguages();
    }
});