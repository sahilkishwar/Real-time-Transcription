/**
 * AudioVisualizer - Web Audio API real-time canvas visualizer
 */
class AudioVisualizer {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    this.ctx = this.canvas ? this.canvas.getContext('2d') : null;
    this.audioContext = null;
    this.analyser = null;
    this.dataArray = null;
    this.source = null;
    this.animationId = null;
    this.isActive = false;

    this.resizeCanvas();
    window.addEventListener('resize', () => this.resizeCanvas());
    this.startIdleAnimation();
  }

  resizeCanvas() {
    if (!this.canvas) return;
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.canvas.width = rect.width;
    this.canvas.height = rect.height || 80;
  }

  connectStream(stream) {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!this.audioContext) {
        this.audioContext = new AudioCtx();
      }
      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume();
      }

      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 128;
      this.analyser.smoothingTimeConstant = 0.8;

      this.source = this.audioContext.createMediaStreamSource(stream);
      this.source.connect(this.analyser);

      const bufferLength = this.analyser.frequencyBinCount;
      this.dataArray = new Uint8Array(bufferLength);
      this.isActive = true;

      this.animateLive();
    } catch (e) {
      console.warn('AudioContext initialization failed:', e);
    }
  }

  disconnect() {
    this.isActive = false;
    if (this.source) {
      this.source.disconnect();
      this.source = null;
    }
    this.startIdleAnimation();
    this.updateAudioLevel(0);
  }

  animateLive() {
    if (!this.isActive || !this.analyser) return;

    this.analyser.getByteFrequencyData(this.dataArray);

    const ctx = this.ctx;
    const width = this.canvas.width;
    const height = this.canvas.height;

    ctx.clearRect(0, 0, width, height);

    // Compute volume RMS
    let sum = 0;
    const barCount = this.dataArray.length;
    const barWidth = (width / barCount) * 1.5;

    for (let i = 0; i < barCount; i++) {
      const value = this.dataArray[i];
      sum += value * value;
      const percent = value / 255;
      const barHeight = Math.max(4, percent * height);

      const x = i * (barWidth + 2);
      const y = height - barHeight;

      // Gradient color based on frequency
      const hue = 220 + percent * 100; // Indigo to Cyan/Pink
      ctx.fillStyle = `hsl(${hue}, 85%, 60%)`;
      ctx.beginPath();
      ctx.roundRect(x, y, barWidth, barHeight, 3);
      ctx.fill();
    }

    const rms = Math.sqrt(sum / barCount) / 255;
    this.updateAudioLevel(rms);

    this.animationId = requestAnimationFrame(() => this.animateLive());
  }

  updateAudioLevel(level) {
    const fill = document.getElementById('audioLevelFill');
    const dbText = document.getElementById('audioLevelDbText');
    const percent = Math.min(100, Math.round(level * 100));

    if (fill) fill.style.width = `${percent}%`;
    if (dbText) {
      const db = level > 0.01 ? Math.round(20 * Math.log10(level)) : -60;
      dbText.textContent = `${db} dB`;
    }
  }

  startIdleAnimation() {
    if (this.isActive) return;
    const ctx = this.ctx;
    if (!ctx) return;

    let offset = 0;
    const drawIdleWave = () => {
      if (this.isActive) return;

      const width = this.canvas.width;
      const height = this.canvas.height;
      ctx.clearRect(0, 0, width, height);

      ctx.beginPath();
      ctx.lineWidth = 2;
      ctx.strokeStyle = 'rgba(99, 102, 241, 0.25)';

      const mid = height / 2;
      for (let x = 0; x < width; x++) {
        const y = mid + Math.sin((x * 0.02) + offset) * 5;
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      offset += 0.04;
      this.animationId = requestAnimationFrame(drawIdleWave);
    };

    if (this.animationId) cancelAnimationFrame(this.animationId);
    this.animationId = requestAnimationFrame(drawIdleWave);
  }
}

window.AudioVisualizer = AudioVisualizer;
