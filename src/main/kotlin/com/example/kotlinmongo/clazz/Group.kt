package com.example.kotlinmongo.clazz

import com.example.kotlinmongo.extension.document
import org.bson.Document
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1

/**
 * grouping 을 할때 내부적으로 matchOperation 을 사용합니다.
 * side effect 가 발생하지 않으려면 Group By 는 마지막 줄에 위치 시키는 것이 좋습니다.
 */
open class Group<T, R>(
    val document: Document,
    private val properties: MutableSet<KProperty1<T, R>> = mutableSetOf(),
) {
    class Sum(
        private val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        infix fun <T, R> Field<T, R>.alias(
            value: String,
        ) =
            GroupOperationWrapper(document, groupOperation.sum(this.key.toDotPath()).`as`(value))
    }

    class Average(
        private val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        infix fun <T, R> Field<T, R>.alias(
            value: String,
        ) =
            GroupOperationWrapper(document, groupOperation.avg(this.key.toDotPath()).`as`(value))
    }

    class Max(
        private val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        infix fun <T, R> Field<T, R>.alias(
            value: String,
        ) =
            GroupOperationWrapper(document, groupOperation.max(this.key.toDotPath()).`as`(value))
    }

    class Min(
        private val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        infix fun <T, R> Field<T, R>.alias(
            value: String,
        ) =
            GroupOperationWrapper(document, groupOperation.min(this.key.toDotPath()).`as`(value))
    }

    class Count(
        private val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        infix fun alias(
            value: String,
        ) =
            GroupOperationWrapper(document, groupOperation.count().`as`(value))
    }

    infix fun Field<T, R>.by(
        type: GroupType,
    ) {
        properties.add(this.key)
    }

    infix fun Field<T, R>.and(
        field: Field<T, R>,
    ) {
        properties.add(this.key)
        properties.add(field.key)
    }

    class GroupOperationWrapper(
        val document: Document,
        private val groupOperation: GroupOperation,
    ) {
        fun toAggregation(): Aggregation {
            val matchOperation = document.matchOperation()
            return Aggregation.newAggregation(matchOperation, groupOperation)
        }

        infix fun sum(
            block: Sum.() -> GroupOperationWrapper,
        ) =
            Sum(document, this.groupOperation).block()

        infix fun avg(
            block: Average.() -> GroupOperationWrapper,
        ) =
            Average(document, this.groupOperation).block()

        infix fun max(
            block: Max.() -> GroupOperationWrapper,
        ) =
            Max(document, this.groupOperation).block()

        infix fun min(
            block: Min.() -> GroupOperationWrapper,
        ) =
            Min(document, this.groupOperation).block()

        infix fun count(
            block: Count.() -> GroupOperationWrapper,
        ) =
            Count(document, this.groupOperation).block()

        private fun Document.matchOperation(): MatchOperation {
            val criteria = Criteria()
            for ((key, value) in this) {
                criteria.and(key).`is`(value)
            }
            return Aggregation.match(criteria)
        }
    }

    infix fun sum(
        block: Sum.() -> GroupOperationWrapper,
    ) =
        Sum(document, Aggregation.group(*properties.map { "\$${it.name}" }.toTypedArray())).block()

    infix fun avg(
        block: Average.() -> GroupOperationWrapper,
    ) = Average(document, Aggregation.group(*properties.map { "\$${it.name}" }.toTypedArray())).block()

    infix fun max(
        block: Max.() -> GroupOperationWrapper,
    ) = Max(document, Aggregation.group(*properties.map { "\$${it.name}" }.toTypedArray())).block()

    infix fun min(
        block: Min.() -> GroupOperationWrapper,
    ) = Min(document, Aggregation.group(*properties.map { "\$${it.name}" }.toTypedArray())).block()

    infix fun count(
        block: Count.() -> GroupOperationWrapper,
    ) = Count(document, Aggregation.group(*properties.map { "\$${it.name}" }.toTypedArray())).block()

//    fun sumOf(
//        alias: String = "total",
//        type: KClass<*>? = null,
//        sumField: Document.() -> Field<T, *>,
//    ): Aggregation {
//        val fieldName = sumField.invoke(Document()).key.name
//
//        return if (type == null) {
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).sum("\$$fieldName").`as`(alias),
//            )
//        } else {
//            val expression = AggregationExpression {
//                Document(MongoTypeCaster.cast(type), "\$$fieldName")
//            }
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).sum(expression).`as`(alias),
//            )
//        }
//    }
//
//    fun avgOf(
//        alias: String = "avg",
//        type: KClass<*>? = null,
//        avgField: Document.() -> Field<T, *>,
//    ): Aggregation {
//        val fieldName = avgField.invoke(Document()).key.name
//
//        return if (type == null) {
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).avg("\$$fieldName").`as`(alias),
//            )
//        } else {
//            val expression = AggregationExpression {
//                Document(MongoTypeCaster.cast(type), "\$$fieldName")
//            }
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).avg(expression).`as`(alias),
//            )
//        }
//    }
//
//    fun maxOf(
//        alias: String = "max",
//        type: KClass<*>? = null,
//        maxField: Document.() -> Field<T, *>,
//    ): Aggregation {
//        val fieldName = maxField.invoke(Document()).key.name
//
//        return if (type == null) {
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).max("\$$fieldName").`as`(alias),
//            )
//        } else {
//            val expression = AggregationExpression {
//                Document(MongoTypeCaster.cast(type), "\$$fieldName")
//            }
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).max(expression).`as`(alias),
//            )
//        }
//    }
//
//    fun minOf(
//        alias: String = "min",
//        type: KClass<*>? = null,
//        minField: Document.() -> Field<T, *>,
//    ): Aggregation {
//        val fieldName = minField.invoke(Document()).key.name
//
//        return if (type == null) {
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).min("\$$fieldName").`as`(alias),
//            )
//        } else {
//            val expression = AggregationExpression {
//                Document(MongoTypeCaster.cast(type), "\$$fieldName")
//            }
//            val matchStage = document.matchOperation()
//            Aggregation.newAggregation(
//                matchStage,
//                Aggregation.group(key.name).min(expression).`as`(alias),
//            )
//        }
//    }
//
//    fun count(
//        alias: String = "count",
//    ): Aggregation {
//        val matchStage = document.matchOperation()
//        return Aggregation.newAggregation(
//            matchStage,
//            Aggregation.group(key.name).count().`as`(alias),
//        )
//    }
}

enum class GroupType {
    SINGLE
}