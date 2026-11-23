// cloudfunctions/sendMessage/index.js - 消息发送云函数
const cloud = require('wx-server-sdk');
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();

exports.main = async (event, context) => {
  const wxContext = cloud.getWXContext();
  const senderOpenid = wxContext.OPENID;
  const { receiverOpenid, goodsId, text } = event;

  if (!receiverOpenid || !text) {
    return { success: false, msg: '缺少必要的参数' };
  }

  try {
    const res = await db.collection('messages').add({
      data: {
        senderId: senderOpenid,
        receiverId: receiverOpenid,
        goodsId: goodsId || '',
        text: text,
        isRead: false,
        createTime: Date.now()
      }
    });

    return {
      success: true,
      msgId: res._id
    };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
};
