
        // Tab switching logic
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabPanes = document.querySelectorAll('.tab-pane');

        if (tabBtns.length > 0) {
            tabBtns.forEach(btn => {
                btn.addEventListener('click', () => {
                    tabBtns.forEach(b => b.classList.remove('active'));
                    tabPanes.forEach(p => p.classList.remove('active'));
                    
                    btn.classList.add('active');
                    const targetId = btn.getAttribute('data-target');
                    document.getElementById(targetId).classList.add('active');
                });
            });
        }

        const themeToggle = document.getElementById('themeToggle');
        const sunIcon = document.getElementById('sunIcon');
        const moonIcon = document.getElementById('moonIcon');
        
        // Setup Initial Theme
        if (localStorage.getItem('theme') === 'light' || (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: light)').matches)) {
            document.body.setAttribute('data-theme', 'light');
            if (sunIcon) sunIcon.style.display = 'block';
            if (moonIcon) moonIcon.style.display = 'none';
        }

        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                if (document.body.getAttribute('data-theme') === 'dark') {
                    document.body.setAttribute('data-theme', 'light');
                    localStorage.setItem('theme', 'light');
                    if (sunIcon) sunIcon.style.display = 'block';
                    if (moonIcon) moonIcon.style.display = 'none';
                } else {
                    document.body.setAttribute('data-theme', 'dark');
                    localStorage.setItem('theme', 'dark');
                    if (sunIcon) sunIcon.style.display = 'none';
                    if (moonIcon) moonIcon.style.display = 'block';
                }
            });
        }

        // Search functionality
        const searchData = [
            { id: 'installation-guide', title: 'Installation guide', desc: 'Step by step guide to install Gabi.' },
            { id: 'downloading-gabi', title: 'Downloading Gabi', desc: 'You can download the latest version from our GitHub Releases page.' },
            { id: 'how-to-use', title: 'How to Use', desc: 'Basic Usage, Instant Actions, Customization, Paste & Fetch, Configure, Download.' },
            { id: 'features', title: 'Key Features', desc: 'Wide Support, Smart Preview, Share to Gabi, Themes.' },
            { id: 'supported-sites', title: 'Supported Sites', desc: 'YouTube, TikTok, Instagram, Twitter, Reddit, Twitch, Facebook, SoundCloud, Pixiv, Pinterest, yt-dlp, gallery-dl, cobalt.' },
            { id: 'customization', title: 'Customization', desc: 'Themes, Lavender, Forest, Midnight, Rose, Monochrome, Material You, Dynamic Theme, Dark mode, Light mode, Storage Access Framework.' },
            { id: 'instant-action', title: 'Instant Action', desc: 'One-Tap Download, fetch links from clipboard, Share to Gabi, background processing.' },
            { id: 'building-from-source', title: 'Building from Source', desc: 'To build Gabi yourself: Clone the repository, Open in Android Studio, Build the project.' }
        ];

        const searchBtn = document.getElementById('searchBtn');
        const searchModal = document.getElementById('searchModal');
        const closeSearch = document.getElementById('closeSearch');
        const searchInput = document.getElementById('searchInput');
        const searchResults = document.getElementById('searchResults');

        function openSearch() {
            if (searchModal) searchModal.classList.add('active');
            if (searchInput) {
                searchInput.value = '';
                setTimeout(() => searchInput.focus(), 100);
            }
            renderResults(searchData);
        }

        function closeSearchModal() {
            if (searchModal) searchModal.classList.remove('active');
        }

        if (searchBtn) searchBtn.addEventListener('click', openSearch);
        if (closeSearch) closeSearch.addEventListener('click', closeSearchModal);
        if (searchModal) searchModal.addEventListener('click', (e) => {
            if (e.target === searchModal) closeSearchModal();
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'b' && (e.ctrlKey || e.metaKey)) {
                e.preventDefault();
                openSearch();
            }
            if (e.key === 'Escape') closeSearchModal();
        });

        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase();
                const filtered = searchData.filter(item => 
                    item.title.toLowerCase().includes(query) || 
                    item.desc.toLowerCase().includes(query)
                );
                renderResults(filtered);
            });
        }

        function renderResults(results) {
            if (!searchResults) return;
            searchResults.innerHTML = '';
            if (results.length === 0) {
                searchResults.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--vp-c-text-2);">No results found</div>';
                return;
            }
            results.forEach(item => {
                const a = document.createElement('a');
                a.href = '#' + item.id;
                a.className = 'search-result-item';
                a.innerHTML = `<div class="search-result-title">${item.title}</div><div class="search-result-desc">${item.desc}</div>`;
                a.addEventListener('click', () => {
                    closeSearchModal();
                });
                searchResults.appendChild(a);
            });
        }

        // Carousel logic
        const slides = document.querySelectorAll('.carousel-slide');
        const prevBtn = document.querySelector('.carousel-arrow.left');
        const nextBtn = document.querySelector('.carousel-arrow.right');
        const mockupContainer = document.querySelector('.phone-mockup');
        let currentSlide = 0;
        let slideInterval;
        
        function showSlide(index) {
            if (!slides || slides.length === 0) return;
            slides[currentSlide].classList.remove('active');
            currentSlide = (index + slides.length) % slides.length;
            slides[currentSlide].classList.add('active');
            
            const pill = document.getElementById('devicePill');
            
            if (slides[currentSlide].getAttribute('data-type') === 'tablet') {
                if (mockupContainer) mockupContainer.classList.add('tablet-mode');
                if (pill) pill.textContent = 'Tablet';
            } else {
                if (mockupContainer) mockupContainer.classList.remove('tablet-mode');
                if (pill) pill.textContent = 'Phone';
            }
        }
        
        function nextSlide() {
            showSlide(currentSlide + 1);
        }
        
        function startCarousel() {
            slideInterval = setInterval(nextSlide, 3000);
        }
        
        function resetCarousel() {
            clearInterval(slideInterval);
            startCarousel();
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                showSlide(currentSlide - 1);
                resetCarousel();
            });
        }
        
        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                nextSlide();
                resetCarousel();
            });
        }
        
        if (slides.length > 0) startCarousel();

        // Fetch all release versions from GitHub
        fetch('https://api.github.com/repos/Hotaro26/gabi/releases')
            .then(response => response.json())
            .then(data => {
                if (data && data.length > 0) {
                    const latest = data[0];
                    const versionLink = document.getElementById('versionLink');
                    if (versionLink) {
                        versionLink.href = latest.html_url;
                        versionLink.innerHTML = `Get ${latest.tag_name} <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>`;
                    }
                    
                    const versionMenu = document.getElementById('versionMenu');
                    if (versionMenu) {
                        data.forEach(release => {
                            const a = document.createElement('a');
                            a.href = release.html_url;
                            a.textContent = release.tag_name;
                            versionMenu.appendChild(a);
                        });
                    }
                }
            })
            .catch(error => console.error('Error fetching releases:', error));

        // Fetch news feed
        const newsFeed = document.getElementById('newsFeed');
        if (newsFeed) {
            fetch('https://api.github.com/repos/Hotaro26/gabi/releases')
                .then(response => response.json())
                .then(data => {
                    newsFeed.innerHTML = '';
                    if (data && data.length > 0) {
                        data.forEach(release => {
                            const date = new Date(release.published_at).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
                            
                            // Convert markdown to simple HTML (basic implementation)
                            let bodyHtml = release.body
                                .replace(/^### (.*$)/gim, '<h3>$1</h3>')
                                .replace(/^## (.*$)/gim, '<h2>$1</h2>')
                                .replace(/^# (.*$)/gim, '<h1>$1</h1>')
                                .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
                                .replace(/\*(.*)\*/gim, '<em>$1</em>')
                                .replace(/\[(.*?)\]\((.*?)\)/gim, "<a href='$2'>$1</a>")
                                .replace(/\\n/gim, '<br>');
                                
                            const card = document.createElement('div');
                            card.className = 'release-card';
                            card.innerHTML = `
                                <div class="release-header">
                                    <h2 class="release-title"><a href="${release.html_url}" target="_blank">${release.name || release.tag_name}</a></h2>
                                    <div style="display: flex; gap: 12px; align-items: center;">
                                        <span class="release-tag">${release.tag_name}</span>
                                        <span class="release-date">${date}</span>
                                    </div>
                                </div>
                                <div class="markdown-body">
                                    ${bodyHtml}
                                </div>
                            `;
                            newsFeed.appendChild(card);
                        });
                    } else {
                        newsFeed.innerHTML = '<div style="text-align:center; padding: 40px; color: var(--vp-c-text-2);">No releases found.</div>';
                    }
                })
                .catch(error => {
                    console.error('Error fetching releases for news:', error);
                    newsFeed.innerHTML = '<div style="text-align:center; padding: 40px; color: #ff5555;">Failed to load news. Check console.</div>';
                });
        }

        // Accent Color Picker Logic
        const colorBtns = document.querySelectorAll('.color-btn');
        const customColorPicker = document.getElementById('customColorPicker');
        const root = document.documentElement;

        const savedColor = localStorage.getItem('accent-color');
        if (savedColor) {
            root.style.setProperty('--vp-c-brand', savedColor);
            let matched = false;
            colorBtns.forEach(btn => {
                if (btn.getAttribute('data-color') === savedColor) {
                    btn.classList.add('active');
                    matched = true;
                }
            });
            if (!matched && customColorPicker) {
                customColorPicker.value = savedColor;
                document.querySelectorAll('.custom-color-btn').forEach(b => b.classList.add('active'));
            }
        }

        colorBtns.forEach(btn => {
            if (!btn.hasAttribute('data-color')) return;
            btn.addEventListener('click', () => {
                const color = btn.getAttribute('data-color');
                root.style.setProperty('--vp-c-brand', color);
                localStorage.setItem('accent-color', color);
                
                colorBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            });
        });

        if (customColorPicker) {
            customColorPicker.addEventListener('input', (e) => {
                const color = e.target.value;
                root.style.setProperty('--vp-c-brand', color);
                localStorage.setItem('accent-color', color);
                
                colorBtns.forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.custom-color-btn').forEach(b => b.classList.add('active'));
            });
        }


function showView(viewId) {
    document.querySelectorAll('.view-section').forEach(el => el.style.display = 'none');
    document.getElementById(viewId).style.display = 'block';
    window.scrollTo(0, 0);
    document.querySelectorAll('.nav-link').forEach(link => { link.style.color = ''; });
}
function handleHash() {
    let hash = window.location.hash.substring(1);
    if (!hash) hash = 'home';
    const parts = hash.split('?')[0].split('#');
    const view = parts[0];
    if (view === 'docs') showView('docs-view');
    else if (view === 'news') showView('news-view');
    else showView('home-view');
    
    if (parts.length > 1) {
        setTimeout(() => {
            const el = document.getElementById(parts[1]);
            if(el) el.scrollIntoView();
        }, 100);
    }
}
window.addEventListener('load', handleHash);
window.addEventListener('hashchange', handleHash);

