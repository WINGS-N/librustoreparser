package wings.v.rustore.parser;

import java.util.ArrayList;
import java.util.List;

public final class RuStoreDefaults {
    public static final String DEFAULT_BASE_URL = "https://www.rustore.ru";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36";

    private static final List<String> DEFAULT_SUSPICIOUS_SEED_DEVELOPER_IDS = List.of(
            "25cb8eb5",
            "df91b73f",
            "3x3cjaw2",
            "xashx42f",
            "9607cd14",
            "mbtttt4v",
            "78c1c8ed",
            "47fc945b",
            "d32f20ae",
            "a5ce9c44",
            "bc26d940",
            "d91661ae",
            "77a2530c",
            "63d0ea9f",
            "589be38f",
            "100677d1",
            "a65961e0",
            "c184ae7c",
            "8f8f8250",
            "taavi9x0",
            "55a9d1b1",
            "2c6a10fd",
            "3smwwz3s",
            "aabea2de",
            "f384b98f",
            "d57e19b3",
            "35702095",
            "75883313",
            "68416ebf",
            "8ccdebf0",
            "63ea48cb",
            "e1d653c2",
            "a15900d5",
            "18c4231b",
            "0f7f523b",
            "7a17e879",
            "d2ef9d4c",
            "b46d9f81",
            "cb96cc4c",
            "041fcfd1",
            "b2730967",
            "cc01aebe",
            "fc3ecc0d",
            "31f09962",
            "80e24324",
            "a1243e95",
            "9ddbb7d9",
            "f0f02d9b",
            "lm5fnpob",
            "2b19ctbj",
            "9ef76af6",
            "457798ab",
            "a1407e2c",
            "94c9fae3",
            "25bd713d",
            "0e6451b5",
            "30001e4c",
            "07c20ddf",
            "692ed825",
            "2fc01da6",
            "8c1fc8f1",
            "3a7b8579",
            "3022b99c",
            "4ba404e1",
            "097f5ee0",
            "e955a92e",
            "5ff74c45",
            "d7ae51b3",
            "pkktcyox",
            "f0f99c7d",
            "5e533ced",
            "3ac7c3c3",
            "42d293c2"
    );

    private static final List<String> DEFAULT_SUSPICIOUS_DIRECT_PACKAGES = List.of(
            "ru.plazius.cofix"
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
