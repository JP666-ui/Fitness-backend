
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `fitness_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `fitness_db`;
DROP TABLE IF EXISTS `common_food`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `common_food` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `food_name` varchar(100) NOT NULL COMMENT '食物名称',
  `calorie_per_100g` int(11) NOT NULL COMMENT '每100克热量(kcal)',
  `category` varchar(50) DEFAULT NULL COMMENT '分类：主食/肉类/蔬菜/水果/饮品/零食',
  `protein` decimal(5,2) DEFAULT NULL COMMENT '蛋白质(g/100g)',
  `fat` decimal(5,2) DEFAULT NULL COMMENT '脂肪(g/100g)',
  `carbohydrate` decimal(5,2) DEFAULT NULL COMMENT '碳水化合物(g/100g)',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_food_name` (`food_name`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COMMENT='常见食物热量表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `common_food` WRITE;
/*!40000 ALTER TABLE `common_food` DISABLE KEYS */;
INSERT INTO `common_food` VALUES (1,'米饭',116,'主食',2.60,0.30,25.90,NULL,'2026-10-13 15:46:45'),(2,'馒头',223,'主食',7.00,1.10,47.00,NULL,'2026-10-13 15:46:45'),(3,'面条',137,'主食',4.50,0.50,28.50,NULL,'2026-10-13 15:46:45'),(4,'鸡蛋',155,'肉类',13.30,8.80,2.80,NULL,'2026-10-13 15:46:45'),(5,'鸡胸肉',165,'肉类',31.00,3.60,0.00,NULL,'2026-10-13 15:46:45'),(6,'牛肉',250,'肉类',26.00,15.00,0.00,NULL,'2026-10-13 15:46:45'),(7,'猪肉',263,'肉类',14.60,20.80,0.00,NULL,'2026-10-13 15:46:45'),(8,'苹果',52,'水果',0.30,0.20,13.80,NULL,'2026-10-13 15:46:45'),(9,'香蕉',89,'水果',1.10,0.30,22.80,NULL,'2026-10-13 15:46:45'),(10,'牛奶',54,'饮品',3.00,3.20,3.40,NULL,'2026-10-13 15:46:45'),(11,'豆浆',31,'饮品',3.00,1.60,1.80,NULL,'2026-10-13 15:46:45'),(12,'面包',265,'主食',8.50,3.50,50.00,NULL,'2026-10-13 15:46:45'),(13,'燕麦',389,'主食',16.90,6.90,66.90,NULL,'2026-10-13 15:46:45'),(14,'玉米',86,'主食',3.20,1.20,19.00,NULL,'2026-10-13 15:46:45'),(15,'红薯',86,'主食',1.60,0.10,20.10,NULL,'2026-10-13 15:46:45'),(16,'土豆',77,'蔬菜',2.00,0.10,17.80,NULL,'2026-10-13 15:46:45'),(17,'西红柿',18,'蔬菜',0.90,0.20,3.90,NULL,'2026-10-13 15:46:45'),(18,'黄瓜',15,'蔬菜',0.70,0.10,2.90,NULL,'2026-10-13 15:46:45'),(19,'胡萝卜',41,'蔬菜',0.90,0.20,9.60,NULL,'2026-10-13 15:46:45'),(20,'西兰花',34,'蔬菜',2.80,0.40,7.00,NULL,'2026-10-13 15:46:45'),(21,'菠菜',23,'蔬菜',2.90,0.40,3.60,NULL,'2026-10-13 15:46:45'),(22,'豆腐',76,'蔬菜',8.10,4.80,1.90,NULL,'2026-10-13 15:46:45'),(23,'酸奶',72,'饮品',3.20,2.70,9.30,NULL,'2026-10-13 15:46:45'),(24,'坚果',607,'零食',20.00,54.00,20.00,NULL,'2026-10-13 15:46:45'),(25,'巧克力',546,'零食',4.90,31.30,61.00,NULL,'2026-10-13 15:46:45'),(26,'可乐',42,'饮品',0.00,0.00,10.60,NULL,'2026-10-13 15:46:45'),(27,'啤酒',43,'饮品',0.30,0.00,3.60,NULL,'2026-10-13 15:46:45'),(28,'白酒',295,'饮品',0.00,0.00,0.00,NULL,'2026-10-13 15:46:45');
/*!40000 ALTER TABLE `common_food` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `exercise_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `exercise_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `exercise_name` varchar(100) NOT NULL COMMENT '运动名称',
  `exercise_type` varchar(50) DEFAULT NULL COMMENT '运动类型',
  `duration` int(11) NOT NULL COMMENT '运动时长(分钟)',
  `calorie_burn` int(11) DEFAULT NULL COMMENT '消耗热量(kcal)',
  `record_date` date NOT NULL COMMENT '记录日期',
  `record_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_user_date` (`user_id`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exercise_record` WRITE;
/*!40000 ALTER TABLE `exercise_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `exercise_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `exercise_tutorial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `exercise_tutorial` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '视频标题',
  `description` varchar(500) DEFAULT NULL COMMENT '视频描述',
  `category` varchar(50) NOT NULL COMMENT '分类：力量训练/有氧运动/瑜伽/核心训练等',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `video_url` varchar(500) DEFAULT NULL COMMENT '视频URL',
  `steps` text COMMENT '动作要领（JSON格式）',
  `difficulty` int(11) DEFAULT '1' COMMENT '难度等级：1-初级 2-中级 3-高级',
  `calorie_burn` int(11) DEFAULT NULL COMMENT '预计消耗热量（千卡/小时）',
  `duration` int(11) DEFAULT NULL COMMENT '推荐时长（分钟）',
  `target_muscles` varchar(200) DEFAULT NULL COMMENT '目标肌群',
  `view_count` int(11) DEFAULT '0' COMMENT '观看次数',
  `status` int(11) DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_view_count` (`view_count`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='运动教学视频表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exercise_tutorial` WRITE;
/*!40000 ALTER TABLE `exercise_tutorial` DISABLE KEYS */;
INSERT INTO `exercise_tutorial` VALUES (1,'标准俯卧撑教学','锻炼胸肌、三角肌和肱三头肌的基础动作','力量训练','https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400','https://example.com/video1.mp4','[\"双手撑地，与肩同宽\", \"身体保持一条直线\", \"屈肘下放身体至胸部接近地面\", \"推起身体回到起始位置\", \"重复动作，保持呼吸均匀\"]',1,300,15,'胸肌、三角肌、肱三头肌',150,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(2,'深蹲教学','锻炼大腿和臀部肌肉的经典动作','力量训练','https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=400','https://example.com/video2.mp4','[\"双脚与肩同宽站立\", \"背部挺直，核心收紧\", \"臀部向后坐，屈膝下蹲\", \"大腿与地面平行\", \"起身回到站立位置\"]',1,350,20,'大腿、臀部',200,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(3,'平板支撑教学','锻炼核心肌群的有效动作','核心训练','https://images.unsplash.com/photo-1566241440091-ec10de8db2e1?w=400','https://example.com/video3.mp4','[\"俯卧，双肘撑地\", \"身体保持一条直线\", \"收紧腹部和臀部\", \"保持呼吸均匀\", \"坚持30-60秒\"]',1,200,5,'核心肌群',180,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(4,'跑步姿势教学','正确的跑步姿势和呼吸方法','有氧运动','https://images.unsplash.com/photo-1552674605-469523170242?w=400','https://example.com/video4.mp4','[\"保持身体直立，目视前方\", \"手臂自然摆动\", \"脚掌中部着地\", \"保持均匀呼吸\", \"循序渐进增加距离\"]',1,600,30,'全身',250,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(5,'瑜伽基础动作','放松身心，提高柔韧性','瑜伽','https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400','https://example.com/video5.mp4','[\"选择安静的环境\", \"穿着舒适的服装\", \"跟随呼吸节奏\", \"不要勉强做动作\", \"保持专注和放松\"]',1,150,45,'全身',120,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(6,'哑铃训练基础','上肢力量训练入门','力量训练','https://images.unsplash.com/photo-1581009146145-b5ef050c149a?w=400','https://example.com/video6.mp4','[\"选择合适重量的哑铃\", \"保持正确姿势\", \"控制动作速度\", \"注意呼吸配合\", \"循序渐进增加重量\"]',2,400,25,'上肢、肩部',90,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(7,'仰卧起坐教学','锻炼腹部肌肉的基础动作','核心训练','https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400','https://example.com/video7.mp4','[\"仰卧，双膝弯曲\", \"双手抱头或放耳旁\", \"收腹起坐\", \"缓慢躺下\", \"重复动作\"]',1,250,15,'腹肌',160,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(8,'波比跳教学','高强度全身燃脂动作','有氧运动','https://images.unsplash.com/photo-1599058945522-28d584b6f0ff?w=400','https://example.com/video8.mp4','[\"站立开始\", \"下蹲双手撑地\", \"双腿后跳呈俯卧撑姿势\", \"做一个俯卧撑\", \"双腿跳回，起身跳跃\"]',3,800,10,'全身',80,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(9,'拉伸放松教学','运动后的拉伸放松','瑜伽','https://images.unsplash.com/photo-1518611012118-696072aa579a?w=400','https://example.com/video9.mp4','[\"选择舒适姿势\", \"缓慢伸展肌肉\", \"保持15-30秒\", \"深呼吸放松\", \"逐个部位进行\"]',1,100,15,'全身',140,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(10,'引体向上教学','锻炼背部和手臂力量','力量训练','https://images.unsplash.com/photo-1598971639058-211a74a96aa7?w=400','https://example.com/video10.mp4','[\"正握单杠，握距略宽于肩\", \"悬垂，身体挺直\", \"拉起至下巴过杠\", \"缓慢下放\", \"重复动作\"]',3,450,15,'背部、肱二头肌',70,1,'2026-10-13 15:46:45','2026-10-13 15:46:45');
/*!40000 ALTER TABLE `exercise_tutorial` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `fitness_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fitness_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `plan_name` varchar(100) NOT NULL COMMENT '计划名称',
  `plan_type` varchar(50) DEFAULT NULL COMMENT '计划类型：减脂/增肌/塑形/维持',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重',
  `target_calorie` int(11) DEFAULT NULL COMMENT '目标热量',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` int(11) DEFAULT '1' COMMENT '状态：0-停用 1-进行中 2-已完成',
  `remark` text COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `fitness_plan` WRITE;
/*!40000 ALTER TABLE `fitness_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `fitness_plan` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `food_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `food_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `food_name` varchar(100) NOT NULL COMMENT '食物名称',
  `weight` int(11) DEFAULT NULL COMMENT '重量(g)',
  `calorie` int(11) NOT NULL COMMENT '热量(kcal)',
  `record_date` date NOT NULL COMMENT '记录日期',
  `record_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `image_url` varchar(500) DEFAULT NULL COMMENT '食物图片URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_user_date` (`user_id`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `food_record` WRITE;
/*!40000 ALTER TABLE `food_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `food_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `nutrition_consult`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nutrition_consult` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `question` text NOT NULL COMMENT '咨询问题',
  `answer` text COMMENT 'AI回答',
  `consult_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '咨询时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_consult_time` (`consult_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营养咨询记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `nutrition_consult` WRITE;
/*!40000 ALTER TABLE `nutrition_consult` DISABLE KEYS */;
/*!40000 ALTER TABLE `nutrition_consult` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（MD5加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `gender` int(11) DEFAULT '0' COMMENT '性别：0-保密 1-男 2-女',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重(kg)',
  `bmi` decimal(4,2) DEFAULT NULL COMMENT 'BMI指数',
  `daily_calorie_goal` int(11) DEFAULT '2000' COMMENT '每日热量目标(kcal)',
  `status` int(11) DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','e10adc3949ba59abbe56e057f20f883e','管理员','admin@fitness.com',NULL,NULL,1,30,175.00,70.00,NULL,NULL,2500,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(2,'test','e10adc3949ba59abbe56e057f20f883e','jp','','',NULL,0,22,183.00,85.00,NULL,NULL,1800,1,'2026-10-13 15:46:45','2026-10-13 15:46:45'),(3,'jp666','aef1e8b3118a132e58b7cd2dd0e1b275','jp',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,2000,1,'2026-10-13 15:48:04','2026-10-13 15:48:04');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

