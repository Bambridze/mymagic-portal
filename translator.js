async function autoTranslate() {
    const urlParams = new URLSearchParams(window.location.search);
    const targetLang = urlParams.get('lang');
    
    if (!targetLang || targetLang.toLowerCase() === 'en') return;

    document.documentElement.lang = targetLang;
    
    const cachedData = localStorage.getItem(`magic_legal_${targetLang}`);
    const elements = document.querySelectorAll('[data-translate="true"]');
    
    if (cachedData) {
        const translations = JSON.parse(cachedData);
        elements.forEach((el, index) => {
            if (translations[index]) el.innerText = translations[index];
        });
        return;
    }

    const contentArea = document.getElementById('content-area');
    if (contentArea) contentArea.classList.add('translating');

    const textArray = Array.from(elements).map(el => el.innerText);
    const savedTranslations = [];

    try {
        for (let i = 0; i < elements.length; i++) {
            const text = textArray[i];
            const response = await fetch(`https://googleapis.com{targetLang}&dt=t&q=${encodeURIComponent(text)}`);
            const result = await response.json();
            
            let translatedText = "";
            if (result && result) {
                translatedText = result.map(item => item).join('');
            } else {
                translatedText = text;
            }
            
            elements[i].innerText = translatedText;
            savedTranslations.push(translatedText);
        }
        
        localStorage.setItem(`magic_legal_${targetLang}`, JSON.stringify(savedTranslations));
        
    } catch (error) {
        console.error("Translation failed:", error);
    } finally {
        if (contentArea) contentArea.classList.remove('translating');
    }
}

document.addEventListener('DOMContentLoaded', autoTranslate);
