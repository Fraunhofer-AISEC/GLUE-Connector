/*
 * Copyright 2025 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.config

/**
 * The base path annotation defines the root path to the config object that the annotated class is relative to.
 * If a class is annotated with `@BasePath("example"), then all properties are looked up relative to that.
 * If this annotation is absent, the name of the class is used as lower case without the optional Config suffix.
 */
annotation class BasePath(
    val path: String,
)
