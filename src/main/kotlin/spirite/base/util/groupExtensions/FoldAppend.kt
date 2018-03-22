package spirite.base.util.groupExtensions

fun <E> Collection<Collection<E>>.foldAppend() : List<E> =
        this.fold(mutableListOf(), {agg, it -> agg.apply { addAll(it) }})

fun <I,O> Collection<I>.mapAggregated( mapping: (I) -> Collection<O>) : List<O> =
        this.fold(mutableListOf(), {agg, it -> agg.apply { addAll(mapping.invoke(it)) }})