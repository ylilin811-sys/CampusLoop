package com.example.homework2.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homework2.data.local.AppDatabase
import com.example.homework2.data.local.UserPreferences
import com.example.homework2.data.local.entity.ActivityEntity
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.OrderRequestEntity
import com.example.homework2.data.local.entity.UserEntity
import com.example.homework2.data.repository.BookInfo
import com.example.homework2.data.repository.IsbnLookupResult
import com.example.homework2.data.repository.IsbnRepository
import com.example.homework2.data.repository.LocalMessageRepository
import com.example.homework2.data.repository.MarketRepository
import com.example.homework2.data.repository.MessageRepository
import com.example.homework2.domain.usecase.HandleOrderRequestUseCase
import com.example.homework2.domain.usecase.JoinGroupUseCase
import com.example.homework2.domain.usecase.OrderRequestResult
import com.example.homework2.domain.usecase.PlaceOrderUseCase
import com.example.homework2.ui.state.EmptyScene
import com.example.homework2.ui.state.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class IsbnLookupState {
    object Idle : IsbnLookupState()
    object Loading : IsbnLookupState()
    data class Success(val bookInfo: BookInfo) : IsbnLookupState()
    data class NotFound(val isbn: String) : IsbnLookupState()
    data class Error(val message: String) : IsbnLookupState()
}

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository: MarketRepository
    private val messageRepository: MessageRepository
    private val isbnRepository = IsbnRepository()
    private val userPreferences: UserPreferences

    private val placeOrderUseCase = PlaceOrderUseCase(database)
    private val handleOrderRequestUseCase = HandleOrderRequestUseCase(database)
    private val joinGroupUseCase = JoinGroupUseCase(database)

    val isOnboardingCompleted: StateFlow<Boolean>
    val isStepGuideCompleted: StateFlow<Boolean>
    val isContactSecurityAlertRead: StateFlow<Boolean>

    val currentUser: StateFlow<UserEntity?>
    val activities: StateFlow<List<ActivityEntity>>

    val messagesList: StateFlow<List<MessageEntity>>
    val unreadMessageCount: StateFlow<Int>

    private val _bannerMessage = MutableStateFlow<MessageEntity?>(null)
    val bannerMessage: StateFlow<MessageEntity?> = _bannerMessage.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSoldItems = MutableStateFlow(false)
    val showSoldItems: StateFlow<Boolean> = _showSoldItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isbnLookupState = MutableStateFlow<IsbnLookupState>(IsbnLookupState.Idle)
    val isbnLookupState: StateFlow<IsbnLookupState> = _isbnLookupState.asStateFlow()

    val goodsList: StateFlow<List<GoodsEntity>>
    val favoriteGoodsList: StateFlow<List<GoodsEntity>>
    val allGoods: StateFlow<List<GoodsEntity>>

    val uiState: StateFlow<UiState<List<GoodsEntity>>>

    init {
        repository = MarketRepository(
            database.goodsDao(),
            database.groupOrderDao(),
            database.favoriteDao(),
            database.userDao(),
            database.activityDao()
        )
        messageRepository = LocalMessageRepository(database.messageDao(), database.orderRequestDao())
        userPreferences = UserPreferences(application)

        isOnboardingCompleted = userPreferences.isOnboardingCompleted
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

        isStepGuideCompleted = userPreferences.isStepGuideCompleted
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

        isContactSecurityAlertRead = userPreferences.isContactSecurityAlertRead
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        currentUser = repository.getCurrentUser()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        activities = repository.getAllActivities()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        messagesList = messageRepository.getMessagesForUser(1)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        unreadMessageCount = messageRepository.getUnreadCount(1)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        goodsList = combine(_selectedCategory, _searchQuery, _showSoldItems) { category, query, showSold ->
            Triple(category, query, showSold)
        }.flatMapLatest { (category, query, showSold) ->
            _isLoading.value = true
            val flow = if (query.isNotBlank()) repository.searchGoods(query) else repository.getGoodsByCategory(category)
            flow.map { list ->
                if (!showSold) list.filter { it.status != "SOLD" } else list
            }.onEach { _isLoading.value = false }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        uiState = combine(goodsList, _isLoading, _searchQuery) { list, loading, query ->
            when {
                loading && list.isEmpty() -> UiState.Loading
                list.isEmpty() && query.isNotBlank() -> UiState.Empty(EmptyScene.SEARCH)
                list.isEmpty() -> UiState.Empty(EmptyScene.HOME)
                else -> UiState.Success(list)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

        favoriteGoodsList = repository.getFavoriteGoods()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        allGoods = repository.getAllGoods()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    fun selectCategory(category: String) { _selectedCategory.value = category }
    fun search(query: String) { _searchQuery.value = query }
    fun toggleShowSoldItems() { _showSoldItems.value = !_showSoldItems.value }
    fun toggleFavorite(goods: GoodsEntity) { viewModelScope.launch { repository.toggleFavorite(goods) } }
    fun deleteGoods(goods: GoodsEntity) { viewModelScope.launch { repository.deleteGoods(goods) } }
    fun updateGoodsStatus(id: Long, status: String) { viewModelScope.launch { repository.updateGoodsStatus(id, status) } }
    suspend fun getGoodsById(id: Long) = repository.getGoodsById(id)
    fun getGroupMembers(goodsId: Long) = repository.getGroupMembers(goodsId)

    fun markMessageAsRead(id: Long) { viewModelScope.launch { messageRepository.markAsRead(id) } }
    fun markAllMessagesAsRead() { viewModelScope.launch { messageRepository.markAllAsRead(1) } }
    fun deleteMessage(id: Long) { viewModelScope.launch { messageRepository.deleteMessage(id) } }
    fun restoreMessage(message: MessageEntity) { viewModelScope.launch { messageRepository.insertMessage(message) } }
    fun dismissBanner() { _bannerMessage.value = null }

    fun getOrderRequestByIdFlow(requestId: Long): Flow<OrderRequestEntity?> {
        return messageRepository.getOrderRequestByIdFlow(requestId)
    }

    fun placeOrder(context: Context, goods: GoodsEntity) {
        viewModelScope.launch {
            val user = currentUser.value
            val buyerNick = user?.nickname?.ifBlank { "爱吃零食的学弟" } ?: "爱吃零食的学弟"
            val buyerAv = user?.avatarPath?.ifBlank { "https://api.iconify.design/twemoji:boy.svg" } ?: "https://api.iconify.design/twemoji:boy.svg"

            val msg = placeOrderUseCase.execute(context, goods, buyerId = 2, buyerName = buyerNick, buyerAvatar = buyerAv)
            _bannerMessage.value = msg
        }
    }

    fun handleOrderRequest(
        context: Context,
        requestId: Long,
        isAccepted: Boolean,
        onResult: (OrderRequestResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = handleOrderRequestUseCase.execute(context, requestId, isAccepted)
            onResult(result)
        }
    }

    fun joinGroupOrderWithUseCase(context: Context, goodsId: Long) {
        viewModelScope.launch {
            val user = currentUser.value
            val myNick = user?.nickname ?: "我"
            val myAv = user?.avatarPath?.ifBlank { "https://api.iconify.design/twemoji:student.svg" } ?: "https://api.iconify.design/twemoji:student.svg"

            val (success, msg) = joinGroupUseCase.execute(context, goodsId, myNick, myAv)
            if (success && msg != null) {
                _bannerMessage.value = msg
            }
        }
    }

    // 演示模式工具
    fun demoSimulateOrderRequest(context: Context) {
        viewModelScope.launch {
            val secondhandGoods = allGoods.value.firstOrNull { it.type == "secondhand" }
            if (secondhandGoods != null) {
                placeOrder(context, secondhandGoods)
            }
        }
    }

    fun demoSimulateJoinGroup(context: Context) {
        viewModelScope.launch {
            val groupGoods = allGoods.value.firstOrNull { it.type == "group" }
            if (groupGoods != null) {
                joinGroupOrderWithUseCase(context, groupGoods.id)
            }
        }
    }

    fun demoClearAllMessages() {
        viewModelScope.launch {
            messageRepository.deleteAllMessages(1)
        }
    }

    suspend fun checkNicknameExists(nickname: String): Boolean {
        return repository.checkNicknameExists(nickname)
    }

    suspend fun saveUserProfile(nickname: String, avatarPath: String, wechatId: String = "", qqNumber: String = "") {
        repository.updateUserProfile(
            UserEntity(
                id = 1,
                nickname = nickname,
                avatarPath = avatarPath,
                wechatId = wechatId,
                qqNumber = qqNumber,
                updateTime = System.currentTimeMillis()
            )
        )
    }

    fun markContactSecurityAlertRead() {
        viewModelScope.launch {
            userPreferences.setContactSecurityAlertRead(true)
        }
    }

    fun searchBookByIsbn(isbn: String) {
        viewModelScope.launch {
            _isbnLookupState.value = IsbnLookupState.Loading
            val result = isbnRepository.lookupBookByIsbn(isbn)
            _isbnLookupState.value = when (result) {
                is IsbnLookupResult.Success -> IsbnLookupState.Success(result.bookInfo)
                is IsbnLookupResult.NotFound -> IsbnLookupState.NotFound(result.isbn)
                is IsbnLookupResult.Error -> IsbnLookupState.Error(result.message)
            }
        }
    }

    fun resetIsbnState() {
        _isbnLookupState.value = IsbnLookupState.Idle
    }

    fun publishGoods(
        title: String,
        price: Double,
        category: String,
        type: String,
        images: List<String>,
        description: String,
        location: String,
        maxCount: Int = 1,
        deadline: String = "",
        extraInfoJson: String = "",
        contactOverrideJson: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val sellerNick = user?.nickname?.ifBlank { "我" } ?: "我"
            val sellerAv = user?.avatarPath?.ifBlank { "https://api.iconify.design/twemoji:student.svg" } ?: "https://api.iconify.design/twemoji:student.svg"

            val entity = GoodsEntity(
                title = title,
                price = price,
                category = category,
                type = type,
                images = if (images.isEmpty()) listOf("https://picsum.photos/600/600?random=${System.currentTimeMillis()}") else images,
                description = description,
                location = if (location.isBlank()) "宿舍区" else location,
                sellerName = sellerNick,
                sellerAvatar = sellerAv,
                currentCount = 1,
                maxCount = if (type == "group") maxCount else 1,
                deadline = deadline,
                extraInfoJson = extraInfoJson,
                contactOverrideJson = contactOverrideJson,
                status = "AVAILABLE"
            )
            repository.insertGoods(entity)
            onSuccess()
        }
    }

    fun completeOnboarding() { viewModelScope.launch { userPreferences.setOnboardingCompleted(true) } }
    fun completeStepGuide() { viewModelScope.launch { userPreferences.setStepGuideCompleted(true) } }
    fun resetGuides() { viewModelScope.launch { userPreferences.resetAllGuides() } }
}
