package com.pengrad.telegrambot

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendLocation
import com.pengrad.telegrambot.request.SendMessage
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun main(){
    println("Запуск бота tgBotSpbStopsByANV")

    val tgSendBusInfo = TGSendBusInfo()
    val busStopsSPB = tgSendBusInfo.readBusStopsDataSPB("data/BusListStops.csv")
    val tgBotToken : String? = tgSendBusInfo.readTokenFromFile("D:\\tgBotSpbStopsByANV.ini")

    tgBotToken?.let {
        println("Токен: $tgBotToken")
    }
    ?: run {
        println("Токен не найден")
        return
    }

    val bot = TelegramBot(tgBotToken)

    // Register for tgBot updates
    bot.setUpdatesListener {
        updates ->
        for (indexUpdate in updates){

            println("Принятое сообщение: $indexUpdate")
            val chatMsg : Message? = indexUpdate.message()
            val chatId: Long? = indexUpdate.message()?.chat()?.id()
            chatId?.let {
                val tgAnswerStr = tgSendBusInfo.tgCreateAnswer( indexUpdate )
                bot.execute( SendMessage( it, tgAnswerStr ) )
                chatMsg?.let {
                    it.location()?.let {
                        val tglocation = chatMsg.location()
                        var minDist = Double.MAX_VALUE
                        lateinit var nearestBusStopsSPB : BusStopsData
//                        nearestBusStopsSPB  = busStopsSPB.minBy {
//                            tgSendBusInfo.distanceInKmBetweenEarthCoordinates(
//                                tglocation.latitude().toDouble(),
//                                tglocation.longitude().toDouble(),
//                                it.location.longitude,
//                                it.location.latitude)
//                        }

                        for (i in busStopsSPB){
                            val dist = tgSendBusInfo.distanceInKmBetweenEarthCoordinates(
                                tglocation.latitude().toDouble(),
                                tglocation.longitude().toDouble(),
                                i.location.longitude,
                                i.location.latitude)
                            if(dist < minDist){
                                minDist = dist
                                nearestBusStopsSPB = BusStopsData(i.name,i.adr,BusStopsDataLocation(i.location.longitude,i.location.latitude))
                            }
                        }

                        //send answer - message to user
                        val tgLocationAnswer = "Ближайшая: ${nearestBusStopsSPB.name}\n" +
                                "Расстояние: ${minDist.format(1)} км \n" +
                                "Координаты:\n ${nearestBusStopsSPB.location.longitude.format(6).replace(',','.') }\n " +
                                nearestBusStopsSPB.location.latitude.format(6).replace(',','.')
                        println("Ответ пользователю: $tgLocationAnswer")
                        bot.execute( SendMessage(chatId, tgLocationAnswer))

                        //send location to user
                        bot.execute(SendLocation(chatId, nearestBusStopsSPB.location.longitude.toFloat(), nearestBusStopsSPB.location.latitude.toFloat()))

                        //println("Ближайшая: ${nearestBusStopsSPB.name} дист: ${minDist.toInt()} ${nearestBusStopsSPB.location.longitude},${nearestBusStopsSPB.location.latitude}")
                    }
                }.run { UpdatesListener.CONFIRMED_UPDATES_ALL  }
            }.run { UpdatesListener.CONFIRMED_UPDATES_ALL  }

        }
        UpdatesListener.CONFIRMED_UPDATES_ALL
    }
}
fun Double.format(digits: Int) = "%.${digits}f".format(this)

class TGSendBusInfo{
    fun readTokenFromFile(fileName: String): String?{
        val fileData : List<String?> = File(fileName).bufferedReader().readLines()
        return if(fileData.lastIndex != -1)  fileData[0] else null
     }

    fun readBusStopsDataSPB( fileName : String) : List<BusStopsData> {
        //val `in` = FileReader("data/BusListStops.csv")
        val `in` = FileReader(fileName)
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setHeader("№,Вид транспортного средства,Тип объекта,Наименование остановки,Официальное наименование,Расположение,Маршруты,Координаты")
            .setSkipHeaderRecord(true)
            .build()
        val busListStopsRecords: Iterable<CSVRecord> = csvFormat.parse(`in`)
        val busStopsSPB : List<BusStopsData>  = busListStopsRecords
            .map { index ->
                BusStopsData( index.get(3), index.get(5), parseBusStopsLocation( index.get(7) ) )
            }
        return busStopsSPB
    }
    fun  distanceInKmBetweenEarthCoordinates(lat1 : Double, lon1 : Double, lat2 : Double, lon2 : Double) : Double{
        val earthRadiusKm = 6371
        val dLat = degreesToRadians(lat2-lat1)
        val dLon = degreesToRadians(lon2-lon1)
        val a = sin(dLat/2) * sin(dLat/2) +
                sin(dLon/2) * sin(dLon/2) * cos(degreesToRadians(lat1)) * cos(degreesToRadians(lat2))
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return earthRadiusKm * c
    }

    private fun parseBusStopsLocation(locStr : String) : BusStopsDataLocation {
        val f = locStr.split(",")
        return BusStopsDataLocation(f[0].toDouble(),f[1].toDouble())
    }

    private fun degreesToRadians(degrees : Double) : Double{
        return degrees * Math.PI / 180
    }

    fun tgCreateAnswer(tgRecivedMsg : Update?) : String{
        val tgAnswer : StringBuilder =  StringBuilder()
        tgAnswer.clear()

        val chatMsg : Message? = tgRecivedMsg?.message()

        chatMsg?.let {
            it.messageId()?.let {
                tgAnswer.append("messageId: ${chatMsg.messageId()}\n")
            }
            it.text()?.let {
                val t = chatMsg.text()
                tgAnswer.append("text: $t\n")
            }
            it.location()?.let {
                val location = chatMsg.location()
                tgAnswer.append("longitude: ${location.longitude()} \nlatitude: ${location.latitude()}\n")
            }
            it.chat()?.firstName()?.let {
                tgAnswer.append("firstName: $it")
            }
        }
        if (tgAnswer.isEmpty() ) tgAnswer.append("Нет ответа")
        return tgAnswer.toString()
    }
}

data class BusStopsData(
    var name: String,
    var adr: String,
    var location: BusStopsDataLocation
){
    override fun toString(): String {
        return "1: name='$name', 2: adr='$adr', 3: location=${location.longitude} ${location.latitude}"
    }
}

data class BusStopsDataLocation(
    val longitude: Double,
    val latitude: Double
)





//val tgCreateAnswerLambda: (Update)-> String = { tgRecivedMsg ->
//    val tgAnswer : StringBuilder =  StringBuilder()
//    tgAnswer.clear()
//
//    val chatMsg : Message? = tgRecivedMsg.message()
//    val chatId: Long = tgRecivedMsg.message().chat().id()
//
//    chatMsg?.let {
//        it.messageId()?.let {
//            tgAnswer.append("messageId: $it\n")
//        }
//        it.text().let {
//            val t = it
//            if( t!=null ) tgAnswer.append("text: $t\n")
//        }
//        it.location()?.let {
//            val location = chatMsg.location()
//            tgAnswer.append("longitude: ${location.longitude()} \nlatitude: ${location.latitude()}")
//        }
//        it.chat().firstName().let {
//            tgAnswer.append("firstName: $it")
//        }
//
//    }
//    if (tgAnswer.isEmpty() ) tgAnswer.append("Нет ответа")
//     tgAnswer.toString()
//}

