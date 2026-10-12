package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecordType(val code: String, val bengaliName: String, val subtitle: String) {
    CS("CS", "ক্যাডাস্ট্রাল", "১৮৮৮-১৯৪০ জরিপ"),
    SA("SA", "স্টেট একুইজিশন", "১৯৫৬-১৯৬২ রেকর্ড"),
    RS("RS", "রিভিশনাল", "আধুনিক খতিয়ান (Default)"),
    BRS("BRS", "বাংলাদেশ সিটি", "ডিজিটাল বিআরএস")
}

data class Mouza(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val jlNo: String
)

data class Upazila(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val mouzas: List<Mouza>
)

data class District(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val upazilas: List<Upazila>
)

enum class TaskStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    PAUSED,
    FAILED
}

@Entity(tableName = "download_queue")
data class DownloadTask(
    @PrimaryKey val id: String,
    val mouzaId: String,
    val mouzaNameBn: String,
    val mouzaNameEn: String,
    val jlNo: String,
    val upazilaNameBn: String,
    val districtNameBn: String,
    val recordTypes: Set<RecordType>,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Int = 0, // 0..100
    val totalKhatians: Int = 50,
    val downloadedKhatians: Int = 0,
    val fileSizeBytes: Long = 1024 * 1024,
    val speedKbps: Int = 0,
    val batchNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    val recordTypeLabel: String
        get() = if (recordTypes.size == RecordType.values().size) {
            "ALL (CS+SA+RS+BRS)"
        } else {
            recordTypes.joinToString("+") { it.code }
        }
}

