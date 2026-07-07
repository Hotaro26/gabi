package com.material.downloader.di

import org.koin.core.module.Module
import org.koin.dsl.module
import com.material.downloader.util.PlatformBridge
import com.material.downloader.util.AndroidPlatformBridge

actual fun platformModule(): Module = module {
    single<PlatformBridge> { AndroidPlatformBridge(get()) }
}
