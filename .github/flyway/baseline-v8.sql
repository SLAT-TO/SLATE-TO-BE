
-- CI 전용 V8 기준 스키마.
-- 운영 DB에 직접 실행하지 않는다.
-- 근거 커밋: bebbfb77599075e8e4d6ad7b96c11cc72789b546
-- V009 최초 추가: 75cd92a67db8d05f3e5358e481eebb51fdfb2ce8
-- MySQL 8.4.9에서 V009, V010, V012, V013 적용 및 현재 Hibernate validate를 검증했다.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_log` (
  `actor_guest_id` bigint DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `group_key` varchar(255) DEFAULT NULL,
  `actor_type` enum('CLIENT_REVIEWER','SYSTEM','USER') NOT NULL,
  `type` enum('DIRECTOR','PD','CINEMATOGRAPHER','EDITOR','ART','SOUND','WRITER','LIGHTING','ACTOR','ETC') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activity_log_project_created_id` (`project_id`,`created_at`,`id`),
  CONSTRAINT `FK273xmgexdxj7yu432gth17luq` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `status` bit(1) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `end_time` bigint DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_time` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `video_id` bigint NOT NULL,
  `content` text NOT NULL,
  `feedback_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj6cjgvuqyyls58wg5gixwgyi3` (`guest_id`),
  KEY `FKpwwmhguqianghvi1wohmtsm8l` (`user_id`),
  KEY `FKejspu7a0b6480ufbmbyb3k1g1` (`video_id`),
  CONSTRAINT `FKejspu7a0b6480ufbmbyb3k1g1` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`),
  CONSTRAINT `FKj6cjgvuqyyls58wg5gixwgyi3` FOREIGN KEY (`guest_id`) REFERENCES `guest` (`id`),
  CONSTRAINT `FKpwwmhguqianghvi1wohmtsm8l` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback_detail` (
  `status` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `feedback_id` bigint NOT NULL,
  `guest_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `FK9kdj80tqen9e7nbnj13vnk661` (`feedback_id`),
  KEY `FKoqc1x02aijxj6qc6i2e1hixjp` (`guest_id`),
  KEY `FKf7wdlxlj3jn437njwp6hni086` (`user_id`),
  CONSTRAINT `FK9kdj80tqen9e7nbnj13vnk661` FOREIGN KEY (`feedback_id`) REFERENCES `feedback` (`id`),
  CONSTRAINT `FKf7wdlxlj3jn437njwp6hni086` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoqc1x02aijxj6qc6i2e1hixjp` FOREIGN KEY (`guest_id`) REFERENCES `guest` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guest` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `share_link_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4l013yp9w51s1lo4fyp08pxfl` (`share_link_id`),
  CONSTRAINT `FK4l013yp9w51s1lo4fyp08pxfl` FOREIGN KEY (`share_link_id`) REFERENCES `share_link` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inquiry` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `content` text NOT NULL,
  `title` varchar(255) NOT NULL,
  `status` enum('ANSWERED','CLOSED','PENDING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKray80kmwpjpjb91ime7ogijjr` (`user_id`),
  CONSTRAINT `FKray80kmwpjpjb91ime7ogijjr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recruitment_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `region_name` enum('CHUNGCHEONGBUK','CHUNGCHEONGNAM','GANGWON','GYEONGGI','GYEONGSANGBUK','GYEONGSANGNAM','JEJU','JEOLLABUK','JEOLLANAM','NATIONWIDE','SEOUL') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKr7tbyfn3o8saha97ydx2etutj` (`recruitment_id`),
  KEY `FK55by463ivfy1u1qfylnjswyje` (`user_id`),
  CONSTRAINT `FK55by463ivfy1u1qfylnjswyje` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr7tbyfn3o8saha97ydx2etutj` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `group_count` int NOT NULL,
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` enum('DEADLINE_REMINDER','PROJECT_INVITED','RECRUITMENT_APPLIED','SCHEDULE_ASSIGNED','VIDEO_FEEDBACK_COMMENTED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi35sfx0x08fonfxf2l8cp2xcp` (`project_id`),
  KEY `idx_notification_group_lookup` (`user_id`,`type`,`target_type`,`target_id`,`is_read`),
  CONSTRAINT `FKi35sfx0x08fonfxf2l8cp2xcp` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_setting` (
  `email_all_enabled` bit(1) NOT NULL,
  `email_assigned` bit(1) NOT NULL,
  `email_deadline_reminder` bit(1) NOT NULL,
  `email_missed_summary` bit(1) NOT NULL,
  `email_new_applicant` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmk226jk5f6j26wg7moshwhdx8` (`user_id`),
  CONSTRAINT `FKmk226jk5f6j26wg7moshwhdx8` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_user_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `client_name` varchar(255) DEFAULT NULL,
  `description` text NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `kind` enum('EXTERNAL','PERSONAL') DEFAULT NULL,
  `length_type` enum('LONG_FORM','SHORT_FORM') DEFAULT NULL,
  `status` enum('COMPLETED','EDITING','PREPARING','REVIEWING') NOT NULL,
  `type` enum('AD_BRAND','CORPORATE_PROMO','DOCUMENTARY','ETC','FILM_DRAMA','MUSIC_VIDEO','WEDDING_EVENT','YOUTUBE_CONTENT') NOT NULL,
  `custom_type_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbae3bnwveolm0lpl6njt46i6q` (`owner_user_id`),
  CONSTRAINT `FKbae3bnwveolm0lpl6njt46i6q` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_file` (
  `is_final` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pinned_at` datetime(6) DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `uploader_id` bigint NOT NULL,
  `content_type` varchar(150) NOT NULL,
  `storage_key` varchar(500) NOT NULL,
  `description` text,
  `file_name` varchar(255) NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `is_pinned` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8lwlt3x7l0bijg1lg36s6ww3s` (`project_id`),
  KEY `FK1qau8susj77xuahw4k2ev7rdp` (`uploader_id`),
  CONSTRAINT `FK1qau8susj77xuahw4k2ev7rdp` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8lwlt3x7l0bijg1lg36s6ww3s` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_invitation` (
  `accepted_at` datetime(6) DEFAULT NULL,
  `accepter_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inviter_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `token_hash` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7gvojt3pdpnn6m65svcb1ardo` (`token_hash`),
  KEY `FKfk1a31vhiqo9067v9laxy5lc3` (`accepter_id`),
  KEY `FKjc52cawe73w5rqva25hyj5iy7` (`inviter_id`),
  KEY `FKcf02s7vmm5na9rk47y8hoh8vq` (`project_id`),
  CONSTRAINT `FKcf02s7vmm5na9rk47y8hoh8vq` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `FKfk1a31vhiqo9067v9laxy5lc3` FOREIGN KEY (`accepter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjc52cawe73w5rqva25hyj5iy7` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) DEFAULT NULL,
  `left_at` datetime(6) DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `permission` enum('ADMIN','MEMBER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK103dwxad12nbaxtmnwus4eft2` (`project_id`),
  KEY `FKmep5284pv47j2o523l14wyx4g` (`user_id`),
  CONSTRAINT `FK103dwxad12nbaxtmnwus4eft2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `FKmep5284pv47j2o523l14wyx4g` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_notice` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `writer_id` bigint NOT NULL,
  `content` text,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwwuu7vrctp138do1vv3uoul0` (`project_id`),
  KEY `FKsvd79yvpipvcwbudbgeqvwwrd` (`writer_id`),
  CONSTRAINT `FKqwwuu7vrctp138do1vv3uoul0` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `FKsvd79yvpipvcwbudbgeqvwwrd` FOREIGN KEY (`writer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_notice_read` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notice_id` bigint NOT NULL,
  `read_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_notice_read_notice_user` (`notice_id`,`user_id`),
  KEY `FKc2y0e3q1g2xd3urf56bf1336c` (`user_id`),
  CONSTRAINT `FKc2y0e3q1g2xd3urf56bf1336c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjep6dy2jsxsyw8yctukjkgtnx` FOREIGN KEY (`notice_id`) REFERENCES `project_notice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pin` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pinned_at` datetime(6) NOT NULL,
  `project_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_pin_user_project` (`user_id`,`project_id`),
  KEY `FKpn11odt7nxhuwavmnubtwe8hu` (`project_id`),
  CONSTRAINT `FKdmomxj3qrlurxpgp1wpy25fqq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpn11odt7nxhuwavmnubtwe8hu` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_member_id` bigint NOT NULL,
  `role_name` enum('ACTOR','ART','CINEMATOGRAPHER','DIRECTOR','EDITOR','ETC','LIGHTING','PD','SOUND','WRITER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpgyck3jehcwr9ak1ob9u2pkgi` (`project_member_id`),
  CONSTRAINT `FKpgyck3jehcwr9ak1ob9u2pkgi` FOREIGN KEY (`project_member_id`) REFERENCES `project_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recruitment` (
  `closed_manually` bit(1) NOT NULL,
  `deadline` date DEFAULT NULL,
  `view_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `writer_id` bigint NOT NULL,
  `contact` varchar(255) DEFAULT NULL,
  `description` text,
  `pay` varchar(255) DEFAULT NULL,
  `shooting_period` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `category` enum('AD_BRAND','CORPORATE_PROMO','DOCUMENTARY','ETC','FILM_DRAMA','MUSIC_VIDEO','WEDDING_EVENT','YOUTUBE_CONTENT') DEFAULT NULL,
  `length_type` enum('LONG_FORM','SHORT_FORM') DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `recruit_part` varchar(255) DEFAULT NULL,
  `status` enum('CLOSED','RECRUITING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4c712dyyel0607c4aoeyperdp` (`writer_id`),
  KEY `idx_recruitment_open` (`deleted_at`,`closed_manually`,`deadline`),
  CONSTRAINT `FK4c712dyyel0607c4aoeyperdp` FOREIGN KEY (`writer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recruitment_application` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recruitment_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `reference_link` varchar(500) DEFAULT NULL,
  `message` text,
  `status` varchar(50) NOT NULL DEFAULT 'PENDING',
  `active_user_id` bigint GENERATED ALWAYS AS (if((`deleted_at` is null),`user_id`,NULL)) VIRTUAL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_recruitment_application_active` (`recruitment_id`,`active_user_id`),
  KEY `FKjx4k46nvo18n8av6a1dggy7xl` (`user_id`),
  CONSTRAINT `FKialcjvc9dd1nxmw7brapo80hw` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`),
  CONSTRAINT `FKjx4k46nvo18n8av6a1dggy7xl` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recruitment_bookmark` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recruitment_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_recruitment_bookmark_user_recruitment` (`user_id`,`recruitment_id`),
  KEY `FKus116m6r6wmkq88su4fj1ox7` (`recruitment_id`),
  CONSTRAINT `FKofxcyy1exo943da4ywncx5pmq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKus116m6r6wmkq88su4fj1ox7` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_token` (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token` varchar(512) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr4k4edos30bx9neoq81mdvwph` (`token`),
  KEY `idx_refresh_token_user_id` (`user_id`),
  CONSTRAINT `FKjtx87i0jvq2svedphegvdwcuy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `end_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint DEFAULT NULL,
  `start_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `writer_id` bigint NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `public_memo` text,
  `title` varchar(255) NOT NULL,
  `schedule_scope` enum('PERSONAL','PROJECT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK86b2w5apjrmn6d17rdn475dm5` (`project_id`),
  KEY `FKqurdrl0gi7efjsb66ib4cc6xb` (`writer_id`),
  CONSTRAINT `FK86b2w5apjrmn6d17rdn475dm5` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `FKqurdrl0gi7efjsb66ib4cc6xb` FOREIGN KEY (`writer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_participant` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsjtoy17dxy10c0ee12tdkhw2w` (`schedule_id`),
  KEY `FK5g48lsj6508kw6es9sae8ujcs` (`user_id`),
  CONSTRAINT `FK5g48lsj6508kw6es9sae8ujcs` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsjtoy17dxy10c0ee12tdkhw2w` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_private_memo` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `FK1olysosityco5q0nm9cgluppm` (`schedule_id`),
  KEY `FK6m2jpohedub0hw11ud78tv342` (`user_id`),
  CONSTRAINT `FK1olysosityco5q0nm9cgluppm` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`),
  CONSTRAINT `FK6m2jpohedub0hw11ud78tv342` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `share_link` (
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_id` bigint NOT NULL,
  `token` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhb6cwnyq94nbqhytvf4fxgqk9` (`token`),
  KEY `FKho2bh2ntuoln19dgyjap6bxm3` (`video_id`),
  CONSTRAINT `FKho2bh2ntuoln19dgyjap6bxm3` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category_name` enum('AD_BRAND','CORPORATE_PROMO','DOCUMENTARY','ETC','FILM_DRAMA','MUSIC_VIDEO','WEDDING_EVENT','YOUTUBE_CONTENT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_category_user_category_name` (`user_id`,`category_name`),
  CONSTRAINT `FKh1fip9lpe4alrpurxqfmpftvl` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_portfolio` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `custom_type_name` varchar(100) DEFAULT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `youtube_url` varchar(500) DEFAULT NULL,
  `client_name` varchar(255) DEFAULT NULL,
  `comment` text,
  `description` text,
  `title` varchar(255) NOT NULL,
  `kind` enum('EXTERNAL','PERSONAL') NOT NULL,
  `type` enum('AD_BRAND','CORPORATE_PROMO','DOCUMENTARY','ETC','FILM_DRAMA','MUSIC_VIDEO','WEDDING_EVENT','YOUTUBE_CONTENT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKh5u8nxj03iyp4li4kaupim7by` (`user_id`),
  CONSTRAINT `FKh5u8nxj03iyp4li4kaupim7by` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_portfolio_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `portfolio_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `category_name` enum('AD_BRAND','CORPORATE_PROMO','DOCUMENTARY','ETC','FILM_DRAMA','MUSIC_VIDEO','WEDDING_EVENT','YOUTUBE_CONTENT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhteie2qsc67bj3fdmn497ipi0` (`portfolio_id`),
  KEY `FK22kajcchfm79otqejn213oi8c` (`user_id`),
  CONSTRAINT `FK22kajcchfm79otqejn213oi8c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhteie2qsc67bj3fdmn497ipi0` FOREIGN KEY (`portfolio_id`) REFERENCES `user_portfolio` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_portfolio_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `portfolio_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_name` enum('ACTOR','ART','CINEMATOGRAPHER','DIRECTOR','EDITOR','ETC','LIGHTING','PD','SOUND','WRITER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsen08i8ngbho8fb66ym5eqimi` (`portfolio_id`),
  KEY `FKeq961u14bktyn1c0hg498sqg9` (`user_id`),
  CONSTRAINT `FKeq961u14bktyn1c0hg498sqg9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsen08i8ngbho8fb66ym5eqimi` FOREIGN KEY (`portfolio_id`) REFERENCES `user_portfolio` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_name` enum('ACTOR','ART','CINEMATOGRAPHER','DIRECTOR','EDITOR','ETC','LIGHTING','PD','SOUND','WRITER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_role_user_role_name` (`user_id`,`role_name`),
  CONSTRAINT `FKj345gk1bovqvfame88rcx7yyx` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `onboarding_completed` bit(1) NOT NULL,
  `term` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(100) NOT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  `bio` text,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `social_id` varchar(255) DEFAULT NULL,
  `social_type` enum('EMAIL','GOOGLE','KAKAO') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video` (
  `duration_seconds` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `youtube_video_id` varchar(100) NOT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `youtube_url` varchar(500) NOT NULL,
  `memo` text,
  `title` varchar(255) NOT NULL,
  `progress_status` enum('DONE','IN_PROGRESS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_video_project_youtube_video_id` (`project_id`,`youtube_video_id`),
  CONSTRAINT `FKnvxs28skf84eiqx1bqsltsdmo` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_bookmark` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `video_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_video_bookmark_video_user` (`video_id`,`user_id`),
  KEY `FKju8x5etmoh1v5o061siukllrd` (`user_id`),
  CONSTRAINT `FKjqx3o5s07mw82wvcdursgq1mj` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`),
  CONSTRAINT `FKju8x5etmoh1v5o061siukllrd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_reference_file` (
  `added_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_file_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK60tf1rko17jfg72p6frmw1eey` (`added_by`),
  KEY `FK8ms307l5c05qxva6evqmup6r8` (`project_file_id`),
  KEY `FKmv871ptaholpgyt22fp0n3s2p` (`video_id`),
  CONSTRAINT `FK60tf1rko17jfg72p6frmw1eey` FOREIGN KEY (`added_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8ms307l5c05qxva6evqmup6r8` FOREIGN KEY (`project_file_id`) REFERENCES `project_file` (`id`),
  CONSTRAINT `FKmv871ptaholpgyt22fp0n3s2p` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
