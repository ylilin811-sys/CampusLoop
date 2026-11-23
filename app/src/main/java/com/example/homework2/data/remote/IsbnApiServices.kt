package com.example.homework2.data.remote

import com.example.homework2.data.remote.model.GoogleBooksResponse
import com.example.homework2.data.remote.model.OpenLibraryBookData
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {
    @GET("books/v1/volumes")
    suspend fun getBookByIsbn(
        @Query("q") query: String
    ): GoogleBooksResponse
}

interface OpenLibraryApiService {
    @GET("api/books")
    suspend fun getBookByBibKeys(
        @Query("bibkeys") bibKeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jsCmd: String = "data"
    ): Map<String, OpenLibraryBookData>
}
