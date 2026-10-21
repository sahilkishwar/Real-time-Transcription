/**
 * SpeechService - Real-time multilingual Speech-to-Text and Text-to-Speech manager
 */
class SpeechService {
  constructor(options = {}) {
    this.onInterimResult = options.onInterimResult || (() => {});
    this.onFinalResult = options.onFinalResult || (() => {});
    this.onError = options.onError || (() => {});
    this.onStatusChange = options.onStatusChange || (() => {});

    this.recognition = null;
    this.isListening = false;
    this.selectedLanguage = 'hinglish';
    this.mediaRecorder = null;
    this.audioChunks = [];
    this.mediaStream = null;

    this.networkErrorCount = 0;
    this.cloudSpeechFailed = false;
    this.lastError = null;
    this.restartTimer = null;

    this.initRecognition();
  }

  initRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      console.warn('Web Speech API is not natively supported in this browser.');
      return false;
    }

    this.recognition = new SpeechRecognition();
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    this.recognition.maxAlternatives = 1;

    // Set recognition language
    this.updateLanguage(this.selectedLanguage);

    this.recognition.onstart = () => {
      this.isListening = true;
      this.cloudSpeechFailed = false;
      this.onStatusChange({ listening: true, message: 'Listening in real-time...' });
    };

    this.recognition.onresult = (event) => {
      this.networkErrorCount = 0;
      this.cloudSpeechFailed = false;
      let interim = '';
      let final = '';

      for (let i = event.resultIndex; i < event.results.length; ++i) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          final += transcript;
        } else {
          interim += transcript;
        }
      }

      if (interim) {
        this.onInterimResult(interim);
      }
      if (final) {
        this.onFinalResult(final);
      }
    };

    this.recognition.onerror = (event) => {
      console.warn('Speech recognition event error:', event.error);
      this.lastError = event.error;

      // Only notify parent onError if not looping
      if (event.error !== 'network' || this.networkErrorCount === 0) {
        this.onError(event.error);
      }
    };

    this.recognition.onend = () => {
      if (this.restartTimer) {
        clearTimeout(this.restartTimer);
        this.restartTimer = null;
      }

      // Auto-restart if user still has recording turned on (continuous mode)
      if (this.isListening) {
        if (this.lastError === 'network') {
          this.networkErrorCount = (this.networkErrorCount || 0) + 1;

          if (this.networkErrorCount === 1) {
            // First fallback: Switch to Indian English (en-IN) which supports Hinglish phonetics and connects reliably
            console.log('Speech network error encountered. Switching to fallback engine (en-IN)...');
            this.recognition.lang = 'en-IN';
            this.restartTimer = setTimeout(() => {
              if (this.isListening) {
                try { this.recognition.start(); } catch (e) {}
              }
            }, 1200);
            return;
          } else if (this.networkErrorCount === 2) {
            // Second fallback: Global English
            console.log('Speech network error encountered. Switching to global model (en-US)...');
            this.recognition.lang = 'en-US';
            this.restartTimer = setTimeout(() => {
              if (this.isListening) {
                try { this.recognition.start(); } catch (e) {}
              }
            }, 1500);
            return;
          } else {
            // Browser cloud speech recognition is unreachable on this network/browser.
            // Gracefully stop recognition attempts to prevent infinite loop and CPU spin.
            // Microphone audio recording remains active and will be transcribed on stop!
            this.cloudSpeechFailed = true;
            console.warn('Browser SpeechRecognition network unavailable. Recording live audio stream for AI backend transcription.');
            this.onStatusChange({
              listening: true,
              message: '🎙️ Recording live audio stream (Browser speech cloud offline — auto-transcribing on stop)'
            });
            return;
          }
        }

        // Standard restart backoff (prevent rapid CPU spinning)
        this.restartTimer = setTimeout(() => {
          if (this.isListening) {
            try {
              this.recognition.start();
            } catch (e) {
              console.warn('Recognition restart caught:', e);
            }
          }
        }, 400);
      } else {
        this.onStatusChange({ listening: false, message: 'Ready to listen' });
      }
    };

    return true;
  }

  updateLanguage(lang) {
    this.selectedLanguage = lang;
    this.networkErrorCount = 0;
    this.cloudSpeechFailed = false;
    if (!this.recognition) return;

    if (lang === 'hinglish') {
      // en-IN supports Indian accents, code-mixed Hinglish vocabulary, and avoids Google Hindi speech server timeouts
      this.recognition.lang = 'en-IN';
    } else if (lang === 'hi-IN') {
      this.recognition.lang = 'hi-IN';
    } else if (lang === 'en-IN') {
      this.recognition.lang = 'en-IN';
    } else {
      this.recognition.lang = 'en-US';
    }
  }

  async startListening() {
    if (this.isListening) return;

    this.networkErrorCount = 0;
    this.cloudSpeechFailed = false;
    this.lastError = null;

    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
      this.onError('Microphone permission denied: ' + e.message);
      throw e;
    }

    if (this.recognition) {
      try {
        this.isListening = true;
        this.recognition.start();
      } catch (e) {
        console.warn('Recognition start caught:', e);
      }
    }

    // Start MediaRecorder for raw audio chunk capture (for server upload & verification)
    try {
      this.audioChunks = [];
      const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : 'audio/webm';
      this.mediaRecorder = new MediaRecorder(this.mediaStream, { mimeType: mime });
      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) {
          this.audioChunks.push(e.data);
        }
      };
      this.mediaRecorder.start(500); // chunk every 500ms
    } catch (err) {
      console.warn('MediaRecorder error:', err);
    }

    return this.mediaStream;
  }

  stopListening() {
    this.isListening = false;
    if (this.restartTimer) {
      clearTimeout(this.restartTimer);
      this.restartTimer = null;
    }

    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (e) {}
    }

    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      try {
        this.mediaRecorder.stop();
      } catch (e) {}
    }

    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }

    this.onStatusChange({ listening: false, message: 'Microphone stopped' });
  }

  getRecordedAudioBlob() {
    if (!this.audioChunks || this.audioChunks.length === 0) return null;
    const mime = (this.mediaRecorder && this.mediaRecorder.mimeType) ? this.mediaRecorder.mimeType : 'audio/webm';
    return new Blob(this.audioChunks, { type: mime });
  }

  /**
   * Text-to-Speech (TTS) synthesizer with voice preference
   */
  speakText(text, lang = 'hi-IN') {
    if (!('speechSynthesis' in window)) {
      alert('Text-to-Speech is not supported in this browser.');
      return;
    }

    window.speechSynthesis.cancel(); // Stop any ongoing speech

    if (!text || !text.trim()) return;

    const utterance = new SpeechSynthesisUtterance(text.trim());
    utterance.rate = 0.95;
    utterance.pitch = 1.0;

    // Pick best matching voice
    const voices = window.speechSynthesis.getVoices();
    const voice = voices.find(v => v.lang.includes('hi') || v.lang.includes('en-IN')) || voices[0];
    if (voice) {
      utterance.voice = voice;
    }

    window.speechSynthesis.speak(utterance);
  }
}

window.SpeechService = SpeechService;
