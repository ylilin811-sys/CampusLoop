// pages/home/home.js - 首页逻辑
const app = getApp();

// 本地离线/样例测试数据（当云数据库未配置或为空时自动开启无缝体验）
const MOCK_GOODS = [
  {
    _id: 'mock_1',
    title: '考研高等数学辅导讲义 (张宇/李永乐)',
    price: 18,
    category: 'books',
    type: 'secondhand',
    images: ['https://picsum.photos/300/300?random=1'],
    location: '图书馆门口',
    userNick: '学霸小张',
    userAvatar: 'https://api.iconify.design/twemoji:boy.svg',
    isFav: false,
    createTime: Date.now()
  },
  {
    _id: 'mock_2',
    title: '麦当劳超值双人餐 差1人拼团分摊',
    price: 15.5,
    category: 'group',
    type: 'group',
    currentCount: 1,
    maxCount: 2,
    images: ['https://picsum.photos/300/300?random=2'],
    location: '二食堂门口',
    userNick: '干饭人阿杰',
    userAvatar: 'https://api.iconify.design/twemoji:man.svg',
    isFav: true,
    createTime: Date.now() - 3600000
  },
  {
    _id: 'mock_3',
    title: 'iPad Air 5 64G 紫色 99新 带Apple Pencil',
    price: 3200,
    category: 'digital',
    type: 'secondhand',
    images: ['https://picsum.photos/300/300?random=3'],
    location: '12舍下',
    userNick: '极客小明',
    userAvatar: 'https://api.iconify.design/twemoji:man-technologist.svg',
    isFav: false,
    createTime: Date.now() - 7200000
  },
  {
    _id: 'mock_4',
    title: '周末周口店/八达岭长城顺风车 拼车3人',
    price: 25,
    category: 'group',
    type: 'group',
    currentCount: 2,
    maxCount: 4,
    images: ['https://picsum.photos/300/300?random=4'],
    location: '学校北门',
    userNick: '老司机王哥',
    userAvatar: 'https://api.iconify.design/twemoji:oncoming-automobile.svg',
    isFav: false,
    createTime: Date.now() - 10800000
  },
  {
    _id: 'mock_5',
    title: '九成新九阳电水壶 1.8L 快速烧水',
    price: 35,
    category: 'daily',
    type: 'secondhand',
    images: ['https://picsum.photos/300/300?random=5'],
    location: '5舍楼下',
    userNick: '生活达人小丽',
    userAvatar: 'https://api.iconify.design/twemoji:woman.svg',
    isFav: false,
    createTime: Date.now() - 14400000
  },
  {
    _id: 'mock_6',
    title: '菜鸟驿站大件快递拼单分摊取件费',
    price: 3,
    category: 'group',
    type: 'group',
    currentCount: 3,
    maxCount: 3,
    images: ['https://picsum.photos/300/300?random=6'],
    location: '东门驿站',
    userNick: '快递小达人',
    userAvatar: 'https://api.iconify.design/twemoji:package.svg',
    isFav: false,
    createTime: Date.now() - 18000000
  }
];

