// components/confetti/confetti.js
Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    particles: []
  },

  attached() {
    this.generateParticles();
  },

  methods: {
    generateParticles() {
      const colors = ['#FF8A3D', '#FFD166', '#06D6A0', '#118AB2', '#EF476F', '#9C27B0'];
      const particles = [];
      for (let i = 0; i < 35; i++) {
        particles.push({
          left: Math.floor(Math.random() * 96),
          color: colors[Math.floor(Math.random() * colors.length)],
          size: Math.floor(Math.random() * 12) + 10,
          delay: (Math.random() * 2).toFixed(2),
          duration: (Math.random() * 2 + 2.5).toFixed(2)
        });
      }
      this.setData({ particles });
    },

    onClose() {
      this.triggerEvent('close');
    }
  }
});
