package kaffa.portrait.aot.meta.serde

class StringPool {
    private val stringToIndex = mutableMapOf<String, Int>()
    private val indexToString = mutableListOf<String>()

    fun intern(string: String): Int {
        return stringToIndex.getOrPut(string) {
            val index = indexToString.size
            indexToString.add(string)
            index
        }
    }

    fun getString(index: Int): String = indexToString[index]

    fun getStrings(): List<String> = indexToString.toList()

    fun size(): Int = indexToString.size
}

class ReadOnlyStringPool(private val strings: List<String>) {
    fun getString(index: Int): String = strings[index]
    fun size(): Int = strings.size
}