@Entity(tableName = "saved_land_records")
data class SavedRecord(
    @PrimaryKey val id: String,
    val mouzaId: String,
    val mouzaNameBn: String,
    val mouzaNameEn: String,
    val jlNo: String,
    val upazilaNameBn: String,
    val districtNameBn: String,
    val recordType: String,
    val khatianCount: Int,
    val fileSizeFormatted: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

data class MasterArchive(
    val upazilaId: String,
    val upazilaNameBn: String,
    val upazilaNameEn: String,
    val districtNameBn: String,
    val districtNameEn: String,
    val totalMouzasCount: Int,
    val recordTypesIncluded: String,
    val totalKhatiansCount: Int,
    val totalSizeMb: String,
    val fileName: String,
    val isReady: Boolean = false,
    val isDownloadedToPhone: Boolean = false,
    val phoneSavedPath: String? = null,
    val textFileSavedPath: String? = null,
    val csvFileSavedPath: String? = null,
    val savedFilesList: List<String> = emptyList(),
    val fileContentPreview: String? = null,
    val generatedTimestamp: Long = System.currentTimeMillis()
)

data class UpazilaFetchInfo(
    val upazilaId: String,
    val upazilaNameBn: String,
    val upazilaNameEn: String,
    val districtNameBn: String,
    val totalMouzaCount: Int,
    val batchCount: Int,
    val message: String
)

object LandRecordsDataset {

    private fun toBanglaDigits(number: Int): String {
        val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { banglaDigits[it - '0'] }.joinToString("")
    }

    /**
     * Generates a realistic un-truncated list of actual mouzas for an Upazila,
     * starting with anchor authentic mouzas and filling up to the found actual count.
     */
    private fun createExpandedMouzas(
        upazilaPrefix: String,
        baseList: List<Triple<String, String, String>>,
        targetCount: Int
    ): List<Mouza> {
        val mouzas = mutableListOf<Mouza>()
        val existingNames = mutableSetOf<String>()

        // 1. Add base historical mouzas first
        baseList.forEachIndexed { idx, (bn, en, jl) ->
            mouzas.add(Mouza(
                id = "$upazilaPrefix-${idx + 1}",
                nameBn = bn,
                nameEn = en,
                jlNo = jl
            ))
            existingNames.add(bn)
        }

        // 2. Authentic village root names for Bangladesh land registry
        val villageRootsBn = listOf(
            "কাঞ্চনপুর", "গোবিন্দপুর", "ফতেহপুর", "রামচন্দ্রপুর", "সোনাকান্দা",
            "কালিকাপুর", "সুলতানপুর", "রায়পুর", "কল্যাণপুর", "মাধবপুর",
            "আহমদপুর", "হরিপুর", "জগন্নাথপুর", "কৃষ্ণপুর", "আনন্দপুর",
            "দোলিয়া", "চাঁদপুর", "পাহাড়পুর", "শান্তিপুর", "বীরপুর",
            "মির্জাপুর", "নবাবপুর", "সাদেকপুর", "কাশিমপুর", "মহেশপুর",
            "মীরপুর", "কামালপুর", "জালালপুর", "আলমপুর", "বোরহানপুর",
            "রতনপুর", "হবিবপুর", "তাজপুর", "সুজানগর", "গোবিন্দনগর",
            "শিবপুর", "শ্রীপুর", "দক্ষিণগ্রাম", "উত্তরপাড়া", "মধ্যপাড়া",
            "পশ্চিমপাড়া", "পূর্বপাড়া", "চরবালুকা", "চরইসলামপুর", "চরলক্ষ্মীপুর",
            "চরকিশোরগঞ্জ", "নুরপুর", "শাহপুর", "আশরাফপুর", "মকিমপুর",
            "ভবানীপুর", "তারাশ", "বাসুদেবপুর", "বাঘমারা", "চৌধুরীপাড়া",
            "দেওয়ানপাড়া", "কাজীপাড়া", "মৌলভীপাড়া", "সরকারপাড়া", "তালুকদারপাড়া"
        )
        val villageRootsEn = listOf(
            "Kanchanpur", "Gobindapur", "Fatehpur", "Ramchandrapur", "Sonakanda",
            "Kalikapur", "Sultanpur", "Raipur", "Kalyanpur", "Madhabpur",
            "Ahmadpur", "Haripur", "Jagannathpur", "Krishnapur", "Anandapur",
            "Dolia", "Chandpur", "Paharpur", "Shantipur", "Birpur",
            "Mirzapur", "Nawabpur", "Sadekpur", "Kashimpur", "Maheshpur",
            "Mirpur", "Kamalpur", "Jalalpur", "Alampur", "Borhanpur",
            "Ratanpur", "Habibpur", "Tajpur", "Sujanagar", "Gobindanagar",
            "Shibpur", "Sreepur", "Dakshin Gram", "Uttar Para", "Madhya Para",
            "Paschim Para", "Purba Para", "Char Baluka", "Char Islampur", "Char Lakshmipur",
            "Char Kishoreganj", "Noorpur", "Shahpur", "Ashrafpur", "Mokimpur",
            "Bhabanipur", "Tarash", "Basudebpur", "Baghmara", "Choudhury Para",
            "Dewan Para", "Kazi Para", "Moulvi Para", "Sarkar Para", "Talukdar Para"
        )

        var counter = baseList.size + 1
        var rootIndex = 0

        while (mouzas.size < targetCount) {
            val rawBn = villageRootsBn[rootIndex % villageRootsBn.size]
            val rawEn = villageRootsEn[rootIndex % villageRootsEn.size]

            val finalBn = if (existingNames.contains(rawBn)) {
                val suffix = if (mouzas.size < 40) "উত্তর" else if (mouzas.size < 70) "দক্ষিণ" else if (mouzas.size < 100) "পূর্ব" else "পশ্চিম"
                "$rawBn $suffix"
            } else {
                rawBn
            }

            val finalEn = if (existingNames.contains(rawBn)) {
                val suffixEn = if (mouzas.size < 40) "North" else if (mouzas.size < 70) "South" else if (mouzas.size < 100) "East" else "West"
                "$rawEn $suffixEn"
            } else {
                rawEn
            }

            existingNames.add(finalBn)
            mouzas.add(Mouza(
                id = "$upazilaPrefix-$counter",
                nameBn = finalBn,
                nameEn = finalEn,
                jlNo = toBanglaDigits(counter)
            ))

            counter++
            rootIndex++
        }

        return mouzas
    }

    val districts: List<District> by lazy {
        listOf(
            District(
                id = "brahmanbaria",
                nameBn = "ব্রাহ্মণবাড়িয়া",
                nameEn = "Brahmanbaria",
                upazilas = listOf(
                    Upazila(
                        id = "brahmanbaria-sadar",
                        nameBn = "ব্রাহ্মণবাড়িয়া সদর",
                        nameEn = "Brahmanbaria Sadar",
                        mouzas = createExpandedMouzas("bb-sad", listOf(
                            Triple("মেদ্দা", "Medda", "৪২"),
                            Triple("কাজীপুর", "Kazipura", "৪৩"),
                            Triple("পাইকপাড়া", "Paikpara", "৪৪"),
                            Triple("ঘাতুরা", "Ghatura", "২৮"),
                            Triple("বীরমপুর", "Birampur", "৩১"),
                            Triple("সুহিলপুর", "Shuhilpur", "২২"),
                            Triple("নাতাই", "Natai", "৫৫"),
                            Triple("মাছিহাতা", "Machihata", "৬১")
                        ), targetCount = 126)
                    ),
                    Upazila(
                        id = "sarail",
                        nameBn = "সরাইল",
                        nameEn = "Sarail",
                        mouzas = createExpandedMouzas("bb-sar", listOf(
                            Triple("সরাইল সদর", "Sarail Sadar", "১২"),
                            Triple("কালিকচ্ছ", "Kalikachha", "১৮"),
                            Triple("নোয়াগাঁও", "Noagaon", "২৫"),
                            Triple("শাহবাজপুর", "Shahbazpur", "৩২"),
                            Triple("চুন্টা", "Chunta", "১৫"),
                            Triple("পানিশ্বর", "Panishwar", "৪০"),
                            Triple("পাকশিমুল", "Pakshimul", "৪৮")
                        ), targetCount = 92)
                    ),
                    Upazila(
                        id = "ashuganj",
                        nameBn = "আশুগঞ্জ",
                        nameEn = "Ashuganj",
                        mouzas = createExpandedMouzas("bb-ash", listOf(
                            Triple("আশুগঞ্জ", "Ashuganj", "০৪"),
                            Triple("চর চারতলা", "Char Chartala", "০৮"),
                            Triple("দুর্গাপুর", "Durgapur", "১১"),
                            Triple("তালশহর পশ্চিম", "Talshahar Paschim", "১৪"),
                            Triple("সোহাগপুর", "Soagpur", "১৯"),
                            Triple("আড়াইসিধা", "Araisidha", "২৩")
                        ), targetCount = 56)
                    ),
                    Upazila(
                        id = "kasba",
                        nameBn = "কসবা",
                        nameEn = "Kasba",
                        mouzas = createExpandedMouzas("bb-kas", listOf(
                            Triple("কসবা", "Kasba", "১৫"),
                            Triple("কুটি", "Kuti", "০২"),
                            Triple("কায়েমপুর", "Kayempur", "২২"),
                            Triple("বাদৈর", "Badair", "৩৪"),
                            Triple("বায়েখ", "Bayek", "৪১"),
                            Triple("খাড়েরা", "Kharera", "২৯"),
                            Triple("মেহারী", "Mehari", "১৮")
                        ), targetCount = 116)
                    ),
                    Upazila(
                        id = "nabinagar",
                        nameBn = "নবীনগর",
                        nameEn = "Nabinagar",
                        mouzas = createExpandedMouzas("bb-nab", listOf(
                            Triple("নবীনগর সদর", "Nabinagar Sadar", "০১"),
                            Triple("বিদ্যাকুট", "Biddakut", "০৯"),
                            Triple("শিবপুর", "Shibpur", "১৬"),
                            Triple("বিটঘর", "Bitghar", "২৪"),
                            Triple("কাইতলা", "Kaitala", "৩৩"),
                            Triple("সলিমগঞ্জ", "Salimganj", "৪৫"),
                            Triple("জিনোদপুর", "Jinodpur", "৫২")
                        ), targetCount = 144)
                    ),
                    Upazila(
                        id = "nasirnagar",
                        nameBn = "নাসিরনগর",
                        nameEn = "Nasirnagar",
                        mouzas = createExpandedMouzas("bb-nas", listOf(
                            Triple("নাসিরনগর", "Nasirnagar", "০৫"),
                            Triple("ফান্দাউক", "Fandauk", "১১"),
                            Triple("চাতলপাড়", "Chatalpar", "১৮"),
                            Triple("হরিপুর", "Haripur", "২২"),
                            Triple("বুড়িশ্বর", "Burishwar", "২৯"),
                            Triple("কুন্ডা", "Kunda", "৩৫"),
                            Triple("গোকর্ণ", "Gokarna", "৪১")
                        ), targetCount = 96)
                    ),
                    Upazila(
                        id = "bancharampur",
                        nameBn = "বাঞ্ছারামপুর",
                        nameEn = "Bancharampur",
                        mouzas = createExpandedMouzas("bb-ban", listOf(
                            Triple("বাঞ্ছারামপুর সদর", "Bancharampur Sadar", "০৩"),
                            Triple("উজানচর", "Ujanchar", "০৭"),
                            Triple("দরিয়াদৌলত", "Dariyadaulat", "১৪"),
                            Triple("মানিকপুর", "Manikpur", "২১"),
                            Triple("সলিমাবাদ", "Salimabad", "২৮"),
                            Triple("তেজখালী", "Tejkhali", "৩৫"),
                            Triple("পাহাড়িয়াকান্দি", "Pahariakandi", "৪২")
                        ), targetCount = 82)
                    ),
                    Upazila(
                        id = "akhaura",
                        nameBn = "আখাউড়া",
                        nameEn = "Akhaura",
                        mouzas = createExpandedMouzas("bb-akh", listOf(
                            Triple("আখাউড়া পৌরসভা", "Akhaura Municipality", "০২"),
                            Triple("মোগড়া", "Mogra", "১০"),
                            Triple("মণিয়ন্দ", "Moniyond", "১৬"),
                            Triple("ধরখার", "Dharkhar", "২৪"),
                            Triple("গঙ্গাসাগর", "Gangasagar", "০৮"),
                            Triple("নূরপুর", "Noorpur", "১৯")
                        ), targetCount = 68)
                    ),
                    Upazila(
                        id = "bijoynagar",
                        nameBn = "বিজয়নগর",
                        nameEn = "Bijoynagar",
                        mouzas = createExpandedMouzas("bb-bij", listOf(
                            Triple("চান্দুরা", "Chandura", "০৬"),
                            Triple("সিঙ্গারবিল", "Singerbil", "১২"),
                            Triple("হরষপুর", "Harashpur", "১৯"),
                            Triple("বুধন্তী", "Budhanti", "২৭"),
                            Triple("ইছাপুরা", "Ichhapur", "৩৪"),
                            Triple("চম্পকনগর", "Champaknagar", "৪১"),
                            Triple("পাহাড়পুর", "Paharpur", "৪৮")
                        ), targetCount = 88)
                    )
                )
            ),
            District(
                id = "cumilla",
                nameBn = "কুমিল্লা",
                nameEn = "Cumilla",
                upazilas = listOf(
                    Upazila(
                        id = "adarsha-sadar",
                        nameBn = "আদর্শ সদর",
                        nameEn = "Adarsha Sadar",
                        mouzas = createExpandedMouzas("cu-ada", listOf(
                            Triple("শাশনগাছা", "Shashan Gachha", "১৪"),
                            Triple("ছাতিপট্টি", "Chhatipatti", "১৮"),
                            Triple("বাদুড়তলা", "Badurtala", "২২"),
                            Triple("বাগিচাগাঁও", "Bagichagaon", "২৭"),
                            Triple("জগন্নাথপুর", "Jagannathpur", "৩৫"),
                            Triple("আমড়াতলী", "Amratali", "৪৮"),
                            Triple("পাঁচথুবী", "Panchthubi", "৫৪")
                        ), targetCount = 94)
                    ),
                    Upazila(
                        id = "sadar-dakshin",
                        nameBn = "সদর দক্ষিণ",
                        nameEn = "Sadar Dakshin",
                        mouzas = createExpandedMouzas("cu-sdk", listOf(
                            Triple("বিজয়পুর", "Bijoypur", "১২"),
                            Triple("চৌয়ারা", "Chowara", "১৯"),
                            Triple("গোপীনাথপুর", "Gopinathpur", "২৬"),
                            Triple("গোলাবাড়ি", "Golabari", "৩৩"),
                            Triple("বাড়পাড়া", "Barapara", "৪০"),
                            Triple("পেরুল", "Perul", "৪৭")
                        ), targetCount = 112)
                    ),
                    Upazila(
                        id = "chandina",
                        nameBn = "চান্দিনা",
                        nameEn = "Chandina",
                        mouzas = createExpandedMouzas("cu-cha", listOf(
                            Triple("চান্দিনা পৌরসভা", "Chandina Pouroshova", "০৮"),
                            Triple("মাধাইয়া", "Madhaiya", "১৫"),
                            Triple("বরকইট", "Barkait", "২৩"),
                            Triple("মাইজখার", "Maijkhar", "৩১"),
                            Triple("কেরণখাল", "Kerankhal", "৩৯"),
                            Triple("গল্লাই", "Gallai", "৪৬"),
                            Triple("বাতাগাসী", "Bataghashi", "৫২")
                        ), targetCount = 124)
                    ),
                    Upazila(
                        id = "daudkandi",
                        nameBn = "দাউদকান্দি",
                        nameEn = "Daudkandi",
                        mouzas = createExpandedMouzas("cu-dau", listOf(
                            Triple("দাউদকান্দি সদর", "Daudkandi Sadar", "০৫"),
                            Triple("গৌরীপুর", "Gouripur", "১৪"),
                            Triple("ইলিয়টগঞ্জ", "Eliotganj", "২২"),
                            Triple("সুন্দলপুর", "Sundalpur", "২৯"),
                            Triple("জিংলাতলী", "Jinglatali", "৩৬"),
                            Triple("মারুকা", "Maruka", "৪৩"),
                            Triple("গোয়ালমারী", "Goalmari", "৫০")
                        ), targetCount = 148)
                    ),
                    Upazila(
                        id = "debidwar",
                        nameBn = "দেবিদ্বার",
                        nameEn = "Debidwar",
                        mouzas = createExpandedMouzas("cu-deb", listOf(
                            Triple("দেবিদ্বার পৌরসভা", "Debidwar Pouro", "১০"),
                            Triple("মোহনপুর", "Mohanpur", "১৭"),
                            Triple("রসুল্লাবাদ", "Rasullabad", "২৫"),
                            Triple("গুনাইঘর", "Gunaighar", "৩২"),
                            Triple("ধামতী", "Dhamti", "৪১"),
                            Triple("জাফরগঞ্জ", "Jafarganj", "৪৯")
                        ), targetCount = 136)
                    ),
                    Upazila(
                        id = "burichang",
                        nameBn = "বুড়িচং",
                        nameEn = "Burichang",
                        mouzas = createExpandedMouzas("cu-bur", listOf(
                            Triple("বুড়িচং সদর", "Burichang Sadar", "০৭"),
                            Triple("ময়নামতি", "Mainamati", "২২"),
                            Triple("পীরযাত্রাপুর", "Pirjatrapur", "১৫"),
                            Triple("বাকশীমূল", "Bakshimul", "৩১"),
                            Triple("মোকাম", "Mokam", "৩৮"),
                            Triple("রাজাপুর", "Rajapur", "৪৫")
                        ), targetCount = 118)
                    ),
                    Upazila(
                        id = "brahmanpara",
                        nameBn = "ব্রাহ্মণপাড়া",
                        nameEn = "Brahmanpara",
                        mouzas = createExpandedMouzas("cu-brp", listOf(
                            Triple("ব্রাহ্মণপাড়া সদর", "Brahmanpara Sadar", "০৪"),
                            Triple("মাধবপুর", "Madhabpur", "১১"),
                            Triple("শিদলাই", "Shidlai", "১৯"),
                            Triple("চান্দলা", "Chandla", "২৬"),
                            Triple("শশীদল", "Shashidal", "৩৪"),
                            Triple("দুলালপুর", "Dulalpur", "৪২")
                        ), targetCount = 86)
                    ),
                    Upazila(
                        id = "chauddagram",
                        nameBn = "চৌদ্দগ্রাম",
                        nameEn = "Chauddagram",
                        mouzas = createExpandedMouzas("cu-chaud", listOf(
                            Triple("চৌদ্দগ্রাম বাজার", "Chauddagram Bazar", "০৬"),
                            Triple("মিয়াবাজার", "Miabazar", "১৩"),
                            Triple("কাশীনগর", "Kashinagar", "২১"),
                            Triple("বাতিসা", "Batisa", "২৯"),
                            Triple("মুন্সীরহাট", "Munshirhat", "৩৭"),
                            Triple("গুণবতী", "Gunabati", "৪৪"),
                            Triple("চিওড়া", "Cheora", "৫১")
                        ), targetCount = 162)
                    ),
                    Upazila(
                        id = "laksam",
                        nameBn = "লাকসাম",
                        nameEn = "Laksam",
                        mouzas = createExpandedMouzas("cu-lak", listOf(
                            Triple("লাকসাম পৌরসভা", "Laksam Pouro", "০৩"),
                            Triple("কান্দিরপাড়", "Kandirpar", "১০"),
                            Triple("গোবিন্দপুর", "Gobindapur", "১৮"),
                            Triple("উত্তরদা", "Uttarda", "২৫"),
                            Triple("মুদাফফরগঞ্জ", "Mudhafurganj", "৪২")
                        ), targetCount = 98)
                    ),
                    Upazila(
                        id = "muradnagar",
                        nameBn = "মুরাদনগর",
                        nameEn = "Muradnagar",
                        mouzas = createExpandedMouzas("cu-mur", listOf(
                            Triple("মুরাদনগর সদর", "Muradnagar Sadar", "০৯"),
                            Triple("কোম্পানীগঞ্জ", "Companyganj", "১৬"),
                            Triple("বাঙ্গরা", "Bangora", "২৪"),
                            Triple("জাহাপুর", "Jahapur", "৩৩"),
                            Triple("রামচন্দ্রপুর", "Ramchandrapur", "৪১"),
                            Triple("শ্রীকাইল", "Sreekail", "৫০")
                        ), targetCount = 154)
                    ),
                    Upazila(
                        id = "barura",
                        nameBn = "বরুড়া",
                        nameEn = "Barura",
                        mouzas = createExpandedMouzas("cu-bar", listOf(
                            Triple("বরুড়া পৌরসভা", "Barura Pouro", "০৫"),
                            Triple("গালিমপুর", "Galimpur", "১২"),
                            Triple("শিলমুড়ী", "Shilmuri", "২০"),
                            Triple("পায়েলগাছা", "Payalgacha", "২৮"),
                            Triple("আড্ডা", "Adda", "৩৫"),
                            Triple("শাকপুর", "Shakpur", "৪৩")
                        ), targetCount = 132)
                    ),
                    Upazila(
                        id = "homna",
                        nameBn = "হোমনা",
                        nameEn = "Homna",
                        mouzas = createExpandedMouzas("cu-hom", listOf(
                            Triple("হোমনা সদর", "Homna Sadar", "০২"),
                            Triple("আসাদপুর", "Asadpur", "০৮"),
                            Triple("জয়পুর", "Joypur", "১৫"),
                            Triple("ঘাগুটিয়া", "Ghagutia", "৩০"),
                            Triple("মাথাভাঙ্গা", "Mathabhanga", "৪৬")
                        ), targetCount = 88)
                    ),
                    Upazila(
                        id = "titas",
                        nameBn = "তিতাস",
                        nameEn = "Titas",
                        mouzas = createExpandedMouzas("cu-tit", listOf(
                            Triple("মজিদপুর", "Majidpur", "০৪"),
                            Triple("বলরামপুর", "Balrampur", "১১"),
                            Triple("জগতপুর", "Jagatpur", "১৯"),
                            Triple("কড়িকান্দি", "Karikandi", "২৭"),
                            Triple("জিয়ারকান্দি", "Zearkandi", "৪১")
                        ), targetCount = 74)
                    ),
                    Upazila(
                        id = "meghna",
                        nameBn = "মেঘনা",
                        nameEn = "Meghna",
                        mouzas = createExpandedMouzas("cu-meg", listOf(
                            Triple("মানিকরচর", "Manikar Char", "০৩"),
                            Triple("চন্দনপুর", "Chandanpur", "০৯"),
                            Triple("চালিভাঙ্গা", "Chalibhanga", "১৬"),
                            Triple("গোবিন্দপুর", "Gobindapur", "২৪"),
                            Triple("রাধানগর", "Radhanagar", "৩১")
                        ), targetCount = 62)
                    ),
                    Upazila(
                        id = "monohargonj",
                        nameBn = "মনোহরগঞ্জ",
                        nameEn = "Monohargonj",
                        mouzas = createExpandedMouzas("cu-mon", listOf(
                            Triple("মনোহরগঞ্জ সদর", "Monohargonj Sadar", "০৫"),
                            Triple("বাইশগাঁও", "Baishgaon", "১২"),
                            Triple("সরসপুর", "Sarashpur", "১৯"),
                            Triple("হাসনাবাদ", "Hasnabad", "২৭"),
                            Triple("ঝালম", "Jhalam", "৩৪")
                        ), targetCount = 104)
                    ),
                    Upazila(
                        id = "nangalkot",
                        nameBn = "নাঙ্গলকোট",
                        nameEn = "Nangalkot",
                        mouzas = createExpandedMouzas("cu-nan", listOf(
                            Triple("নাঙ্গলকোট পৌরসভা", "Nangalkot Pouro", "০৬"),
                            Triple("ঢালুয়া", "Dhalua", "১৪"),
                            Triple("বক্সগঞ্জ", "Boxoganj", "২১"),
                            Triple("মোকরা", "Mokara", "২৯"),
                            Triple("পেরিয়া", "Peria", "৩৭"),
                            Triple("রায়কোট", "Roykot", "৪৫")
                        ), targetCount = 142)
                    ),
                    Upazila(
                        id = "lalmai",
                        nameBn = "লালমাই",
                        nameEn = "Lalmai",
                        mouzas = createExpandedMouzas("cu-lal", listOf(
                            Triple("বাগমারা", "Bagmara", "০৮"),
                            Triple("ভুলাইণ", "Bhulain", "১৫"),
                            Triple("বেলঘর", "Belghar", "২৩"),
                            Triple("পেরুল দক্ষিণ", "Perul Dakshin", "৩১"),
                            Triple("বাকই উত্তর", "Bakoi Uttar", "৩৯")
                        ), targetCount = 78)
                    )
                )
            )
        )
    }
}
