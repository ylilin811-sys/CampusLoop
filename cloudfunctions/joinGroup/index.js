// cloudfunctions/joinGroup/index.js - 加入拼单原子云函数
const cloud = require('wx-server-sdk');
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();
const _ = db.command;

exports.main = async (event, context) => {
  const wxContext = cloud.getWXContext();
  const openid = wxContext.OPENID;
  const { goodsId, nickName, avatarUrl } = event;

  if (!goodsId) {
    return { success: false, msg: '缺少 goodsId 参数' };
  }

  try {
    // 数据库事务或原子操作：只有当前人数 < maxCount 且尚未加入时更新
    const goodsRes = await db.collection('goods').doc(goodsId).get();
    const goods = goodsRes.data;

    if (goods.currentCount >= goods.maxCount) {
      return { success: false, msg: '拼单已满员' };
    }

    const alreadyJoined = (goods.members || []).some(m => m.openid === openid);
    if (alreadyJoined) {
      return { success: false, msg: '你已加入该拼单' };
    }

    const newMember = {
      openid,
      nickName: nickName || '校友',
      avatarUrl: avatarUrl || '',
      isCreator: false,
      joinTime: Date.now()
    };

    const updateRes = await db.collection('goods').doc(goodsId).update({
      data: {
        currentCount: _.inc(1),
        members: _.push(newMember)
      }
    });

    const isFull = (goods.currentCount + 1) >= goods.maxCount;

    return {
      success: true,
      currentCount: goods.currentCount + 1,
      isFull,
      msg: isFull ? '拼单成功满员！' : '加入拼单成功！'
    };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
};
