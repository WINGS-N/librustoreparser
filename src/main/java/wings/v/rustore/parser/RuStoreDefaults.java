package wings.v.rustore.parser;

import java.util.ArrayList;
import java.util.List;

public final class RuStoreDefaults {
    public static final String DEFAULT_BASE_URL = "https://www.rustore.ru";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36";

    private static final List<String> DEFAULT_SUSPICIOUS_SEED_DEVELOPER_IDS = List.of(
            "25cb8eb5", // ООО "Иви.ру"
            "df91b73f", // VK
            "3x3cjaw2", // ALIEXPRESS CIS HOLDING PTE. LTD.
            "xashx42f", // MAX
            "9607cd14", // ООО «ИТ ИКС 5 Технологии»
            "mbtttt4v", // X5 Digital
            "78c1c8ed", // Перекрёсток
            "47fc945b", // Рестрим Медиа
            "d32f20ae", // ООО "ДубльГИС"
            "a5ce9c44", // ООО «Новые Туристические Технологии»
            "bc26d940", // ООО «Айриэлтор»
            "d91661ae", // ООО "Интернет Решения"
            "77a2530c", // Lamoda
            "63d0ea9f", // Лента
            "589be38f", // ООО "КЕХ еКоммерц"
            "100677d1", // ООО "ЛитРес"
            "a65961e0", // ПАО Сбербанк
            "c184ae7c", // ООО "ЯНДЕКС"
            "dpxjd5sd", // АО "Даль"
            "8f8f8250", // РАМБЛЕР ИНТЕРНЕТ ХОЛДИНГ
            "taavi9x0", // ООО "ИНФОМЕТЕОС"
            "55a9d1b1", // АО "Вкусвилл"
            "2c6a10fd", // ООО "Хэдхантер"
            "3smwwz3s", // Лемана ПРО
            "aabea2de", // ПАО "Магнит"
            "f384b98f", // ООО "РУФОРМ"
            "d57e19b3", // ООО "РИТМ Медиа"
            "35702095", // ООО "НСТ"
            "75883313", // АО "Телекомпания НТВ"
            "68416ebf", // ООО «НТВ-ПЛЮС»
            "8ccdebf0", // ООО "Фонбет ТВ"
            "63ea48cb", // ООО "Премьер"
            "e1d653c2", // ООО "ГПМ Радио"
            "a15900d5", // ООО "Оператор Газпром ИД"
            "18c4231b", // Okko LLC
            "0f7f523b", // НАО «Национальная спутниковая компания»
            "7a17e879", // ООО "Вайлдберриз"
            "d2ef9d4c", // ООО «ВсеИнструменты.ру»
            "b46d9f81", // Минцифры РФ
            "cb96cc4c", // АО «НСПК»
            "041fcfd1", // ОАО "РЖД"
            "b2730967", // ИАЦ в сфере здравоохранения города Москвы
            "cc01aebe", // ООО "Умное пространство"
            "fc3ecc0d", // АО «ТБанк»
            "31f09962", // АО «Россельхозбанк»
            "80e24324", // Банк ВТБ (ПАО)
            "a1243e95", // АО "Альфа-Банк"
            "9ddbb7d9", // Газпромбанк
            "e3cc3e91", // ГПН-Региональные продажи
            "4d21a418", // Публичное акционерное общество «Совкомбанк»
            "f0f02d9b", // ПАО "Банк ПСБ"
            "lm5fnpob", // ПСБ Маркет
            "2b19ctbj", // CDEK Digital
            "2207f9ae", // Сайтсофт
            "9ef76af6", // ПАО «Ростелеком»
            "457798ab", // Пикабу
            "a1407e2c", // Почта России
            "94c9fae3", // ПАО МТС
            "25bd713d", // МегаФон
            "0e6451b5", // ПАО "ВымпелКом"
            "30001e4c", // ООО "Скартел"
            "07c20ddf", // ЭР-Телеком Холдинг
            "692ed825", // Центр развития перспективных технологий
            "2fc01da6", // Информационный город ГКУ
            "8c1fc8f1", // Дептранс Москвы
            "3a7b8579", // ООО "НОВЫЕ ОБЛАЧНЫЕ ТЕХНОЛОГИИ"
            "3022b99c", // ООО «Т2 Мобайл»
            "wulv5o2f", // ООО "ЛУКОЙЛ-Интер-Кард"
            "4ba404e1", // Оператор информационной системы
            "097f5ee0", // ООО "еАптека"
            "e955a92e", // Семейный доктор
            "5ff74c45", // YCLIENTS
            "d7ae51b3", // АО "Лаборатория Касперского"
            "pkktcyox", // ООО "Доктор Веб"
            "f0f99c7d", // ООО «М Тех»
            "f620e826", // АО «АльфаСтрахование»
            "5e533ced", // ДНС
            "3ac7c3c3", // ПАО "Аэрофлот"
            "42d293c2" // ООО "ФЛАУВАУ"
    );

    private static final List<String> DEFAULT_SUSPICIOUS_DIRECT_PACKAGES = List.of(
            "ru.plazius.cofix",
            "ru.vk.store"
    );

    private RuStoreDefaults() {
    }

    public static List<String> getDefaultSuspiciousSeedDeveloperIds() {
        return new ArrayList<>(DEFAULT_SUSPICIOUS_SEED_DEVELOPER_IDS);
    }

    public static List<String> getDefaultSuspiciousDirectPackages() {
        return new ArrayList<>(DEFAULT_SUSPICIOUS_DIRECT_PACKAGES);
    }
}
