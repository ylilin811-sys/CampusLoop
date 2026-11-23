package com.example.homework2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.homework2.R
import com.example.homework2.data.local.dao.ActivityDao
import com.example.homework2.data.local.dao.FavoriteDao
import com.example.homework2.data.local.dao.GoodsDao
import com.example.homework2.data.local.dao.GroupOrderDao
import com.example.homework2.data.local.dao.MessageDao
import com.example.homework2.data.local.dao.OrderRequestDao
import com.example.homework2.data.local.dao.UserDao
import com.example.homework2.data.local.entity.ActivityEntity
import com.example.homework2.data.local.entity.CategoryDetail
import com.example.homework2.data.local.entity.FavoriteEntity
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.data.local.entity.GroupOrderEntity
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.data.local.entity.OrderRequestEntity
import com.example.homework2.data.local.entity.UserEntity
import com.example.homework2.util.CategoryDetailParser
import com.example.homework2.util.ContactInfo
import com.example.homework2.util.ContactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Database(
    entities = [
        GoodsEntity::class,
        GroupOrderEntity::class,
        FavoriteEntity::class,
        UserEntity::class,
        ActivityEntity::class,
        MessageEntity::class,
        OrderRequestEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goodsDao(): GoodsDao
    abstract fun groupOrderDao(): GroupOrderDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun userDao(): UserDao
    abstract fun activityDao(): ActivityDao
    abstract fun messageDao(): MessageDao
    abstract fun orderRequestDao(): OrderRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_market.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(
                        database.goodsDao(),
                        database.groupOrderDao(),
                        database.userDao(),
                        database.activityDao(),
                        database.messageDao()
                    )
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val currentActs = database.activityDao().getAllActivities().firstOrNull()
                    if (currentActs.isNullOrEmpty()) {
                        ensureInitialActivities(database.activityDao())
                    }
                }
            }
        }

        private suspend fun ensureInitialActivities(activityDao: ActivityDao) {
            val initialActivities = listOf(
                ActivityEntity(
                    titleRes = R.string.banner_graduation_title,
                    subtitleRes = R.string.banner_graduation_subtitle,
                    categoryTarget = "graduation"
                ),
                ActivityEntity(
                    titleRes = R.string.banner_group_title,
                    subtitleRes = R.string.banner_group_subtitle,
                    categoryTarget = "group"
                ),
                ActivityEntity(
                    titleRes = R.string.banner_hot_title,
                    subtitleRes = R.string.banner_hot_subtitle,
                    categoryTarget = "hot"
                ),
                ActivityEntity(
                    titleRes = R.string.banner_textbook_title,
                    subtitleRes = R.string.banner_textbook_subtitle,
                    categoryTarget = "books"
                )
            )
            activityDao.insertActivities(initialActivities)
        }

        private suspend fun populateInitialData(
            goodsDao: GoodsDao,
            groupDao: GroupOrderDao,
            userDao: UserDao,
            activityDao: ActivityDao,
            messageDao: MessageDao
        ) {
            userDao.insertUser(
                UserEntity(
                    id = 1,
                    nickname = "校园小霸王",
                    avatarPath = "",
                    wechatId = "campus_boss88",
                    qqNumber = "88886666"
                )
            )

            messageDao.insertMessage(
                MessageEntity(
                    receiverId = 1,
                    senderName = "闲圈助手",
                    senderAvatar = "https://api.iconify.design/twemoji:party-popper.svg",
                    type = MessageType.SYSTEM.code,
                    title = "🎉 欢迎来到闲圈！",
                    content = "闲圈是专属于校园学友的二手交易与拼单平台。在这里您可以轻松闲置变现、低价捡漏或寻找拼单搭子！",
                    isRead = false
                )
            )

            ensureInitialActivities(activityDao)

            val bookDetail18 = CategoryDetail.BookDetail(
                bookName = "高等数学辅导讲义",
                author = "张宇",
                publisher = "北京理工大学出版社",
                edition = "第18版",
                isbn = "9787568288888",
                course = "高等数学(一)",
                notes = "少量笔记",
                damaged = "无破损",
                isLatestEdition = true
            )

            val bookDetail17 = CategoryDetail.BookDetail(
                bookName = "高等数学辅导讲义",
                author = "张宇",
                publisher = "北京理工大学出版社",
                edition = "第17版",
                isbn = "9787568288888",
                course = "高等数学(一)",
                notes = "大量笔记",
                damaged = "少量划痕",
                isLatestEdition = false
            )

            val digitalDetail = CategoryDetail.DigitalDetail(
                brand = "Apple",
                model = "iPad Air 5",
                purchaseDate = "2023年9月",
                specs = "64GB 紫色",
                warranty = "在保",
                batteryHealth = "98%",
                accessories = listOf("充电器", "原装包装盒"),
                repaired = "无拆修"
            )

            val dailyDetail = CategoryDetail.DailyDetail(
                brand = "九阳",
                usageDuration = "3个月",
                flaw = "无瑕疵"
            )

            val sellerContact1 = ContactUtils.toJson(ContactInfo(wechatId = "xueba_zhang888", qqNumber = "12345678", isPublic = true))
            val sellerContact2 = ContactUtils.toJson(ContactInfo(wechatId = "senior_brother66", qqNumber = "87654321", isPublic = true))

            val initialGoods = listOf(
                GoodsEntity(
                    title = "考研高等数学辅导讲义 (张宇第18版)",
                    price = 18.0,
                    category = "books",
                    type = "secondhand",
                    images = listOf(
                        "https://picsum.photos/600/600?random=1",
                        "https://picsum.photos/600/600?random=11"
                    ),
                    description = "九成新无划痕，送历年真题解析，楼下当面交易。",
                    location = "图书馆门口",
                    sellerName = "学霸小张",
                    sellerAvatar = "https://api.iconify.design/twemoji:boy.svg",
                    extraInfoJson = CategoryDetailParser.toJson(bookDetail18),
                    contactOverrideJson = sellerContact1,
                    status = "AVAILABLE"
                ),
                GoodsEntity(
                    title = "考研高等数学辅导讲义 (张宇第17版)",
                    price = 12.0,
                    category = "books",
                    type = "secondhand",
                    images = listOf(
                        "https://picsum.photos/600/600?random=10"
                    ),
                    description = "17版旧书低价转让，内容大体一致，适合预算有限的学弟学妹。",
                    location = "紫荆5舍",
                    sellerName = "毕业学长",
                    sellerAvatar = "https://api.iconify.design/twemoji:man.svg",
                    extraInfoJson = CategoryDetailParser.toJson(bookDetail17),
                    contactOverrideJson = sellerContact2,
                    status = "AVAILABLE"
                ),
                GoodsEntity(
                    title = "麦当劳超值双人餐 差1人拼团分摊",
                    price = 15.5,
                    category = "group",
                    type = "group",
                    images = listOf(
                        "https://picsum.photos/600/600?random=2"
                    ),
                    description = "麦当劳双人餐拼单，AA分摊，楼下食堂门口拿号当面享用。",
                    location = "二食堂门口",
                    sellerName = "干饭人阿杰",
                    sellerAvatar = "https://api.iconify.design/twemoji:man.svg",
                    currentCount = 1,
                    maxCount = 2,
                    deadline = "20:00",
                    contactOverrideJson = sellerContact1,
                    status = "AVAILABLE"
                ),
                GoodsEntity(
                    title = "iPad Air 5 64G 紫色 99新 带Apple Pencil",
                    price = 3200.0,
                    category = "digital",
                    type = "secondhand",
                    images = listOf(
                        "https://picsum.photos/600/600?random=3",
                        "https://picsum.photos/600/600?random=13"
                    ),
                    description = "无划痕保养极好，箱说齐全，自提可微瑕优惠。",
                    location = "12舍楼下",
                    sellerName = "极客小明",
                    sellerAvatar = "https://api.iconify.design/twemoji:man-technologist.svg",
                    extraInfoJson = CategoryDetailParser.toJson(digitalDetail),
                    contactOverrideJson = sellerContact1,
                    status = "RESERVED"
                ),
                GoodsEntity(
                    title = "九成新九阳电水壶 1.8L 快速烧水",
                    price = 35.0,
                    category = "daily",
                    type = "secondhand",
                    images = listOf(
                        "https://picsum.photos/600/600?random=5"
                    ),
                    description = "宿舍可用低功率，3分钟快速烧开。",
                    location = "5舍楼下",
                    sellerName = "生活达人小丽",
                    sellerAvatar = "https://api.iconify.design/twemoji:woman.svg",
                    extraInfoJson = CategoryDetailParser.toJson(dailyDetail),
                    contactOverrideJson = sellerContact2,
                    status = "SOLD"
                )
            )

            initialGoods.forEach { goods ->
                val id = goodsDao.insertGoods(goods)
                if (goods.type == "group") {
                    groupDao.insertMember(
                        GroupOrderEntity(
                            goodsId = id,
                            memberName = goods.sellerName,
                            memberAvatar = goods.sellerAvatar,
                            isCreator = true
                        )
                    )
                }
            }
        }
    }
}
