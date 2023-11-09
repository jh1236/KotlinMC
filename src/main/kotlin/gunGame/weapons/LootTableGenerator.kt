package gunGame.weapons

import enums.Items
import gunGame.dp
import internal.structure.StringFile

object LootTableGenerator {

    fun genLootTable(path:String, item: Items, name: String, lore: List<String>, nbt: String): String {
        val strOut = """
                    |{
                    |  "type": "minecraft:command",
                    |  "pools": [
                    |    {
                    |      "rolls": 1,
                    |      "entries": [
                    |        {
                    |          "type": "minecraft:item",
                    |          "name": "$item",
                    |          "functions": [
                    |            {
                    |              "function": "minecraft:set_name",
                    |              "name": "$name"
                    |            },
                    |            {
                    |              "function": "minecraft:set_lore",
                    |              "lore": [${lore.joinToString(",")}]
                    |            },
                    |            {
                    |              "function": "minecraft:set_nbt",
                    |              "tag": "$nbt"
                    |            }
                    |          ]
                    |        }
                    |      ]
                    |    }
                    |  ]
                    |}
""".trimMargin()
        val list = ArrayList<String>()
        list.addAll(strOut.split("\n"))
        val s = StringFile(path, list, "loot_tables", "json")
        dp.addFile(s)
        return s.mcName
    }
}