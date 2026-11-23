// pages/mine/mine.js - 我的页面逻辑
const app = getApp();

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    tempAvatarUrl: '',
    inputNickname: '',
    currentTab: 'publish', // 'publish' | 'fav' | 'group'
    displayList: [],
    myPublishList: [
      {
        _id: 'mock_1',
        title: '考研高等数学辅导讲义 (张宇/李永乐)',
        price: 18,
        type: 'secondhand',
        images: ['https://picsum.photos/300/300?random=1']
      }
    ],
    myFavList: [
      {
        _id: 'mock_3',
        title: 'iPad Air 5 64G 紫色 99新',
        price: 3200,
        type: 'secondhand',
        images: ['https://picsum.photos/300/300?random=3']
      }
    ],
    myGroupList: [
      {
        _id: 'mock_2',
        title: '麦当劳超值双人餐 差1人拼团分摊',
        price: 15.5,
        type: 'group',
        images: ['https://picsum.photos/300/300?random=2']
      }
    ]
  },

  onLoad() {
    this.checkLoginState();
    this.updateDisplayList();
  },

  checkLoginState() {
    if (app.globalData.isLoggedIn) {
      this.setData({
        isLoggedIn: true,
        userInfo: app.globalData.userInfo
      });
    }
  },

  // 选择头像
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    this.setData({ tempAvatarUrl: avatarUrl });
  },

  // 输入昵称
  onInputNickname(e) {
    this.setData({ inputNickname: e.detail.value });
  },

  // 确认登录
  confirmLogin() {
    const avatarUrl = this.data.tempAvatarUrl || 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';
    const nickName = this.data.inputNickname.trim() || '校友小强';

    const userInfo = { avatarUrl, nickName };
    app.globalData.userInfo = userInfo;
    app.globalData.isLoggedIn = true;
    wx.setStorageSync('userInfo', userInfo);

    this.setData({
      isLoggedIn: true,
      userInfo
    });

    wx.showToast({ title: '登录成功！', icon: 'success' });
  },

  // 切换列表页签
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab }, () => {
      this.updateDisplayList();
    });
  },

  updateDisplayList() {
    const { currentTab, myPublishList, myFavList, myGroupList } = this.data;
    let displayList = [];
    if (currentTab === 'publish') displayList = myPublishList;
    else if (currentTab === 'fav') displayList = myFavList;
    else if (currentTab === 'group') displayList = myGroupList;

    this.setData({ displayList });
  },

  // 跳转详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 重新查看新手引导
  replayGuide() {
    wx.showModal({
      title: '重新查看新手引导',
      content: '确定要重置新手引导并重新开启全屏与步骤高亮引导吗？',
      confirmColor: '#FF8A3D',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('has_guided');
          wx.removeStorageSync('has_step_guided');
          wx.setStorageSync('force_onboarding', true);

          wx.switchTab({
            url: '/pages/home/home',
            success: () => {
              wx.showToast({ title: '引导已重置', icon: 'success' });
            }
          });
        }
      }
    });
  },

  showAbout() {
    wx.showModal({
      title: '关于校园集市',
      content: '「校园集市」版本 v1.0.0\n专为高校学生打造的二手闲置物品交易与拼饭/拼车/拼快递平台。支持微信云开发与原生CSS3动效。',
      showCancel: false,
      confirmColor: '#FF8A3D'
    });
  }
});
