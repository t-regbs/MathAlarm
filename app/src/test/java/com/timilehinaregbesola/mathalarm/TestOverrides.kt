package com.timilehinaregbesola.mathalarm

import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import org.koin.dsl.module

val testModule = module {
    single<DateTimeProvider> { DateTimeProviderFake() }
}