Page({
  data: {
    showOnboarding: false,
    showStepGuide: false,
    loading: true,
    hasMore: false,
    page: 1,
    searchKeyword: '',
    currentCategory: 'all',
    categories: [
      { id: 'all', name: '全部', icon: '🔥' },
      { id: 'books', name: '二手书', icon: '📚' },
      { id: 'digital', name: '数码', icon: '💻' },
      { id: 'daily', name: '生活用品', icon: '🛋️' },
      { id: 'group', name: '拼单', icon: '🤝' }
    ],
    goodsList: [],
    fabExpanded: false
  },

  onLoad() {
    this.checkFirstLaunch();
    this.fetchGoodsList();
  },

  onShow() {
    // 如果从我的页面重置了引导
    const forceShowOnboarding = wx.getStorageSync('force_onboarding');
    if (forceShowOnboarding) {
      wx.removeStorageSync('force_onboarding');
      this.setData({ showOnboarding: true });
    }
  },

  // 1. 检查新手引导
  checkFirstLaunch() {
    const hasGuided = wx.getStorageSync('has_guided');
    if (!hasGuided) {
      this.setData({ showOnboarding: true });
    } else {
      this.checkStepGuide();
    }
  },

  onOnboardingFinish() {
    this.setData({ showOnboarding: false });
    this.checkStepGuide();
  },

  checkStepGuide() {
    const hasStepGuided = wx.getStorageSync('has_step_guided');
    if (!hasStepGuided) {
      setTimeout(() => {
        this.setData({ showStepGuide: true });
      }, 500);
    }
  },

  onStepGuideFinish() {
    this.setData({ showStepGuide: false });
  },

  // 2. 获取商品/拼单列表
  fetchGoodsList(isRefresh = false) {
    if (isRefresh) {
      this.setData({ page: 1, hasMore: true });
    }
    this.setData({ loading: true });

    if (wx.cloud) {
      const db = wx.cloud.database();
      let query = db.collection('goods');

      let whereCondition = {};
      if (this.data.currentCategory !== 'all') {
        whereCondition.category = this.data.currentCategory;
      }
      if (this.data.searchKeyword) {
        whereCondition.title = db.RegExp({
          regexp: this.data.searchKeyword,
          options: 'i'
        });
      }

      query.where(whereCondition)
        .orderBy('createTime', 'desc')
        .skip((this.data.page - 1) * 10)
        .limit(10)
        .get()
        .then(res => {
          let list = res.data;
          if (list.length === 0 && this.data.page === 1) {
            // 云库无数据时回退到 Mock 样例数据
            list = this.filterMockData();
          }
          this.setData({
            goodsList: isRefresh ? list : [...this.data.goodsList, ...list],
            loading: false,
            hasMore: res.data.length === 10
          });
          if (isRefresh) wx.stopPullDownRefresh();
        })
        .catch(err => {
          console.log('云开发数据加载失败，使用本地Mock数据:', err);
          this.setData({
            goodsList: this.filterMockData(),
            loading: false,
            hasMore: false
          });
          if (isRefresh) wx.stopPullDownRefresh();
        });
    } else {
      this.setData({
        goodsList: this.filterMockData(),
        loading: false,
        hasMore: false
      });
      if (isRefresh) wx.stopPullDownRefresh();
    }
  },

  filterMockData() {
    return MOCK_GOODS.filter(item => {
      const categoryMatch = this.data.currentCategory === 'all' || item.category === this.data.currentCategory;
      const searchMatch = !this.data.searchKeyword || item.title.includes(this.data.searchKeyword);
      return categoryMatch && searchMatch;
    });
  },

  // 3. 搜索与筛选
  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
  },

  onSearch() {
    this.fetchGoodsList(true);
  },

  clearSearch() {
    this.setData({ searchKeyword: '' });
    this.fetchGoodsList(true);
  },

  onSelectCategory(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ currentCategory: id });
    this.fetchGoodsList(true);
  },

  // 4. 收藏切换 (动画效果)
  toggleFavorite(e) {
    const { index, id } = e.currentTarget.dataset;
    const goodsList = [...this.data.goodsList];
    const isFav = !goodsList[index].isFav;
    goodsList[index].isFav = isFav;

    this.setData({ goodsList });

    wx.showToast({
      title: isFav ? '已加入收藏' : '已取消收藏',
      icon: 'none'
    });
  },

  // 5. FAB 展开与页面跳转
  toggleFab() {
    this.setData({ fabExpanded: !this.data.fabExpanded });
  },

  goToPublish(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ fabExpanded: false });
    wx.navigateTo({
      url: `/pages/publish/publish?type=${type}`
    });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 下拉刷新与上拉加载
  onPullDownRefresh() {
    this.fetchGoodsList(true);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.fetchGoodsList(false);
    }
  }
});
