package com.itera.pam.p2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ==========================================
// TUGAS PRAKTIKUM 2: NEWS FEED SIMULATOR
// Nama: Marvin Karyanda
// NIM: 123140185
// Mata Kuliah: Pengembangan Aplikasi Mobile (IF25-22017)
// ==========================================

enum class NewsCategory {
    BREAKING,
    TECH,
    CAMPUS_ITERA,
    SPORTS
}

data class NewsArticle(
    val id: Int,
    val title: String,
    val summary: String,
    val category: NewsCategory,
    val author: String,
    val isRead: Boolean = false
)

data class FeedUiState(
    val articles: List<NewsArticle> = emptyList(),
    val activeFilter: NewsCategory? = null,
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

// Service Simulasi Pengambilan Berita (Coroutines Async/Parallel)
class NewsService {
    suspend fun fetchBreakingNews(): List<NewsArticle> {
        delay(900) // Simulasi latency network
        return listOf(
            NewsArticle(1, "ITERA Resmikan Lab AI Terbaru", "Fasilitas riset komputasi modern siap digunakan mahasiswa.", NewsCategory.CAMPUS_ITERA, "Humas ITERA"),
            NewsArticle(2, "Kotlin 2.1 Resmi Dirilis!", "Peningkatan performa compiler dan dukungan KMP yang makin matang.", NewsCategory.TECH, "JetBrains")
        )
    }

    suspend fun fetchTechNews(): List<NewsArticle> {
        delay(800) // Simulasi latency network
        return listOf(
            NewsArticle(3, "Compose Multiplatform Jadi Standar Mobile", "Pengembangan cross-platform Android & iOS semakin efisien.", NewsCategory.TECH, "Android Dev"),
            NewsArticle(4, "AI Agent di IDE Membantu Efisiensi Dev", "Model AI terbaru mampu pair-programming secara real-time.", NewsCategory.TECH, "Tech Insider")
        )
    }

    suspend fun fetchCampusNews(): List<NewsArticle> {
        delay(700) // Simulasi latency network
        return listOf(
            NewsArticle(5, "Mahasiswa IF ITERA Juara Hackathon Nasional", "Inovasi aplikasi mobile berbasis AI raih medali emas.", NewsCategory.CAMPUS_ITERA, "Redaksi Kampus")
        )
    }

    // Live Streaming Berita Ticker menggunakan Flow
    fun liveNewsTickerFlow(): Flow<NewsArticle> = flow {
        val incomingNews = listOf(
            NewsArticle(6, "Update Cuaca Lampung: Cerah Berawan", "Suhu 28°C di sekitar kampus ITERA.", NewsCategory.BREAKING, "BMKG"),
            NewsArticle(7, "Pendaftaran Asisten Praktikum PAM Dibuka", "Segera lengkapi berkas sebelum deadline.", NewsCategory.CAMPUS_ITERA, "Lab IF"),
            NewsArticle(8, "Tim Robotika ITERA Lolos ke Babak Final", "Robot otonom siap bertanding minggu depan.", NewsCategory.CAMPUS_ITERA, "UKM Robotika")
        )

        for (news in incomingNews) {
            delay(1200) // Emit berita baru setiap 1.2 detik
            emit(news)
        }
    }
}

// Manager State Manajemen menggunakan StateFlow
class NewsFeedManager(private val service: NewsService) {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // 1. Fetch Parallel menggunakan coroutine async
    suspend fun loadInitialFeeds() = coroutineScope {
        _uiState.update { it.copy(isLoading = true) }

        val breakingDeferred = async { service.fetchBreakingNews() }
        val techDeferred = async { service.fetchTechNews() }
        val campusDeferred = async { service.fetchCampusNews() }

        val allArticles = breakingDeferred.await() + techDeferred.await() + campusDeferred.await()

        _uiState.update {
            it.copy(
                articles = allArticles,
                unreadCount = allArticles.count { !it.isRead },
                isLoading = false
            )
        }
    }

    // 2. Filter data
    fun setCategoryFilter(category: NewsCategory?) {
        _uiState.update { it.copy(activeFilter = category) }
    }

    // 3. Tambah berita baru
    fun addArticle(article: NewsArticle) {
        _uiState.update {
            val updated = listOf(article) + it.articles
            it.copy(
                articles = updated,
                unreadCount = updated.count { !it.isRead }
            )
        }
    }
}

fun main() = runBlocking {
    println("==================================================")
    println("   NEWS FEED SIMULATOR (Coroutines & Flow)       ")
    println("   Pengembang: Marvin Karyanda (123140185)       ")
    println("==================================================\n")

    val service = NewsService()
    val feedManager = NewsFeedManager(service)

    // Background StateFlow Collector
    val stateJob = launch {
        feedManager.uiState.collect { state ->
            if (!state.isLoading && state.articles.isNotEmpty()) {
                println("[STATE UPDATE] Total Berita: ${state.articles.size} | Unread: ${state.unreadCount}")
            }
        }
    }

    // Benchmark Async Parallel Fetching
    println(">>> 1. Mengambil berita dari 3 sumber secara paralel (Async)...")
    val startTime = System.currentTimeMillis()
    feedManager.loadInitialFeeds()
    val duration = System.currentTimeMillis() - startTime
    println(">>> Berhasil dimuat dalam ${duration}ms! (Paralel optimal < 1500ms)\n")

    // Menampilkan artikel awal
    println("--- DAFTAR ARTIKEL TERKINI ---")
    feedManager.uiState.value.articles.forEachIndexed { index, article ->
        println("${index + 1}. [${article.category}] ${article.title} - ${article.author}")
        println("   \"${article.summary}\"")
    }
    println("------------------------------\n")

    // Live Stream Flow Demo
    println(">>> 2. Mengaktifkan Live Stream Flow (Real-time news ticker)...")
    service.liveNewsTickerFlow()
        .filter { it.category == NewsCategory.CAMPUS_ITERA || it.category == NewsCategory.BREAKING }
        .map { "🔔 [LIVE TICKER] ${it.category}: ${it.title}" }
        .collect { tickerText ->
            println(tickerText)
            feedManager.addArticle(
                NewsArticle(
                    id = (10..99).random(),
                    title = tickerText,
                    summary = "Berita live realtime melalui Kotlin Flow",
                    category = NewsCategory.CAMPUS_ITERA,
                    author = "Live Stream"
                )
            )
        }

    println("\n==================================================")
    println("News Feed Simulation Complete!")
    println("==================================================")

    stateJob.cancel()
}
