// app.js - 校园集市小程序入口与全局逻辑
App({
  onLaunch: function () {
    if (!wx.cloud) {
      console.error('请使用 2.2.3 或以上的基础库以使用云能力');
    } else {
      // 初始化微信云开发环境
      wx.cloud.init({
        // env: 'your-cloud-env-id', // 替换为你的微信云开发环境ID，为空则使用默认环境
        traceUser: true,
      });
    }

    // 全局数据定义
    this.globalData = {
      userInfo: null,
      isLoggedIn: false,
      theme: 'light' // 支持 'light' / 'dark'
    };

    // 检查本地是否有已保存的用户信息
    const cachedUserInfo = wx.getStorageSync('userInfo');
    if (cachedUserInfo) {
      this.globalData.userInfo = cachedUserInfo;
      this.globalData.isLoggedIn = true;
    }
  }
});
