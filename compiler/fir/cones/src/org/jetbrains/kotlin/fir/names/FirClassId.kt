package org.jetbrains.kotlin.fir.names

import org.jetbrains.kotlin.name.ClassId

class FirClassId(val packageFqName: FirFqName, val relativeClassName: FirFqName) {

    constructor(packageFqName: FirFqName, shortName: FirName) : this(packageFqName, FirFqName.create(shortName))

    val shortClassName get() = relativeClassName.shortName()

    val outerClassId: FirClassId?
        get() {
            val parentFqName = relativeClassName.parent()
            if (parentFqName.isRoot()) return null
            return FirClassId(packageFqName, parentFqName)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirClassId) return false

        if (relativeClassName != other.relativeClassName) return false
        if (packageFqName != other.packageFqName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = relativeClassName.hashCode()
        result = 31 * result + packageFqName.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append(packageFqName.segments().joinToString(separator = "/"))
            if (!packageFqName.isRoot()) {
                append("/")
            }
            append(relativeClassName)
        }
    }

    fun asSingleFqName(): FirFqName {
        return FirFqName.create(*packageFqName.segments(), *relativeClassName.segments())
    }

    fun createNestedClassId(name: FirName): FirClassId {
        return FirClassId(packageFqName, relativeClassName.child(name))
    }

    fun asClassId() = ClassId.fromString(toString())
}