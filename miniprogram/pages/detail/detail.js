// pages/detail/detail.js - 详情页逻辑
const app = getApp();

Page({
  data: {
    id: '',
    goods: null,
    showConfetti: false,
    hasJoined: false,
    progressPercent: 0,
    countdownText: '12:00:00',
    publishTimeStr: '刚刚'
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.fetchGoodsDetail(options.id);
    }
  },

  fetchGoodsDetail(id) {
    if (wx.cloud && !id.startsWith('mock_')) {
      const db = wx.cloud.database();
      db.collection('goods').doc(id).get()
        .then(res => {
          this.setupGoodsData(res.data);
        })
        .catch(err => {
          console.log('获取云端详情失败，载入Mock数据:', err);
          this.loadMockDetail(id);
        });
    } else {
      this.loadMockDetail(id);
    }
  },

  loadMockDetail(id) {
    // 根据 ID 匹配
    const mockData = {
      _id: id,
      title: id === 'mock_2' ? '麦当劳超值双人餐 差1人拼团分摊' : '考研高等数学辅导讲义 (张宇/李永乐)',
      price: id === 'mock_2' ? 15.5 : 18,
      category: id === 'mock_2' ? 'group' : 'books',
      type: id === 'mock_2' ? 'group' : 'secondhand',
      currentCount: 1,
      maxCount: 2,
      images: [
        'https://picsum.photos/600/600?random=1',
        'https://picsum.photos/600/600?random=2'
      ],
      location: '二食堂门口 / 宿舍5舍',
      description: '全套配套讲义无划痕，全新转让！拼单的同学可以直接加微信，楼下当面交付，分摊运费和套餐优惠。',
      userNick: '学霸小张',
      userAvatar: 'https://api.iconify.design/twemoji:boy.svg',
      isFav: false,
      members: [
        { nickName: '学霸小张', avatarUrl: 'https://api.iconify.design/twemoji:boy.svg', isCreator: true }
      ],
      createTime: Date.now() - 3600000
    };
    this.setupGoodsData(mockData);
  },

  setupGoodsData(goods) {
    this.setData({ goods });

    // 计算进度条动画与倒计时
    if (goods.type === 'group') {
      const percent = Math.min(100, Math.floor((goods.currentCount / goods.maxCount) * 100));
      setTimeout(() => {
        this.setData({ progressPercent: percent });
      }, 200);

      this.startCountdown();
    }
  },

  startCountdown() {
    let seconds = 12 * 3600 + 45 * 60; // 模拟12小时
    this.timer = setInterval(() => {
      seconds--;
      if (seconds <= 0) {
        clearInterval(this.timer);
        this.setData({ countdownText: '拼单已截止' });
        return;
      }
      const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
      const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
      const s = String(seconds % 60).padStart(2, '0');
      this.setData({ countdownText: `${h}:${m}:${s}` });
    }, 1000);
  },

  onUnload() {
    if (this.timer) clearInterval(this.timer);
  },

  // 图片全屏预览
  previewImage(e) {
    const current = e.currentTarget.dataset.src;
    wx.previewImage({
      current,
      urls: this.data.goods.images
    });
  },

  // 收藏切换
  toggleFavorite() {
    const isFav = !this.data.goods.isFav;
    this.setData({
      'goods.isFav': isFav
    });
    wx.showToast({
      title: isFav ? '已加入收藏' : '已取消收藏',
      icon: 'none'
    });
  },

  // 加入拼单 (带平滑增长 + 人数增加 + 撒花/彩带动画)
  joinGroup() {
    if (this.data.hasJoined) {
      return wx.showToast({ title: '您已在拼单队伍中', icon: 'none' });
    }
    if (this.data.goods.currentCount >= this.data.goods.maxCount) {
      return wx.showToast({ title: '拼单已满员，无法加入', icon: 'none' });
    }

    const userInfo = app.globalData.userInfo || {
      nickName: '新拼友',
      avatarUrl: 'https://api.iconify.design/twemoji:party-popper.svg'
    };

    const newCount = this.data.goods.currentCount + 1;
    const newMembers = [...(this.data.goods.members || []), { nickName: userInfo.nickName, avatarUrl: userInfo.avatarUrl, isCreator: false }];
    const newPercent = Math.min(100, Math.floor((newCount / this.data.goods.maxCount) * 100));

    this.setData({
      'goods.currentCount': newCount,
      'goods.members': newMembers,
      progressPercent: newPercent,
      hasJoined: true
    });

    // 判断是否满员 -> 触发撒花彩带动画
    if (newCount >= this.data.goods.maxCount) {
      setTimeout(() => {
        this.setData({ showConfetti: true });
      }, 300);
    } else {
      wx.showToast({
        title: '成功加入拼单！',
        icon: 'success'
      });
    }
  },

  onCloseConfetti() {
    this.setData({ showConfetti: false });
  },

  // 私信学友 / 联系卖家
  contactSeller() {
    const { goods } = this.data;
    wx.navigateTo({
      url: `/pages/message/message?sellerNick=${encodeURIComponent(goods.userNick)}&goodsTitle=${encodeURIComponent(goods.title)}`
    });
  }
});
