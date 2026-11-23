// components/onboarding/onboarding.js
Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    currentIndex: 0
  },

  methods: {
    onSwiperChange(e) {
      this.setData({
        currentIndex: e.detail.current
      });
    },

    onFinish() {
      wx.setStorageSync('has_guided', true);
      this.triggerEvent('finish');
    }
  }
});
