package org.maxwelltech.recipetree

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform