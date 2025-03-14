package com.kmsl.dsl.extension

import com.kmsl.dsl.clazz.FieldName
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Field
import kotlin.reflect.KClass

val KClass<*>.fieldName
    get() = this.java.declaredFields.first {
        it.isAnnotationPresent(Id::class.java) or it.hasJakartaIdAnnotation()
    }?.run {
        isAccessible = true
        val hasFieldAnnotation = annotations.any { it is Field }
        if (hasFieldAnnotation) annotations.filterIsInstance<Field>().first().value
        else FieldName._ID
    } ?: this.simpleName!!