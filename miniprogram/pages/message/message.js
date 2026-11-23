// pages/message/message.js - 消息与留言页逻辑
const app = getApp();

Page({
  data: {
    userAvatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    chatList: [
      {
        id: 'chat_1',
        nick: '学霸小张',
        avatar: 'https://api.iconify.design/twemoji:boy.svg',
        goodsTitle: '考研高等数学辅导讲义 (张宇/李永乐)',
        lastMsg: '同学你好，书还在的，明天中午图书馆门口方便面交吗？',
        time: '10:42',
        unread: 1,
        messages: [
          { text: '你好！请问这本高数讲义还在吗？', isSelf: true },
          { text: '同学你好，书还在的，明天中午图书馆门口方便面交吗？', isSelf: false }
        ]
      },
      {
        id: 'chat_2',
        nick: '干饭人阿杰',
        avatar: 'https://api.iconify.design/twemoji:man.svg',
        goodsTitle: '麦当劳超值双人餐 差1人拼团分摊',
        lastMsg: '好的，我们凑齐2人了，微信发个AA口令红包即可。',
        time: '昨天',
        unread: 0,
        messages: [
          { text: '哥们，麦当劳拼单加我一个！', isSelf: true },
          { text: '好的，我们凑齐2人了，微信发个AA口令红包即可。', isSelf: false }
        ]
      }
    ],
    activeChatIndex: null,
    activeChat: null,
    inputMsg: ''
  },

  onLoad(options) {
    if (app.globalData.userInfo) {
      this.setData({ userAvatar: app.globalData.userInfo.avatarUrl });
    }

    if (options.sellerNick && options.goodsTitle) {
      const sellerNick = decodeURIComponent(options.sellerNick);
      const goodsTitle = decodeURIComponent(options.goodsTitle);

      const newChat = {
        id: 'chat_' + Date.now(),
        nick: sellerNick,
        avatar: 'https://api.iconify.design/twemoji:person.svg',
        goodsTitle: goodsTitle,
        lastMsg: '我已经发起咨询，期待回复！',
        time: '刚刚',
        unread: 0,
        messages: [
          { text: `你好，我对你发布的【${goodsTitle}】很感兴趣！`, isSelf: true }
        ]
      };

      const chatList = [newChat, ...this.data.chatList];
      this.setData({
        chatList,
        activeChatIndex: 0,
        activeChat: newChat
      });
    }
  },

  openChat(e) {
    const index = e.currentTarget.dataset.index;
    const chatList = [...this.data.chatList];
    chatList[index].unread = 0; // 清空未读数

    this.setData({
      chatList,
      activeChatIndex: index,
      activeChat: chatList[index]
    });
  },

  closeChat() {
    this.setData({
      activeChatIndex: null,
      activeChat: null
    });
  },

  onInputMsg(e) {
    this.setData({ inputMsg: e.detail.value });
  },

  sendMsg() {
    const text = this.data.inputMsg.trim();
    if (!text) return;

    const { activeChatIndex, chatList } = this.data;
    const targetChat = { ...chatList[activeChatIndex] };
    const messages = [...targetChat.messages, { text, isSelf: true }];

    targetChat.messages = messages;
    targetChat.lastMsg = text;
    targetChat.time = '刚刚';

    chatList[activeChatIndex] = targetChat;

    this.setData({
      chatList,
      activeChat: targetChat,
      inputMsg: ''
    });

    // 模拟卖家自动回复 (3秒后)
    setTimeout(() => {
      const replyMsgs = [...targetChat.messages, { text: '收到！同学我会在约定的时间地点等你哈～', isSelf: false }];
      targetChat.messages = replyMsgs;
      targetChat.lastMsg = '收到！同学我会在约定的时间地点等你哈～';
      chatList[activeChatIndex] = targetChat;

      if (this.data.activeChatIndex === activeChatIndex) {
        this.setData({ activeChat: targetChat, chatList });
      }
    }, 1500);
  }
});
