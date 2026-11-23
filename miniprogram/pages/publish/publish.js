// pages/publish/publish.js - 发布页逻辑
const app = getApp();

Page({
  data: {
    type: 'secondhand', // 'secondhand' | 'group'
    images: [],
    title: '',
    price: '',
    categoryIndex: 0,
    categoryList: [
      { id: 'books', name: '二手书 📚' },
      { id: 'digital', name: '数码 💻' },
      { id: 'daily', name: '生活用品 🛋️' },
      { id: 'group', name: '拼单 🤝' }
    ],
    maxCount: 2,
    endDate: '',
    endTime: '20:00',
    todayDate: '',
    location: '',
    description: '',
    submitting: false
  },

  onLoad(options) {
    if (options.type) {
      this.setData({
        type: options.type === 'group' ? 'group' : 'secondhand',
        categoryIndex: options.type === 'group' ? 3 : 0
      });
    }

    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const dateStr = `${yyyy}-${mm}-${dd}`;

    this.setData({
      todayDate: dateStr,
      endDate: dateStr
    });
  },

  // 1. 类型与分类
  switchType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      type,
      categoryIndex: type === 'group' ? 3 : 0
    });
  },

  onCategoryChange(e) {
    this.setData({ categoryIndex: e.detail.value });
  },

  // 2. 输入框监听
  onInputTitle(e) { this.setData({ title: e.detail.value }); },
  onInputPrice(e) { this.setData({ price: e.detail.value }); },
  onInputMaxCount(e) { this.setData({ maxCount: e.detail.value }); },
  onDateChange(e) { this.setData({ endDate: e.detail.value }); },
  onTimeChange(e) { this.setData({ endTime: e.detail.value }); },
  onInputLocation(e) { this.setData({ location: e.detail.value }); },
  onInputDescription(e) { this.setData({ description: e.detail.value }); },

  // 3. 图片选择与预览
  chooseImage() {
    const remaining = 9 - this.data.images.length;
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFiles = res.tempFiles.map(file => file.tempFilePath);
        this.setData({
          images: [...this.data.images, ...tempFiles]
        });
      }
    });
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.src;
    wx.previewImage({
      current,
      urls: this.data.images
    });
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = [...this.data.images];
    images.splice(index, 1);
    this.setData({ images });
  },

  // 4. 提交发布逻辑
  async onSubmit() {
    const { title, price, description, type, images, categoryList, categoryIndex, location, maxCount, endDate, endTime } = this.data;

    // 表单校验
    if (!title.trim()) return wx.showToast({ title: '请输入标题', icon: 'none' });
    if (!price || parseFloat(price) <= 0) return wx.showToast({ title: '请输入有效价格', icon: 'none' });
    if (!description.trim()) return wx.showToast({ title: '请输入详细描述', icon: 'none' });
    if (images.length === 0) return wx.showToast({ title: '请至少上传1张图片', icon: 'none' });

    if (type === 'group') {
      if (!maxCount || parseInt(maxCount) < 2) return wx.showToast({ title: '拼单人数上限至少为2人', icon: 'none' });
    }

    this.setData({ submitting: true });

    try {
      // 1) 云存储图片上传 (或本地使用临时路径)
      const cloudImages = [];
      if (wx.cloud) {
        for (let i = 0; i < images.length; i++) {
          const filePath = images[i];
          const cloudPath = `goods/${Date.now()}_${i}.png`;
          try {
            const uploadRes = await wx.cloud.uploadFile({ cloudPath, filePath });
            cloudImages.push(uploadRes.fileID);
          } catch (e) {
            cloudImages.push(filePath);
          }
        }
      } else {
        cloudImages.push(...images);
      }

      // 2) 构造写入数据库的对象
      const userInfo = app.globalData.userInfo || { nickName: '校友', avatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0' };

      const payload = {
        title,
        price: parseFloat(price),
        category: categoryList[categoryIndex].id,
        type,
        images: cloudImages,
        description,
        location: location || '宿舍区',
        userNick: userInfo.nickName,
        userAvatar: userInfo.avatarUrl,
        isFav: false,
        createTime: Date.now()
      };

      if (type === 'group') {
        payload.currentCount = 1; // 发起人占1名
        payload.maxCount = parseInt(maxCount);
        payload.deadline = `${endDate} ${endTime}`;
        payload.members = [
          { nickName: userInfo.nickName, avatarUrl: userInfo.avatarUrl, isCreator: true }
        ];
      }

      // 3) 写入云数据库
      if (wx.cloud) {
        const db = wx.cloud.database();
        await db.collection('goods').add({ data: payload });
      }

      this.setData({ submitting: false });
      wx.showToast({
        title: type === 'group' ? '拼单发起成功！' : '二手发布成功！',
        icon: 'success'
      });

      setTimeout(() => {
        wx.switchTab({ url: '/pages/home/home' });
      }, 1200);

    } catch (err) {
      console.error('发布失败:', err);
      this.setData({ submitting: false });
      wx.showToast({ title: '发布失败，请重试', icon: 'none' });
    }
  }
});
