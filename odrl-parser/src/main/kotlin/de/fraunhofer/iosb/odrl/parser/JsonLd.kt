package de.fraunhofer.iosb.odrl.parser

import java.lang.annotation.Inherited

@Inherited
@Target(AnnotationTarget.CLASS)
annotation class JsonLd(val context: String)
