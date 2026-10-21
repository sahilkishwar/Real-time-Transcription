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
      this.onStatusChange({ listening: true, message: 'Listening in real-time...' });
    };

    this.recognition.onresult = (event) => {
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
      this.onError(event.error);
    };

    this.recognition.onend = () => {
      // Auto-restart if user still has recording turned on (continuous mode)
      if (this.isListening) {
        try {
          this.recognition.start();
        } catch (e) {
          this.isListening = false;
          this.onStatusChange({ listening: false, message: 'Microphone stopped' });
        }
      } else {
        this.onStatusChange({ listening: false, message: 'Ready to listen' });
      }
    };

    return true;
  }

  updateLanguage(lang) {
    this.selectedLanguage = lang;
    if (!this.recognition) return;

    if (lang === 'hinglish') {
      // In Indian context, en-IN / hi-IN captures mixed Hinglish words seamlessly
      this.recognition.lang = 'hi-IN';
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

    // Also start MediaRecorder for raw audio chunk capture (for server upload & verification)
    try {
      this.audioChunks = [];
      const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : 'audio/webm';
      this.mediaRecorder = new MediaRecorder(this.mediaStream, { mimeType: mime });
      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          this.audioChunks.push(e.data);
        }
      };
      this.mediaRecorder.start(1000); // chunk every 1s
    } catch (err) {
      console.warn('MediaRecorder error:', err);
    }

    return this.mediaStream;
  }

  stopListening() {
    this.isListening = false;

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
    if (this.audioChunks.length === 0) return null;
    return new Blob(this.audioChunks, { type: 'audio/webm' });
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
