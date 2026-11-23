package com.example.homework2.data.repository

import com.example.homework2.data.remote.GoogleBooksApiService
import com.example.homework2.data.remote.OpenLibraryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

data class BookInfo(
    val isbn: String,
    val title: String,
    val author: String,
    val publisher: String,
    val edition: String,
    val coverUrl: String
)

sealed class IsbnLookupResult {
    data class Success(val bookInfo: BookInfo) : IsbnLookupResult()
    data class NotFound(val isbn: String) : IsbnLookupResult()
    data class Error(val message: String) : IsbnLookupResult()
}

class IsbnRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val googleBooksApi: GoogleBooksApiService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleBooksApiService::class.java)

    private val openLibraryApi: OpenLibraryApiService = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenLibraryApiService::class.java)

    suspend fun lookupBookByIsbn(isbn: String): IsbnLookupResult = withContext(Dispatchers.IO) {
        val cleanIsbn = isbn.trim().replace("-", "")

        // 1. 尝试 Google Books API
        try {
            val googleRes = googleBooksApi.getBookByIsbn("isbn:$cleanIsbn")
            if (googleRes.totalItems > 0 && !googleRes.items.isNullOrEmpty()) {
                val volume = googleRes.items[0].volumeInfo
                if (volume != null && !volume.title.isNullOrBlank()) {
                    val cover = volume.imageLinks?.thumbnail
                        ?: volume.imageLinks?.smallThumbnail ?: ""
                    val secureCover = if (cover.startsWith("http://")) cover.replace("http://", "https://") else cover

                    val bookInfo = BookInfo(
                        isbn = cleanIsbn,
                        title = volume.title,
                        author = volume.authors?.joinToString(", ") ?: "未知作者",
                        publisher = volume.publisher ?: "未知出版社",
                        edition = if (!volume.publishedDate.isNullOrBlank()) "第1版 (${volume.publishedDate.take(4)}年)" else "第1版",
                        coverUrl = secureCover
                    )
                    return@withContext IsbnLookupResult.Success(bookInfo)
                }
            }
        } catch (e: Exception) {
            // Google Books 失败，回退到 Open Library API
        }

        // 2. 回退到 Open Library API
        try {
            val key = "ISBN:$cleanIsbn"
            val openRes = openLibraryApi.getBookByBibKeys(key)
            val bookData = openRes[key]
            if (bookData != null && !bookData.title.isNullOrBlank()) {
                val cover = bookData.cover?.large ?: bookData.cover?.medium ?: ""
                val secureCover = if (cover.startsWith("http://")) cover.replace("http://", "https://") else cover

                val bookInfo = BookInfo(
                    isbn = cleanIsbn,
                    title = bookData.title,
                    author = bookData.authors?.mapNotNull { it.name }?.joinToString(", ") ?: "未知作者",
                    publisher = bookData.publishers?.mapNotNull { it.name }?.joinToString(", ") ?: "未知出版社",
                    edition = if (!bookData.publishDate.isNullOrBlank()) "第1版 (${bookData.publishDate})" else "第1版",
                    coverUrl = secureCover
                )
                return@withContext IsbnLookupResult.Success(bookInfo)
            }
        } catch (e: Exception) {
            return@withContext IsbnLookupResult.Error("网络连接超时，请检查网络设置或稍后重试")
        }

        return@withContext IsbnLookupResult.NotFound(cleanIsbn)
    }
}
