// components/step-guide/step-guide.js
Component({
  properties: {
    visible: {
      type: Boolean,
      value: false,
      observer(newVal) {
        if (newVal) {
          this.initStepRect();
        }
      }
    }
  },

  data: {
    currentStep: 0,
    steps: [
      {
        title: "🔍 顶部搜索栏",
        desc: "在这里可以输入关键字，快速搜搜二手好物或合意拼单哦！"
      },
      {
        title: "➕ 悬浮发布按钮",
        desc: "点击右下角按钮，随时发布闲置物品或发起拼餐拼车。"
      },
      {
        title: "👤 个人中心",
        desc: "在'我的'页面可查看我的发布、已参与拼单及收藏夹。"
      }
    ],
    currentStepRect: { top: 100, left: 20, width: 335, height: 44, borderRadius: '44rpx' },
    tooltipTop: 160,
    tooltipLeft: 20
  },

  methods: {
    initStepRect() {
      this.updateStepRect(0);
    },

    updateStepRect(stepIndex) {
      const windowInfo = wx.getSystemInfoSync();
      const screenWidth = windowInfo.screenWidth;
      const screenHeight = windowInfo.screenHeight;

      let rect = {};
      let tTop = 0;
      let tLeft = (screenWidth - 290) / 2; // 居中 580rpx (290px)

      if (stepIndex === 0) {
        // 搜索栏: 顶部
        rect = {
          top: windowInfo.statusBarHeight + 10,
          left: 16,
          width: screenWidth - 100,
          height: 44,
          borderRadius: '22px'
        };
        tTop = rect.top + 56;
      } else if (stepIndex === 1) {
        // 发布按钮: 右下角悬浮按钮
        rect = {
          top: screenHeight - 160,
          left: screenWidth - 76,
          width: 60,
          height: 60,
          borderRadius: '30px'
        };
        tTop = rect.top - 170;
      } else if (stepIndex === 2) {
        // 我的 TabBar: 底部最右边
        rect = {
          top: screenHeight - 60,
          left: screenWidth * 0.75 - 10,
          width: screenWidth * 0.25,
          height: 50,
          borderRadius: '12px'
        };
        tTop = rect.top - 180;
      }

      this.setData({
        currentStep: stepIndex,
        currentStepRect: rect,
        tooltipTop: Math.max(20, tTop),
        tooltipLeft: Math.max(10, tLeft)
      });
    },

    onNextStep() {
      if (this.data.currentStep < this.data.steps.length - 1) {
        const next = this.data.currentStep + 1;
        this.updateStepRect(next);
      } else {
        this.onFinish();
      }
    },

    onFinish() {
      wx.setStorageSync('has_step_guided', true);
      this.triggerEvent('finish');
    }
  }
});
