package com.chle.common.constant; // 根据项目包结构调整

/**
 * 应用常量定义
 */
public final class AppConstants {

    // ========== 用户服务相关常量 ==========
    public static final class User {
        public static final String LOCK_KEY_PREFIX = "login:lock:";
        public static final String CACHE_KEY_PREFIX = "user:cache:";
        public static final int MAX_FAIL_COUNT = 5;
        public static final long LOCK_DURATION_MINUTES = 10;
        public static final long CACHE_TTL_MINUTES = 30;
    }

    // ========== 计划服务相关常量 ==========
    public static final class Plan {
        public static final String STATS_KEY_PREFIX = "plan:stats:";
        public static final String STATS_TODAY_NEW = "todayNew";
        public static final String STATS_COMPLETED = "completed";
        public static final String STATS_ONGOING = "ongoing";
        public static final String STATS_EXPIRED = "expired";
        public static final String STATS_TOTAL = "total";
    }

    private AppConstants() {} // 防止实例化
}