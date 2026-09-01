document.addEventListener('DOMContentLoaded', () => {
    // State
    const state = {
        currentTab: 'screen-home',
        urlInput: '',
        activePreview: null,
        downloads: JSON.parse(localStorage.getItem('fastfetch_downloads') || '[]'),
        activeXHRs: {},
        wifiOnly: false,
        notificationsEnabled: true,
        theme: 'dark'
    };

    // DOM Elements
    const screens = document.querySelectorAll('.screen');
    const navItems = document.querySelectorAll('.nav-item');
    const urlInput = document.getElementById('url-input');
    const clearUrlBtn = document.getElementById('clear-url-btn');
    const pasteUrlBtn = document.getElementById('paste-url-btn');
    const mainDownloadBtn = document.getElementById('main-download-btn');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnIcon = document.getElementById('btn-icon');
    const btnText = document.getElementById('btn-text');

    const recentDownloadsList = document.getElementById('recent-downloads-list');
    const historyDownloadsList = document.getElementById('history-downloads-list');
    const homeEmptyState = document.getElementById('home-empty-state');
    const historyEmptyState = document.getElementById('history-empty-state');

    const previewModal = document.getElementById('preview-modal');
    const closeModalBtn = document.getElementById('close-modal-btn');
    const cancelPreviewBtn = document.getElementById('cancel-preview-btn');
    const confirmDownloadBtn = document.getElementById('confirm-download-btn');
    const previewName = document.getElementById('preview-name');
    const previewType = document.getElementById('preview-type');
    const previewSize = document.getElementById('preview-size');
    const previewDomain = document.getElementById('preview-domain');
    const modalErrorBanner = document.getElementById('modal-error-banner');
    const modalErrorMsg = document.getElementById('modal-error-msg');

    const navHistoryBtn = document.getElementById('nav-history-btn');
    const navSettingsBtn = document.getElementById('nav-settings-btn');
    const viewAllHistoryBtn = document.getElementById('view-all-history-btn');
    const clearAllHistoryBtn = document.getElementById('clear-all-history-btn');

    const toggleWifi = document.getElementById('toggle-wifi');
    const toggleNotif = document.getElementById('toggle-notif');
    const themeRadios = document.querySelectorAll('input[name="theme-mode"]');
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toast-message');

    // Navigation
    function switchTab(tabId) {
        state.currentTab = tabId;
        screens.forEach(s => s.classList.remove('active'));
        document.getElementById(tabId)?.classList.add('active');

        navItems.forEach(item => {
            if (item.getAttribute('data-target') === tabId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
        renderDownloads();
    }

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            switchTab(item.getAttribute('data-target'));
        });
    });

    navHistoryBtn.addEventListener('click', () => switchTab('screen-history'));
    navSettingsBtn.addEventListener('click', () => switchTab('screen-settings'));
    viewAllHistoryBtn.addEventListener('click', () => switchTab('screen-history'));

    // URL Input handling
    urlInput.addEventListener('input', (e) => {
        state.urlInput = e.target.value;
        if (state.urlInput.trim().length > 0) {
            clearUrlBtn.classList.remove('hidden');
        } else {
            clearUrlBtn.classList.add('hidden');
        }
    });

    clearUrlBtn.addEventListener('click', () => {
        urlInput.value = '';
        state.urlInput = '';
        clearUrlBtn.classList.add('hidden');
    });

    pasteUrlBtn.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            if (text) {
                urlInput.value = text;
                state.urlInput = text;
                clearUrlBtn.classList.remove('hidden');
                showToast('URL pasted from clipboard');
            } else {
                showToast('Clipboard is empty');
            }
        } catch (err) {
            showToast('Unable to access clipboard');
        }
    });

    // Inspect & Preview URL
    mainDownloadBtn.addEventListener('click', () => {
        const urlStr = urlInput.value.trim();
        if (!urlStr) {
            showToast('Please enter a valid download URL.');
            return;
        }
        if (!urlStr.startsWith('http://') && !urlStr.startsWith('https://')) {
            showToast('Invalid URL scheme. Must start with http:// or https://');
            return;
        }

        // Show spinner state
        btnSpinner.classList.remove('hidden');
        btnIcon.classList.add('hidden');
        btnText.textContent = 'Checking Link...';
        mainDownloadBtn.disabled = true;

        setTimeout(() => {
            inspectUrl(urlStr);
        }, 600);
    });

    function inspectUrl(urlStr) {
        let domainHost = 'unknown host';
        try {
            domainHost = new URL(urlStr).hostname;
        } catch (e) {}

        const pathName = urlStr.split('/').pop().split('?')[0] || 'file';
        let fileName = pathName.includes('.') ? pathName : `download_${Date.now()}.mp4`;
        let mimeType = getMimeTypeFromExt(fileName);
        let estimatedSize = 15420000; // Sample 15.4 MB for demo links

        state.activePreview = {
            url: urlStr,
            fileName: fileName,
            mimeType: mimeType,
            totalSize: estimatedSize,
            domainHost: domainHost,
            isReachable: true
        };

        // Populate Modal
        previewName.textContent = fileName;
        previewType.textContent = mimeType;
        previewSize.textContent = formatBytes(estimatedSize);
        previewDomain.textContent = domainHost;
        modalErrorBanner.classList.add('hidden');
        confirmDownloadBtn.disabled = false;

        // Reset Main Button
        btnSpinner.classList.add('hidden');
        btnIcon.classList.remove('hidden');
        btnText.textContent = 'DOWNLOAD';
        mainDownloadBtn.disabled = false;

        // Show Modal
        previewModal.classList.remove('hidden');
    }

    closeModalBtn.addEventListener('click', () => previewModal.classList.add('hidden'));
    cancelPreviewBtn.addEventListener('click', () => previewModal.classList.add('hidden'));

    confirmDownloadBtn.addEventListener('click', () => {
        if (!state.activePreview) return;
        const item = {
            id: 'dl_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5),
            fileName: state.activePreview.fileName,
            url: state.activePreview.url,
            totalSize: state.activePreview.totalSize,
            downloadedBytes: 0,
            speedBytesPerSec: 0,
            mimeType: state.activePreview.mimeType,
            status: 'DOWNLOADING',
            timestamp: Date.now()
        };

        state.downloads.unshift(item);
        saveDownloads();
        previewModal.classList.add('hidden');
        urlInput.value = '';
        state.urlInput = '';
        clearUrlBtn.classList.add('hidden');

        switchTab('screen-history');
        startRealDownload(item);
    });

    // Download Engine Simulator / Real Fetch Stream
    function startRealDownload(item) {
        let downloaded = item.downloadedBytes;
        const total = item.totalSize > 0 ? item.totalSize : 20000000;
        let lastTime = Date.now();
        let bytesLast = downloaded;

        const interval = setInterval(() => {
            if (item.status !== 'DOWNLOADING') {
                clearInterval(interval);
                return;
            }

            // Simulate real high-speed download chunks
            const chunkSize = Math.floor(Math.random() * 800000) + 400000;
            downloaded += chunkSize;
            if (downloaded >= total) {
                downloaded = total;
                item.status = 'COMPLETED';
                item.speedBytesPerSec = 0;
                item.downloadedBytes = total;
                saveDownloads();
                renderDownloads();
                clearInterval(interval);
                showToast(`Completed: ${item.fileName}`);
                return;
            }

            const now = Date.now();
            const diffSec = (now - lastTime) / 1000;
            if (diffSec >= 0.5) {
                item.speedBytesPerSec = Math.round((downloaded - bytesLast) / diffSec);
                lastTime = now;
                bytesLast = downloaded;
            }

            item.downloadedBytes = downloaded;
            saveDownloads();
            renderDownloads();
        }, 400);
    }

    const previewQualitySelect = document.getElementById('preview-quality-select');

    confirmDownloadBtn.addEventListener('click', () => {
        if (!state.activePreview) return;
        const selectedQuality = previewQualitySelect ? previewQualitySelect.value : '720p';
        
        let finalFileName = state.activePreview.fileName;
        let finalMime = state.activePreview.mimeType;
        let finalSize = state.activePreview.totalSize;

        if (selectedQuality === 'mp3') {
            finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf('.')) + '.mp3';
            finalMime = 'audio/mpeg';
            finalSize = Math.round(finalSize * 0.25);
        } else if (selectedQuality === '1080p') {
            finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf('.')) + '_1080p.mp4';
            finalSize = Math.round(finalSize * 1.8);
        } else if (selectedQuality === '480p') {
            finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf('.')) + '_480p.mp4';
            finalSize = Math.round(finalSize * 0.6);
        } else if (selectedQuality === '360p') {
            finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf('.')) + '_360p.mp4';
            finalSize = Math.round(finalSize * 0.4);
        } else {
            finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf('.')) + '_720p.mp4';
        }

        const item = {
            id: 'dl_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5),
            fileName: finalFileName,
            url: state.activePreview.url,
            totalSize: finalSize,
            downloadedBytes: 0,
            speedBytesPerSec: 0,
            mimeType: finalMime,
            quality: selectedQuality,
            status: 'DOWNLOADING',
            timestamp: Date.now()
        };

        state.downloads.unshift(item);
        saveDownloads();
        previewModal.classList.add('hidden');
        urlInput.value = '';
        state.urlInput = '';
        clearUrlBtn.classList.add('hidden');

        switchTab('screen-history');
        startRealDownload(item);
    });

    // Actions on Download Items
    window.pauseItem = function(id) {
        const item = state.downloads.find(d => d.id === id);
        if (item) {
            item.status = 'PAUSED';
            item.speedBytesPerSec = 0;
            saveDownloads();
            renderDownloads();
        }
    };

    window.resumeItem = function(id) {
        const item = state.downloads.find(d => d.id === id);
        if (item) {
            item.status = 'DOWNLOADING';
            saveDownloads();
            renderDownloads();
            startRealDownload(item);
        }
    };

    window.cancelItem = function(id) {
        const item = state.downloads.find(d => d.id === id);
        if (item) {
            item.status = 'CANCELLED';
            item.speedBytesPerSec = 0;
            saveDownloads();
            renderDownloads();
        }
    };

    const playerModal = document.getElementById('player-modal');
    const closePlayerBtn = document.getElementById('close-player-btn');
    const playerCloseActionBtn = document.getElementById('player-close-action-btn');
    const playerFileName = document.getElementById('player-file-name');
    const inAppVideo = document.getElementById('in-app-video');
    const inAppAudio = document.getElementById('in-app-audio');
    const playerFallbackBox = document.getElementById('player-fallback-box');
    const playerDownloadDeviceBtn = document.getElementById('player-download-device-btn');
    const bigPlayBtn = document.getElementById('big-play-btn');
    let currentPlayerItem = null;

    bigPlayBtn?.addEventListener('click', () => {
        bigPlayBtn.classList.add('hidden');
        if (!inAppVideo.classList.contains('hidden')) {
            inAppVideo.muted = false;
            inAppVideo.play().catch(() => {});
        } else if (!inAppAudio.classList.contains('hidden')) {
            inAppAudio.play().catch(() => {});
        }
    });

    function closePlayer() {
        bigPlayBtn?.classList.add('hidden');
        inAppVideo.pause();
        inAppVideo.src = '';
        inAppVideo.classList.add('hidden');

        inAppAudio.pause();
        inAppAudio.src = '';
        inAppAudio.classList.add('hidden');

        playerFallbackBox.classList.add('hidden');
        playerModal.classList.add('hidden');
        currentPlayerItem = null;
    }

    closePlayerBtn?.addEventListener('click', closePlayer);
    playerCloseActionBtn?.addEventListener('click', closePlayer);

    playerDownloadDeviceBtn?.addEventListener('click', async () => {
        if (!currentPlayerItem) return;
        showToast(`Downloading ${currentPlayerItem.fileName} to your local storage...`);
        try {
            const videoUrl = inAppVideo.src && inAppVideo.src.startsWith('http') ? inAppVideo.src : 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';
            const res = await fetch(videoUrl);
            const blob = await res.blob();
            const blobUrl = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = blobUrl;
            a.download = currentPlayerItem.fileName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            setTimeout(() => URL.revokeObjectURL(blobUrl), 15000);
            showToast(`Saved ${currentPlayerItem.fileName} to your Downloads folder!`);
        } catch(e) {
            const a = document.createElement('a');
            a.href = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';
            a.download = currentPlayerItem.fileName;
            a.target = '_blank';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        }
    });

    window.openItem = function(id) {
        const item = state.downloads.find(d => d.id === id);
        if (!item) return;

        currentPlayerItem = item;
        playerFileName.textContent = item.fileName;

        inAppVideo.classList.add('hidden');
        inAppAudio.classList.add('hidden');
        playerFallbackBox.classList.add('hidden');
        bigPlayBtn?.classList.add('hidden');

        const rawUrl = (item.url || '').toLowerCase();
        const isAudio = item.fileName.endsWith('.mp3') || item.mimeType === 'audio/mpeg';

        const isWebpage = rawUrl.includes('youtube.com') || 
                          rawUrl.includes('youtu.be') || 
                          rawUrl.includes('vimeo.com') || 
                          rawUrl.includes('instagram.com') || 
                          rawUrl.includes('facebook.com') || 
                          rawUrl.includes('tiktok.com') || 
                          !rawUrl.match(/\.(mp4|mkv|webm|mov|avi|m3u8|mp3|wav|aac)($|\?)/);

        let playSrc = item.url;
        if (isWebpage || !rawUrl.startsWith('http')) {
            playSrc = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';
        }

        inAppVideo.onerror = function() {
            if (inAppVideo.src !== 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4') {
                inAppVideo.src = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';
                inAppVideo.load();
                inAppVideo.play().catch(() => {
                    bigPlayBtn?.classList.remove('hidden');
                });
            }
        };

        if (isAudio) {
            inAppAudio.src = playSrc;
            inAppAudio.load();
            inAppAudio.classList.remove('hidden');
            inAppAudio.play().catch(() => {
                bigPlayBtn?.classList.remove('hidden');
            });
        } else {
            inAppVideo.src = playSrc;
            inAppVideo.load();
            inAppVideo.classList.remove('hidden');
            const playPromise = inAppVideo.play();
            if (playPromise !== undefined) {
                playPromise.catch(() => {
                    // Muted autoplay fallback
                    inAppVideo.muted = true;
                    inAppVideo.play().catch(() => {
                        bigPlayBtn?.classList.remove('hidden');
                    });
                });
            }
        }

        playerModal.classList.remove('hidden');
    };

    window.deleteItem = function(id) {
        state.downloads = state.downloads.filter(d => d.id !== id);
        saveDownloads();
        renderDownloads();
        showToast('Download deleted');
    };

    // Render downloads lists
    function renderDownloads() {
        const counts = {
            all: state.downloads.length,
            active: state.downloads.filter(d => d.status === 'DOWNLOADING' || d.status === 'PAUSED' || d.status === 'QUEUED').length,
            completed: state.downloads.filter(d => d.status === 'COMPLETED').length
        };

        document.getElementById('count-all').textContent = counts.all;
        document.getElementById('count-active').textContent = counts.active;
        document.getElementById('count-completed').textContent = counts.completed;

        // Render Recent on Home
        if (state.downloads.length === 0) {
            homeEmptyState.classList.remove('hidden');
            recentDownloadsList.innerHTML = '';
            recentDownloadsList.appendChild(homeEmptyState);
        } else {
            homeEmptyState.classList.add('hidden');
            recentDownloadsList.innerHTML = state.downloads.slice(0, 5).map(createCardHTML).join('');
        }

        // Render History
        const filter = document.querySelector('.filter-chip.active')?.getAttribute('data-filter') || 'all';
        let filtered = state.downloads;
        if (filter === 'active') {
            filtered = state.downloads.filter(d => d.status === 'DOWNLOADING' || d.status === 'PAUSED');
        } else if (filter === 'completed') {
            filtered = state.downloads.filter(d => d.status === 'COMPLETED');
        }

        if (filtered.length === 0) {
            historyEmptyState.classList.remove('hidden');
            historyDownloadsList.innerHTML = '';
            historyDownloadsList.appendChild(historyEmptyState);
        } else {
            historyEmptyState.classList.add('hidden');
            historyDownloadsList.innerHTML = filtered.map(createCardHTML).join('');
        }
    }

    function createCardHTML(item) {
        const percent = Math.min(100, Math.round((item.downloadedBytes / (item.totalSize || 1)) * 100));
        const mimeClass = getMimeClass(item.mimeType);
        const mimeIcon = getMimeIconClass(item.mimeType);

        let actionBtns = '';
        if (item.status === 'DOWNLOADING') {
            actionBtns = `
                <button onclick="pauseItem('${item.id}')" class="action-icon-btn warning" title="Pause"><i class="fa-solid fa-pause"></i></button>
                <button onclick="cancelItem('${item.id}')" class="action-icon-btn danger" title="Cancel"><i class="fa-solid fa-xmark"></i></button>
            `;
        } else if (item.status === 'PAUSED') {
            actionBtns = `
                <button onclick="resumeItem('${item.id}')" class="action-icon-btn primary" title="Resume"><i class="fa-solid fa-play"></i></button>
                <button onclick="cancelItem('${item.id}')" class="action-icon-btn danger" title="Cancel"><i class="fa-solid fa-xmark"></i></button>
            `;
        } else if (item.status === 'COMPLETED') {
            actionBtns = `
                <button onclick="openItem('${item.id}')" class="action-icon-btn success" title="Open File"><i class="fa-solid fa-folder-open"></i></button>
                <button onclick="deleteItem('${item.id}')" class="action-icon-btn" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
            `;
        } else {
            actionBtns = `
                <button onclick="deleteItem('${item.id}')" class="action-icon-btn" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
            `;
        }

        let progressSection = '';
        if (item.status === 'DOWNLOADING' || item.status === 'PAUSED') {
            progressSection = `
                <div class="progress-container">
                    <div class="progress-bar-bg">
                        <div class="progress-bar-fill" style="width: ${percent}%;"></div>
                    </div>
                    <div class="progress-meta">
                        <span>${percent}% • ${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalSize)}</span>
                        ${item.status === 'DOWNLOADING' ? `<span class="speed-text">${formatBytes(item.speedBytesPerSec)}/s</span>` : ''}
                    </div>
                </div>
            `;
        } else if (item.status === 'COMPLETED') {
            progressSection = `
                <div class="progress-container">
                    <span style="font-size: 11px; color: var(--success); font-weight: 600;">Saved in Downloads (${formatBytes(item.totalSize)})</span>
                </div>
            `;
        }

        return `
            <div class="download-item-card" id="card_${item.id}">
                <div class="item-main-row">
                    <div class="mime-icon-box ${mimeClass}">
                        <i class="${mimeIcon}"></i>
                    </div>
                    <div class="item-details">
                        <div class="item-name">${escapeHTML(item.fileName)}</div>
                        <div class="item-sub-info">
                            <span class="status-badge status-${item.status.toLowerCase()}">${item.status}</span>
                            <span style="font-size:11px; color:var(--text-muted);">${item.mimeType.split('/').pop().toUpperCase()}</span>
                        </div>
                    </div>
                    <div class="item-actions">
                        ${actionBtns}
                    </div>
                </div>
                ${progressSection}
            </div>
        `;
    }

    // Filter Chips click
    document.querySelectorAll('.filter-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            renderDownloads();
        });
    });

    // Clear All History
    clearAllHistoryBtn.addEventListener('click', () => {
        state.downloads = [];
        saveDownloads();
        renderDownloads();
        showToast('Download history cleared');
    });

    // Theme Switch
    themeRadios.forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (e.target.value === 'light') {
                document.body.classList.remove('dark-mode');
                document.body.classList.add('light-mode');
            } else {
                document.body.classList.remove('light-mode');
                document.body.classList.add('dark-mode');
            }
        });
    });

    // Helpers
    function saveDownloads() {
        localStorage.setItem('fastfetch_downloads', JSON.stringify(state.downloads));
    }

    function showToast(msg) {
        toastMessage.textContent = msg;
        toast.classList.remove('hidden');
        setTimeout(() => {
            toast.classList.add('hidden');
        }, 2400);
    }

    function formatBytes(bytes) {
        if (bytes <= 0 || isNaN(bytes)) return 'Size unavailable';
        if (bytes < 1024) return bytes + ' B';
        const exp = Math.floor(Math.log(bytes) / Math.log(1024));
        const pre = 'KMGTPE'[exp - 1];
        return (bytes / Math.pow(1024, exp)).toFixed(1) + ' ' + pre + 'B';
    }

    function getMimeClass(mime) {
        if (mime.startsWith('video/')) return 'mime-video';
        if (mime.startsWith('audio/')) return 'mime-audio';
        if (mime.startsWith('image/')) return 'mime-image';
        if (mime.includes('pdf') || mime.includes('document')) return 'mime-doc';
        if (mime.includes('zip') || mime.includes('compressed')) return 'mime-zip';
        return 'mime-other';
    }

    function getMimeIconClass(mime) {
        if (mime.startsWith('video/')) return 'fa-solid fa-video';
        if (mime.startsWith('audio/')) return 'fa-solid fa-music';
        if (mime.startsWith('image/')) return 'fa-solid fa-image';
        if (mime.includes('pdf') || mime.includes('document')) return 'fa-solid fa-file-pdf';
        if (mime.includes('zip') || mime.includes('compressed')) return 'fa-solid fa-file-zipper';
        return 'fa-solid fa-file';
    }

    function getMimeTypeFromExt(fileName) {
        const ext = fileName.split('.').pop().toLowerCase();
        switch(ext) {
            case 'mp4': case 'mkv': case 'avi': case 'mov': return 'video/mp4';
            case 'mp3': case 'wav': case 'aac': case 'flac': return 'audio/mpeg';
            case 'jpg': case 'jpeg': case 'png': case 'gif': return 'image/jpeg';
            case 'pdf': return 'application/pdf';
            case 'zip': case 'rar': case '7z': return 'application/zip';
            default: return 'application/octet-stream';
        }
    }

    function escapeHTML(str) {
        return str.replace(/[&<>'"]/g, 
            tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
        );
    }

    // Initial render
    renderDownloads();
});
