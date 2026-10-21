/**
 * DashboardManager - Full CRUD Operations & Analytics for Transcription History
 */
class DashboardManager {
  constructor(options = {}) {
    this.container = document.getElementById('historyGridContainer');
    this.emptyState = document.getElementById('historyEmptyState');
    this.onRecordSelect = options.onRecordSelect || (() => {});
    this.showToast = options.showToast || console.log;

    this.currentPage = 0;
    this.pageSize = 12;
    this.currentSearch = '';
    this.currentLanguage = 'all';
    this.filterFavorite = false;
    this.currentSort = 'createdAt-desc';

    this.debounceTimer = null;
    this.initEventListeners();
  }

  initEventListeners() {
    // Search with debounce
    const searchInput = document.getElementById('historySearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        clearTimeout(this.debounceTimer);
        this.debounceTimer = setTimeout(() => {
          this.currentSearch = e.target.value.trim();
          this.currentPage = 0;
          this.loadTranscriptions();
        }, 300);
      });
    }

    // Language Filter
    const langSelect = document.getElementById('filterLanguageSelect');
    if (langSelect) {
      langSelect.addEventListener('change', (e) => {
        this.currentLanguage = e.target.value;
        this.currentPage = 0;
        this.loadTranscriptions();
      });
    }

    // Favorite Filter
    const favBtn = document.getElementById('filterFavoriteBtn');
    if (favBtn) {
      favBtn.addEventListener('click', () => {
        this.filterFavorite = !this.filterFavorite;
        favBtn.classList.toggle('active', this.filterFavorite);
        favBtn.style.borderColor = this.filterFavorite ? 'var(--primary)' : 'var(--border-color)';
        this.currentPage = 0;
        this.loadTranscriptions();
      });
    }

    // Sort Dropdown
    const sortSelect = document.getElementById('historySortSelect');
    if (sortSelect) {
      sortSelect.addEventListener('change', (e) => {
        this.currentSort = e.target.value;
        this.loadTranscriptions();
      });
    }

    // Refresh Button
    const refreshBtn = document.getElementById('refreshHistoryBtn');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        this.loadTranscriptions();
        this.loadStats();
        this.showToast('History refreshed', 'info');
      });
    }

    // Export Dropdown Trigger
    const exportBtn = document.getElementById('exportMenuBtn');
    const exportMenu = document.getElementById('exportDropdownMenu');
    if (exportBtn && exportMenu) {
      exportBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        exportMenu.style.display = exportMenu.style.display === 'block' ? 'none' : 'block';
      });

      document.addEventListener('click', () => {
        exportMenu.style.display = 'none';
      });

      exportMenu.querySelectorAll('.dropdown-item').forEach(item => {
        item.addEventListener('click', (e) => {
          e.preventDefault();
          const format = item.getAttribute('data-export');
          window.location.href = `/api/transcriptions/export?format=${format}`;
          exportMenu.style.display = 'none';
          this.showToast(`Downloading transcriptions as ${format.toUpperCase()}...`, 'success');
        });
      });
    }

    // Confirm Edit Modal Button
    const confirmEditBtn = document.getElementById('confirmEditBtn');
    if (confirmEditBtn) {
      confirmEditBtn.addEventListener('click', () => this.handleSaveEdit());
    }
  }

  async loadStats() {
    try {
      const res = await fetch('/api/transcriptions/stats');
      if (!res.ok) throw new Error('Stats fetch error');
      const data = await res.json();

      const totalCount = document.getElementById('statsTotalCount');
      const totalWords = document.getElementById('statsTotalWords');
      const favoriteCount = document.getElementById('favoriteCountSpan');

      if (totalCount) totalCount.textContent = data.totalTranscriptions.toLocaleString();
      if (totalWords) totalWords.textContent = data.totalWords.toLocaleString();
      if (favoriteCount) favoriteCount.textContent = data.favoriteCount;

    } catch (err) {
      console.warn('Failed to load stats:', err);
    }
  }

  async loadTranscriptions() {
    if (!this.container) return;

    try {
      const [sortBy, sortDir] = this.currentSort.split('-');
      const params = new URLSearchParams({
        page: this.currentPage,
        size: this.pageSize,
        sortBy: sortBy || 'createdAt',
        sortDir: sortDir || 'desc'
      });

      if (this.currentSearch) params.append('keyword', this.currentSearch);
      if (this.currentLanguage && this.currentLanguage !== 'all') params.append('language', this.currentLanguage);
      if (this.filterFavorite) params.append('favorite', 'true');

      const res = await fetch(`/api/transcriptions?${params.toString()}`);
      if (!res.ok) throw new Error('Failed to fetch transcriptions');

      const pageData = await res.json();
      this.renderList(pageData.content || []);
      this.loadStats();

    } catch (err) {
      console.error('Error loading transcriptions:', err);
      this.showToast('Could not load transcriptions', 'error');
    }
  }

  renderList(items) {
    this.container.innerHTML = '';

    if (!items || items.length === 0) {
      if (this.emptyState) this.emptyState.style.display = 'block';
      return;
    }

    if (this.emptyState) this.emptyState.style.display = 'none';

    items.forEach(item => {
      const card = this.createCardElement(item);
      this.container.appendChild(card);
    });
  }

  createCardElement(item) {
    const card = document.createElement('div');
    card.className = 'history-card';
    card.id = `historyCard-${item.id}`;

    // Language badge class
    let badgeClass = 'badge-hinglish';
    if (item.detectedLanguage.includes('Hindi') && !item.detectedLanguage.includes('English')) {
      badgeClass = 'badge-hindi';
    } else if (item.detectedLanguage.includes('English') && !item.detectedLanguage.includes('Hindi')) {
      badgeClass = 'badge-english';
    }

    // Ratio percentage breakdown
    let hindiPercent = 50;
    let englishPercent = 50;
    if (item.languageRatio) {
      const hMatch = item.languageRatio.match(/Hindi:\s*(\d+)%/);
      const eMatch = item.languageRatio.match(/English:\s*(\d+)%/);
      if (hMatch) hindiPercent = parseInt(hMatch[1], 10);
      if (eMatch) englishPercent = parseInt(eMatch[1], 10);
    }

    const isStarred = item.isFavorite ? 'starred' : '';
    const starIcon = item.isFavorite ? '★' : '☆';

    card.innerHTML = `
      <div class="history-card-header">
        <h4 class="history-card-title">${this.escapeHtml(item.title || 'Untitled Transcription')}</h4>
        <button class="star-btn ${isStarred}" data-id="${item.id}" title="Toggle Star/Favorite">${starIcon}</button>
      </div>

      <div class="history-card-meta">
        <span class="badge ${badgeClass}">${this.escapeHtml(item.detectedLanguage)}</span>
        <span>⏱️ ${item.formattedCreatedAt || 'Recent'}</span>
        <span>📝 ${item.wordCount || 0} words</span>
      </div>

      <div class="ratio-bar-wrapper">
        <div class="ratio-label">
          <span>Hindi: ${hindiPercent}%</span>
          <span>English: ${englishPercent}%</span>
        </div>
        <div class="ratio-progress-track">
          <div class="ratio-hindi-fill" style="width: ${hindiPercent}%;"></div>
          <div class="ratio-english-fill" style="width: ${englishPercent}%;"></div>
        </div>
      </div>

      <div class="history-card-body">${this.escapeHtml(item.content)}</div>

      <div class="history-card-footer">
        <div class="history-card-actions">
          <button class="btn btn-outline" style="padding: 0.35rem 0.65rem; font-size: 0.78rem;" data-action="tts" data-id="${item.id}" title="Listen using Text-to-Speech">
            🔊 Play
          </button>
          <button class="btn btn-outline" style="padding: 0.35rem 0.65rem; font-size: 0.78rem;" data-action="copy" data-id="${item.id}" title="Copy to clipboard">
            📋 Copy
          </button>
        </div>

        <div class="history-card-actions">
          <button class="btn btn-outline" style="padding: 0.35rem 0.65rem; font-size: 0.78rem;" data-action="edit" data-id="${item.id}" title="Edit record">
            ✏️
          </button>
          <button class="btn btn-danger" style="padding: 0.35rem 0.65rem; font-size: 0.78rem;" data-action="delete" data-id="${item.id}" title="Delete record">
            🗑️
          </button>
        </div>
      </div>
    `;

    // Attach card action listeners
    const starBtn = card.querySelector('.star-btn');
    starBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.toggleFavorite(item.id, starBtn);
    });

    card.querySelector('[data-action="tts"]').addEventListener('click', () => {
      if (window.speechService) {
        window.speechService.speakText(item.content);
        this.showToast('Speaking transcription...', 'info');
      }
    });

    card.querySelector('[data-action="copy"]').addEventListener('click', () => {
      navigator.clipboard.writeText(item.content);
      this.showToast('Transcript copied to clipboard!', 'success');
    });

    card.querySelector('[data-action="edit"]').addEventListener('click', () => {
      this.openEditModal(item);
    });

    card.querySelector('[data-action="delete"]').addEventListener('click', () => {
      this.deleteRecord(item.id);
    });

    return card;
  }

  async toggleFavorite(id, buttonEl) {
    try {
      const res = await fetch(`/api/transcriptions/${id}/favorite`, { method: 'PATCH' });
      if (!res.ok) throw new Error('Favorite toggle failed');
      const updated = await res.json();

      buttonEl.classList.toggle('starred', updated.isFavorite);
      buttonEl.textContent = updated.isFavorite ? '★' : '☆';
      this.loadStats();
      this.showToast(updated.isFavorite ? 'Marked as favorite' : 'Removed from favorites', 'info');
    } catch (err) {
      this.showToast('Failed to update favorite', 'error');
    }
  }

  openEditModal(item) {
    document.getElementById('editModalId').value = item.id;
    document.getElementById('editModalTitle').value = item.title || '';
    document.getElementById('editModalContent').value = item.content || '';
    document.getElementById('editModalTags').value = item.tags || '';
    document.getElementById('editModalNotes').value = item.notes || '';

    const modal = document.getElementById('editModalOverlay');
    if (modal) modal.classList.add('active');
  }

  async handleSaveEdit() {
    const id = document.getElementById('editModalId').value;
    const title = document.getElementById('editModalTitle').value.trim();
    const content = document.getElementById('editModalContent').value.trim();
    const tags = document.getElementById('editModalTags').value.trim();
    const notes = document.getElementById('editModalNotes').value.trim();

    if (!content) {
      this.showToast('Transcript content cannot be empty', 'error');
      return;
    }

    try {
      const res = await fetch(`/api/transcriptions/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, content, tags, notes })
      });

      if (!res.ok) throw new Error('Update failed');

      const modal = document.getElementById('editModalOverlay');
      if (modal) modal.classList.remove('active');

      this.showToast('Transcription updated successfully!', 'success');
      this.loadTranscriptions();

    } catch (err) {
      this.showToast('Failed to update transcription', 'error');
    }
  }

  async deleteRecord(id) {
    if (!confirm('Are you sure you want to delete this transcription? This cannot be undone.')) {
      return;
    }

    try {
      const res = await fetch(`/api/transcriptions/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Delete failed');

      this.showToast('Transcription deleted', 'success');
      this.loadTranscriptions();
      this.loadStats();
    } catch (err) {
      this.showToast('Failed to delete transcription', 'error');
    }
  }

  escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}

window.DashboardManager = DashboardManager;
