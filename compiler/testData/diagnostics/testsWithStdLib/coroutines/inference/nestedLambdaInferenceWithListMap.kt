// !LANGUAGE: +NewInference

val configurations4 = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>listOf<!>(
    3 to mapOf(
        2 to listOf(
            1 to listOf(
                {
                    2
                }
            )
        )
    )
)

val configurations3 = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>listOf<!>(
    3 to mapOf(
        2 to listOf(
            {
                2
            }
        )
    )
)

val configurations2 = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>mapOf<!>(
    2 <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER, IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>to<!> <!TYPE_MISMATCH, TYPE_MISMATCH!>listOf(
        {
            2
        }
    )<!>
)

val configurations1 = listOf(
    {
        2
    }
)