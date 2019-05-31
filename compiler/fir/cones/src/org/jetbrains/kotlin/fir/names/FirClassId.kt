package org.jetbrains.kotlin.fir.names

class FirClassId(val packageFqName: FirFqName, val relativeClassName: FirFqName) {

    constructor(packageFqName: FirFqName, shortName: FirName) : this(packageFqName, FirFqName.create(shortName))

    val shortClassName get() = relativeClassName.shortName()

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

    fun asSingleFqName(): FirFqName {
        return FirFqName.create(*packageFqName.segments(), *relativeClassName.segments())
    }

    fun createNestedClassId(name: FirName): FirClassId {
        return FirClassId(packageFqName, relativeClassName.child(name))
    }
}