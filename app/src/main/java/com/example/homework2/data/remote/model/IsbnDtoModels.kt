package com.example.homework2.data.remote.model

import com.google.gson.annotations.SerializedName

// --- Google Books API DTO ---
data class GoogleBooksResponse(
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("items") val items: List<GoogleBookItem>? = null
)

data class GoogleBookItem(
    @SerializedName("volumeInfo") val volumeInfo: GoogleVolumeInfo? = null
)

data class GoogleVolumeInfo(
    @SerializedName("title") val title: String? = null,
    @SerializedName("authors") val authors: List<String>? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("publishedDate") val publishedDate: String? = null,
    @SerializedName("imageLinks") val imageLinks: GoogleImageLinks? = null
)

data class GoogleImageLinks(
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("smallThumbnail") val smallThumbnail: String? = null
)

// --- Open Library API DTO ---
data class OpenLibraryBookData(
    @SerializedName("title") val title: String? = null,
    @SerializedName("authors") val authors: List<OpenLibraryAuthor>? = null,
    @SerializedName("publishers") val publishers: List<OpenLibraryPublisher>? = null,
    @SerializedName("publish_date") val publishDate: String? = null,
    @SerializedName("cover") val cover: OpenLibraryCover? = null
)

data class OpenLibraryAuthor(
    @SerializedName("name") val name: String? = null
)

data class OpenLibraryPublisher(
    @SerializedName("name") val name: String? = null
)

data class OpenLibraryCover(
    @SerializedName("large") val large: String? = null,
    @SerializedName("medium") val medium: String? = null
)
