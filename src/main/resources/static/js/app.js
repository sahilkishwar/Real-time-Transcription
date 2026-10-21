/**
 * App.js - Main Application Orchestrator
 */
document.addEventListener('DOMContentLoaded', () => {
  // State
  let fullFinalTranscript = '';
  let liveInterimTranscript = '';
  let recordingStartTime = null;
  let timerInterval = null;
  let socket = null;
  let wsConnected = false;

  // Initialize Modules
  const visualizer = new AudioVisualizer('audioVisualizerCanvas');
  const dashboard = new DashboardManager({
    showToast: showToast
  });

  const speechService = new SpeechService({
    onInterimResult: handleInterimTranscript,
    onFinalResult: handleFinalTranscript,
    onError: handleSpeechError,
    onStatusChange: handleSpeechStatusChange
  });
  window.speechService = speechService;

  // DOM Elements
  const tabButtons = document.querySelectorAll('.nav-tab');
  const tabContents = document.querySelectorAll('.tab-content');
  const toggleRecordBtn = document.getElementById('toggleRecordBtn');
  const micStatusLabel = document.getElementById('micStatusLabel');
  const micGlowRing = document.getElementById('micGlowRing');
  const micIcon = document.getElementById('micIcon');
  const languageSelect = document.getElementById('languageSelect');
  const finalizedSpan = document.getElementById('finalizedTextSpan');
  const interimSpan = document.getElementById('interimTextSpan');
  const placeholder = document.getElementById('transcriptPlaceholder');
  const streamLangBadge = document.getElementById('streamLanguageBadge');
  const streamRatioText = document.getElementById('streamRatioText');
  const streamWordCount = document.getElementById('streamWordCount');
  const streamWpm = document.getElementById('streamWpm');
  const streamTimer = document.getElementById('streamTimer');

  // Initialize
  initWebSocket();
  initNavigation();
  initTheme();
  initMicrophoneControls();
  initActionButtons();
  initDropzone();
  dashboard.loadTranscriptions();
  dashboard.loadStats();

  /**
   * Real-Time WebSocket Streaming Channel
   */
  function initWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/transcribe`;

    try {
      socket = new WebSocket(wsUrl);

      socket.onopen = () => {
        wsConnected = true;
        updateWsIndicator(true, 'WebSocket Live');
        // Heartbeat ping every 25s
        setInterval(() => {
          if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: 'PING' }));
          }
        }, 25000);
      };

      socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.type === 'STREAM_METRICS') {
            if (streamLangBadge) streamLangBadge.textContent = msg.primaryLanguage;
            if (streamRatioText) streamRatioText.textContent = msg.languageRatio;
            if (streamWordCount) streamWordCount.textContent = msg.wordCount;
          }
        } catch (e) {}
      };

      socket.onerror = (err) => {
        console.warn('WebSocket error:', err);
        updateWsIndicator(false, 'WebSocket Reconnecting...');
      };

      socket.onclose = () => {
        wsConnected = false;
        updateWsIndicator(false, 'WebSocket Disconnected');
        // Attempt reconnection after 3 seconds
        setTimeout(initWebSocket, 3000);
      };
    } catch (e) {
      console.warn('Could not initialize WebSocket:', e);
      updateWsIndicator(false, 'WebSocket Offline');
    }
  }

  function updateWsIndicator(connected, text) {
    const badge = document.getElementById('wsStatusBadge');
    const dot = document.getElementById('wsStatusDot');
    const label = document.getElementById('wsStatusText');
    if (!badge || !dot || !label) return;

    label.textContent = text;
    if (connected) {
      dot.style.background = 'var(--success)';
      dot.style.boxShadow = '0 0 8px var(--success)';
      badge.style.borderColor = 'rgba(16, 185, 129, 0.25)';
    } else {
      dot.style.background = 'var(--danger)';
      dot.style.boxShadow = '0 0 8px var(--danger)';
      badge.style.borderColor = 'rgba(239, 68, 68, 0.25)';
    }
  }

  /**
   * Navigation Tabs
   */
  function initNavigation() {
    tabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetId = btn.getAttribute('data-target');

        tabButtons.forEach(b => b.classList.remove('active'));
        tabContents.forEach(c => c.classList.remove('active'));

        btn.classList.add('active');
        const targetSection = document.getElementById(targetId);
        if (targetSection) targetSection.classList.add('active');

        if (targetId === 'tabDashboard') {
          dashboard.loadTranscriptions();
          dashboard.loadStats();
        }
      });
    });

    // Close modals on overlay or [data-close] click
    document.querySelectorAll('.modal-overlay').forEach(modal => {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.remove('active');
      });
    });

    document.querySelectorAll('[data-close]').forEach(btn => {
      btn.addEventListener('click', () => {
        const modalId = btn.getAttribute('data-close');
        const modal = document.getElementById(modalId);
        if (modal) modal.classList.remove('active');
      });
    });
  }

  /**
   * Dark / Light Theme Toggle
   */
  function initTheme() {
    const themeBtn = document.getElementById('themeToggleBtn');
    const savedTheme = localStorage.getItem('procucev-theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);

    if (themeBtn) {
      themeBtn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('procucev-theme', next);
        showToast(`Switched to ${next} mode`, 'info');
      });
    }
  }

  /**
   * Microphone & Recording Controls
   */
  function initMicrophoneControls() {
    if (toggleRecordBtn) {
      toggleRecordBtn.addEventListener('click', toggleRecording);
    }

    // Spacebar shortcut to toggle recording
    document.addEventListener('keydown', (e) => {
      if (e.code === 'Space' && !['INPUT', 'TEXTAREA'].includes(e.target.tagName)) {
        e.preventDefault();
        toggleRecording();
      }
    });

    // Language selection change
    if (languageSelect) {
      languageSelect.addEventListener('change', (e) => {
        speechService.updateLanguage(e.target.value);
        showToast(`Recognition language: ${e.target.selectedOptions[0].text}`, 'info');
      });
    }
  }

  async function toggleRecording() {
    if (speechService.isListening) {
      // Stop recording
      const hadNetworkIssue = speechService.cloudSpeechFailed || speechService.lastError === 'network';
      speechService.stopListening();
      visualizer.disconnect();
      stopTimer();
      toggleRecordBtn.classList.remove('recording');
      micGlowRing.style.opacity = '0';
      micIcon.textContent = '🎙️';
      micStatusLabel.textContent = 'Microphone Paused';

      // Check if browser recognized any words
      const currentText = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
      const recordedBlob = speechService.getRecordedAudioBlob();

      // If browser speech was empty or had network issues, auto-transcribe the recorded audio!
      if ((!currentText || hadNetworkIssue) && recordedBlob && recordedBlob.size > 1500) {
        micStatusLabel.textContent = 'Transcribing recorded audio...';
        showToast('Processing recorded audio with AI speech service...', 'info');
        await transcribeRecordedBlob(recordedBlob);
      }
    } else {
      // Start recording
      try {
        const stream = await speechService.startListening();
        if (stream) {
          visualizer.connectStream(stream);
        }
        startTimer();
        toggleRecordBtn.classList.add('recording');
        micGlowRing.style.opacity = '1';
        micIcon.textContent = '⏹️';
        micStatusLabel.textContent = 'Listening in real-time...';
        if (placeholder) placeholder.style.display = 'none';
      } catch (err) {
        showToast('Microphone access failed. Please allow mic permissions.', 'error');
      }
    }
  }

  async function transcribeRecordedBlob(blob) {
    if (!blob || blob.size < 1000) return;

    const formData = new FormData();
    formData.append('file', blob, 'microphone-recording.webm');
    formData.append('language', languageSelect ? languageSelect.value : 'hinglish');

    const headers = {};
    const storedApiKey = localStorage.getItem('procucev_gemini_api_key');
    if (storedApiKey && storedApiKey.trim()) {
      headers['X-Gemini-API-Key'] = storedApiKey.trim();
    }

    try {
      const res = await fetch('/api/speech/transcribe-audio', {
        method: 'POST',
        headers: headers,
        body: formData
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();

      if (data.success && data.transcript) {
        fullFinalTranscript = data.transcript;
        liveInterimTranscript = '';
        if (finalizedSpan) finalizedSpan.textContent = data.transcript;
        if (interimSpan) interimSpan.textContent = '';
        if (placeholder) placeholder.style.display = 'none';

        updateLiveMetrics();
        showToast('Audio transcribed successfully! 🎙️', 'success');
        micStatusLabel.textContent = 'Transcription ready';
      }
    } catch (err) {
      console.warn('Audio transcription error:', err);
      showToast('Could not transcribe audio: ' + err.message, 'error');
      micStatusLabel.textContent = 'Ready to listen';
    }
  }

  function handleInterimTranscript(interim) {
    liveInterimTranscript = interim;
    if (interimSpan) {
      interimSpan.textContent = ' ' + interim;
    }
    if (placeholder) placeholder.style.display = 'none';
    updateLiveMetrics();
  }

  function handleFinalTranscript(final) {
    fullFinalTranscript += (fullFinalTranscript ? ' ' : '') + final.trim();
    liveInterimTranscript = '';
    if (finalizedSpan) {
      finalizedSpan.textContent = fullFinalTranscript;
    }
    if (interimSpan) {
      interimSpan.textContent = '';
    }
    if (placeholder) placeholder.style.display = 'none';
    updateLiveMetrics();
  }

  function handleSpeechError(error) {
    if (error === 'no-speech') return;
    if (error === 'network') {
      showToast('Browser speech cloud has a network restriction. Audio is being recorded for auto-transcription on stop.', 'info');
      return;
    }
    showToast(`Speech error: ${error}`, 'error');
  }

  function handleSpeechStatusChange(status) {
    if (micStatusLabel) {
      micStatusLabel.textContent = status.message;
    }
  }

  function updateLiveMetrics() {
    const combined = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
    if (!combined) return;

    // Word count
    const words = combined.split(/\s+/).filter(Boolean).length;
    if (streamWordCount) streamWordCount.textContent = words;

    // Speaking rate WPM
    if (recordingStartTime) {
      const minutes = (Date.now() - recordingStartTime) / 60000;
      if (minutes > 0.05) {
        const wpm = Math.round(words / minutes);
        if (streamWpm) streamWpm.textContent = `${wpm} WPM`;
      }
    }

    // Send to WebSocket for real-time analysis
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({
        type: 'ANALYZE_STREAM',
        text: combined
      }));
    }
  }

  function startTimer() {
    recordingStartTime = Date.now();
    timerInterval = setInterval(() => {
      const elapsedSec = Math.floor((Date.now() - recordingStartTime) / 1000);
      const m = String(Math.floor(elapsedSec / 60)).padStart(2, '0');
      const s = String(elapsedSec % 60).padStart(2, '0');
      if (streamTimer) streamTimer.textContent = `${m}:${s}`;
    }, 1000);
  }

  function stopTimer() {
    clearInterval(timerInterval);
  }

  /**
   * Action Toolbar Buttons
   */
  function initActionButtons() {
    // Copy
    const copyBtn = document.getElementById('copyTranscriptBtn');
    if (copyBtn) {
      copyBtn.addEventListener('click', () => {
        const text = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
        if (!text) {
          showToast('Nothing to copy yet!', 'error');
          return;
        }
        navigator.clipboard.writeText(text);
        showToast('Transcript copied to clipboard!', 'success');
      });
    }

    // Clear
    const clearBtn = document.getElementById('clearTranscriptBtn');
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        fullFinalTranscript = '';
        liveInterimTranscript = '';
        if (finalizedSpan) finalizedSpan.textContent = '';
        if (interimSpan) interimSpan.textContent = '';
        if (placeholder) placeholder.style.display = 'flex';
        if (streamWordCount) streamWordCount.textContent = '0';
        if (streamWpm) streamWpm.textContent = '0 WPM';
        if (streamTimer) streamTimer.textContent = '00:00';
        showToast('Transcript cleared', 'info');
      });
    }

    // Read Aloud (TTS)
    const ttsBtn = document.getElementById('ttsPlayBtn');
    if (ttsBtn) {
      ttsBtn.addEventListener('click', () => {
        const text = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
        if (!text) {
          showToast('Please speak or generate some text first', 'error');
          return;
        }
        speechService.speakText(text);
        showToast('Speaking transcript aloud...', 'info');
      });
    }

    // Save to Database Modal
    const saveBtn = document.getElementById('saveTranscriptBtn');
    const saveModal = document.getElementById('saveModalOverlay');
    const confirmSaveBtn = document.getElementById('confirmSaveBtn');

    if (saveBtn && saveModal) {
      saveBtn.addEventListener('click', () => {
        const text = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
        if (!text) {
          showToast('Cannot save an empty transcript', 'error');
          return;
        }

        const titleInput = document.getElementById('saveModalTitle');
        const contentArea = document.getElementById('saveModalContent');

        if (titleInput) {
          const preview = text.length > 35 ? text.substring(0, 35) + '...' : text;
          titleInput.value = `Multilingual Note - ${preview}`;
        }
        if (contentArea) contentArea.value = text;

        saveModal.classList.add('active');
      });
    }

    if (confirmSaveBtn) {
      confirmSaveBtn.addEventListener('click', async () => {
        const title = document.getElementById('saveModalTitle').value.trim();
        const content = document.getElementById('saveModalContent').value.trim();
        const tags = document.getElementById('saveModalTags').value.trim();
        const notes = document.getElementById('saveModalNotes').value.trim();

        if (!content) {
          showToast('Content is required', 'error');
          return;
        }

        try {
          const res = await fetch('/api/transcriptions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              title,
              content,
              tags: tags || 'realtime,hinglish',
              notes,
              provider: 'browser-live-speech'
            })
          });

          if (!res.ok) throw new Error('Save failed');

          saveModal.classList.remove('active');
          showToast('Saved to Relational Database successfully! 🎉', 'success');
          dashboard.loadTranscriptions();
          dashboard.loadStats();

        } catch (err) {
          showToast('Failed to save to database: ' + err.message, 'error');
        }
      });
    }

    // AI Hinglish Refine Modal
    const polishBtn = document.getElementById('aiPolishBtn');
    const polishModal = document.getElementById('polishModalOverlay');
    const runPolishBtn = document.getElementById('runPolishBtn');
    const applyPolishBtn = document.getElementById('applyPolishBtn');

    if (polishBtn && polishModal) {
      polishBtn.addEventListener('click', () => {
        const text = (fullFinalTranscript + ' ' + liveInterimTranscript).trim();
        if (!text) {
          showToast('Please record or enter text to refine', 'error');
          return;
        }

        document.getElementById('polishOriginalPreview').textContent = text;
        document.getElementById('polishResultContent').value = '';
        polishModal.classList.add('active');
      });
    }

    if (runPolishBtn) {
      runPolishBtn.addEventListener('click', async () => {
        const text = document.getElementById('polishOriginalPreview').textContent;
        const mode = document.getElementById('polishModeSelect').value;
        const resultArea = document.getElementById('polishResultContent');

        runPolishBtn.textContent = '⏳ Processing...';
        runPolishBtn.disabled = true;

        const polishHeaders = { 'Content-Type': 'application/json' };
        const storedApiKey = localStorage.getItem('procucev_gemini_api_key');
        if (storedApiKey && storedApiKey.trim()) {
          polishHeaders['X-Gemini-API-Key'] = storedApiKey.trim();
        }

        try {
          const res = await fetch('/api/speech/polish', {
            method: 'POST',
            headers: polishHeaders,
            body: JSON.stringify({ text, mode })
          });

          if (!res.ok) throw new Error('Refinement failed');
          const data = await res.json();
          resultArea.value = data.polishedText || text;
          showToast('AI refinement complete!', 'success');
        } catch (err) {
          showToast('Refinement service error: ' + err.message, 'error');
        } finally {
          runPolishBtn.textContent = '⚡ Process with AI';
          runPolishBtn.disabled = false;
        }
      });
    }

    if (applyPolishBtn) {
      applyPolishBtn.addEventListener('click', () => {
        const refined = document.getElementById('polishResultContent').value.trim();
        if (!refined) {
          showToast('No refined text to apply', 'error');
          return;
        }
        fullFinalTranscript = refined;
        liveInterimTranscript = '';
        if (finalizedSpan) finalizedSpan.textContent = refined;
        if (interimSpan) interimSpan.textContent = '';
        polishModal.classList.remove('active');
        showToast('Applied refined Hinglish text!', 'success');
        updateLiveMetrics();
      });
    }

    // Direct Edit Sync for Live Transcript Area
    const liveArea = document.getElementById('liveTranscriptArea');
    if (liveArea) {
      liveArea.addEventListener('input', () => {
        fullFinalTranscript = liveArea.innerText.trim();
        liveInterimTranscript = '';
        if (placeholder) {
          placeholder.style.display = fullFinalTranscript ? 'none' : 'flex';
        }
        updateLiveMetrics();
      });
    }

    // Gemini API Key Modal
    const apiKeyBtn = document.getElementById('apiKeyModalBtn');
    const apiKeyModal = document.getElementById('apiKeyModalOverlay');
    const saveApiKeyBtn = document.getElementById('saveApiKeyBtn');
    const apiKeyInput = document.getElementById('geminiApiKeyInput');

    if (apiKeyBtn && apiKeyModal) {
      apiKeyBtn.addEventListener('click', () => {
        if (apiKeyInput) {
          apiKeyInput.value = localStorage.getItem('procucev_gemini_api_key') || '';
        }
        apiKeyModal.classList.add('active');
      });
    }

    if (saveApiKeyBtn && apiKeyInput) {
      saveApiKeyBtn.addEventListener('click', () => {
        const val = apiKeyInput.value.trim();
        if (val) {
          localStorage.setItem('procucev_gemini_api_key', val);
          showToast('Gemini API key saved! Multimodal audio AI active 🔑', 'success');
        } else {
          localStorage.removeItem('procucev_gemini_api_key');
          showToast('Gemini API key removed. Using default mode.', 'info');
        }
        if (apiKeyModal) apiKeyModal.classList.remove('active');
      });
    }

    // Sample Prompt Loader
    const sampleBtn = document.getElementById('samplePromptBtn');
    if (sampleBtn) {
      const sampleSentences = [
        "Aaj hum log sprint planning complete karenge because API deployment deadline kal sham tak hai.",
        "Server restart ho gaya tha lekin abhi Docker container perfectly run kar raha hai without latency.",
        "Yeh real-time transcription service Hindi aur English code-mixing ko accurately recognize karti hai.",
        "Please check the database schema and ensure that all CRUD operations are properly tested in the dashboard.",
        "आज की मीटिंग में हमने डिसाइड किया है कि real-time audio visualizer को WebSocket के साथ sync करेंगे।"
      ];
      let sampleIndex = 0;

      sampleBtn.addEventListener('click', () => {
        const sentence = sampleSentences[sampleIndex % sampleSentences.length];
        sampleIndex++;

        fullFinalTranscript = sentence;
        liveInterimTranscript = '';
        if (finalizedSpan) finalizedSpan.textContent = sentence;
        if (interimSpan) interimSpan.textContent = '';
        if (placeholder) placeholder.style.display = 'none';

        updateLiveMetrics();
        showToast('Loaded authentic Hinglish sample!', 'info');
      });
    }
  }

  /**
   * Audio File Dropzone
   */
  function initDropzone() {
    const dropzone = document.getElementById('audioDropzone');
    const fileInput = document.getElementById('audioFileInput');
    const uploadStatus = document.getElementById('fileUploadStatus');

    if (!dropzone || !fileInput) return;

    dropzone.addEventListener('click', () => fileInput.click());

    dropzone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropzone.classList.add('drag-over');
    });

    dropzone.addEventListener('dragleave', () => {
      dropzone.classList.remove('drag-over');
    });

    dropzone.addEventListener('drop', (e) => {
      e.preventDefault();
      dropzone.classList.remove('drag-over');
      if (e.dataTransfer.files.length > 0) {
        handleAudioFileUpload(e.dataTransfer.files[0]);
      }
    });

    fileInput.addEventListener('change', (e) => {
      if (e.target.files.length > 0) {
        handleAudioFileUpload(e.target.files[0]);
      }
    });

    async function handleAudioFileUpload(file) {
      if (!file.type.startsWith('audio/') && !file.name.match(/\.(wav|mp3|m4a|webm|ogg)$/i)) {
        showToast('Please select a valid audio file (.wav, .mp3, .webm, .ogg)', 'error');
        return;
      }

      uploadStatus.style.display = 'block';
      uploadStatus.textContent = `⏳ Uploading and transcribing "${file.name}"...`;

      const formData = new FormData();
      formData.append('file', file);
      formData.append('language', languageSelect ? languageSelect.value : 'hinglish');

      const headers = {};
      const storedApiKey = localStorage.getItem('procucev_gemini_api_key');
      if (storedApiKey && storedApiKey.trim()) {
        headers['X-Gemini-API-Key'] = storedApiKey.trim();
      }

      try {
        const res = await fetch('/api/speech/transcribe-audio', {
          method: 'POST',
          headers: headers,
          body: formData
        });

        if (!res.ok) throw new Error('Audio transcription failed');
        const data = await res.json();

        if (data.success && data.transcript) {
          fullFinalTranscript = data.transcript;
          liveInterimTranscript = '';
          if (finalizedSpan) finalizedSpan.textContent = data.transcript;
          if (placeholder) placeholder.style.display = 'none';

          updateLiveMetrics();
          uploadStatus.textContent = `✅ Transcribed "${file.name}" successfully!`;
          showToast('Audio transcribed successfully!', 'success');
        } else {
          throw new Error(data.error || 'Transcription failed');
        }
      } catch (err) {
        uploadStatus.textContent = `❌ Error: ${err.message}`;
        showToast(`Upload failed: ${err.message}`, 'error');
      }
    }
  }

  /**
   * Floating Toast Notifications with deduplication
   */
  let lastToastText = '';
  let lastToastTimestamp = 0;

  function showToast(message, type = 'info') {
    const now = Date.now();
    if (message === lastToastText && (now - lastToastTimestamp) < 3000) {
      return; // Deduplicate repeated toasts
    }
    lastToastText = message;
    lastToastTimestamp = now;

    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }
});
