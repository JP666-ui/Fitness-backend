-- ============================================================
-- 智能运动营养与健康评测系统 数据库初始化脚本
-- 生成方式：mysqldump（与线上库结构一致）
-- 包含表：user / food_record / exercise_record / weight_record /
--         fitness_plan / common_food / user_ai_config /
--         nutrition_consult / notice_message / health_assessment
-- 默认账号：admin/123456（管理员）  test/123456  jp666/123456（普通用户）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `fitness_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `fitness_db`;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `common_food`
--

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
) ENGINE=InnoDB AUTO_INCREMENT=152 DEFAULT CHARSET=utf8mb4 COMMENT='常见食物热量表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `common_food`
--

LOCK TABLES `common_food` WRITE;
/*!40000 ALTER TABLE `common_food` DISABLE KEYS */;
INSERT INTO `common_food` VALUES (1,'米饭',116,'主食',2.60,0.30,25.90,'蒸熟的白米饭','2026-10-14 15:12:25'),(2,'馒头',223,'主食',7.00,1.10,47.00,'普通白面馒头','2026-10-14 15:12:25'),(3,'花卷',217,'主食',6.40,1.00,45.60,'葱油花卷','2026-10-14 15:12:25'),(4,'包子（肉馅）',227,'主食',8.50,8.30,28.00,'猪肉大葱包','2026-10-14 15:12:25'),(5,'包子（素馅）',180,'主食',5.60,4.50,29.00,'青菜香菇包','2026-10-14 15:12:25'),(6,'油条',386,'主食',6.90,17.60,51.00,'油炸，热量较高','2026-10-14 15:12:25'),(7,'油饼',399,'主食',7.00,18.00,52.00,'油炸面食','2026-10-14 15:12:25'),(8,'煎饼果子',260,'主食',8.00,9.00,36.00,'含鸡蛋与薄脆','2026-10-14 15:12:25'),(9,'面条（煮）',137,'主食',4.50,0.50,28.50,'煮熟的挂面','2026-10-14 15:12:25'),(10,'炒面',219,'主食',6.50,7.00,32.00,'含油炒制','2026-10-14 15:12:25'),(11,'炒饭',186,'主食',5.00,5.50,29.00,'蛋炒饭','2026-10-14 15:12:25'),(12,'饺子（猪肉）',240,'主食',9.00,9.00,30.00,'猪肉白菜饺','2026-10-14 15:12:25'),(13,'馄饨',190,'主食',7.50,5.50,27.00,'鲜肉小馄饨','2026-10-14 15:12:25'),(14,'小米粥',46,'主食',1.40,0.70,8.40,'稀粥，热量低','2026-10-14 15:12:25'),(15,'白粥',46,'主食',1.10,0.20,9.90,'大米稀粥','2026-10-14 15:12:25'),(16,'燕麦',389,'主食',16.90,6.90,66.90,'生燕麦片','2026-10-14 15:12:25'),(17,'全麦面包',246,'主食',8.80,3.50,45.00,'全麦制品','2026-10-14 15:12:25'),(18,'面包（白）',312,'主食',8.30,5.10,58.60,'普通甜面包','2026-10-14 15:12:25'),(19,'红薯',86,'主食',1.60,0.10,20.10,'蒸红薯','2026-10-14 15:12:25'),(20,'紫薯',82,'主食',1.50,0.20,19.00,'蒸紫薯','2026-10-14 15:12:25'),(21,'土豆',77,'主食',2.00,0.10,17.80,'蒸土豆','2026-10-14 15:12:25'),(22,'玉米（鲜）',86,'主食',3.20,1.20,19.00,'煮玉米','2026-10-14 15:12:25'),(23,'山药',57,'主食',1.90,0.20,12.40,'蒸山药','2026-10-14 15:12:25'),(24,'南瓜',22,'主食',0.70,0.10,5.30,'蒸南瓜','2026-10-14 15:12:25'),(25,'粽子',195,'主食',3.50,3.00,38.00,'糯米粽','2026-10-14 15:12:25'),(26,'汤圆',311,'主食',4.40,10.00,51.00,'黑芝麻馅','2026-10-14 15:12:25'),(27,'米粉',109,'主食',1.80,0.20,25.00,'煮米粉','2026-10-14 15:12:25'),(28,'河粉',130,'主食',2.40,0.30,29.00,'炒河粉用','2026-10-14 15:12:25'),(29,'意大利面',158,'主食',5.80,0.90,31.00,'煮熟','2026-10-14 15:12:25'),(30,'年糕',154,'主食',3.30,0.60,34.00,'炒年糕','2026-10-14 15:12:25'),(31,'鸡胸肉',165,'肉类',31.00,3.60,0.00,'低脂高蛋白','2026-10-14 15:12:25'),(32,'鸡腿肉',181,'肉类',16.00,13.00,0.00,'带皮','2026-10-14 15:12:25'),(33,'鸡翅',194,'肉类',17.40,11.80,4.60,'烤鸡翅','2026-10-14 15:12:25'),(34,'炸鸡',279,'肉类',20.00,17.00,10.00,'油炸，热量高','2026-10-14 15:12:25'),(35,'牛肉（瘦）',250,'肉类',26.00,15.00,0.00,'牛里脊','2026-10-14 15:12:25'),(36,'牛肉（肥瘦）',125,'肉类',19.90,4.20,2.00,'炖牛肉','2026-10-14 15:12:25'),(37,'猪肉（瘦）',143,'肉类',20.30,6.20,1.50,'猪里脊','2026-10-14 15:12:25'),(38,'猪肉（五花）',508,'肉类',13.00,53.00,0.00,'热量极高','2026-10-14 15:12:25'),(39,'红烧肉',460,'肉类',12.00,44.00,6.00,'含糖油，热量高','2026-10-14 15:12:25'),(40,'回锅肉',370,'肉类',14.00,33.00,5.00,'川菜，油大','2026-10-14 15:12:25'),(41,'排骨',278,'肉类',18.30,20.40,1.70,'猪肋排','2026-10-14 15:12:25'),(42,'羊肉',203,'肉类',19.00,14.10,0.00,'羊肉卷','2026-10-14 15:12:25'),(43,'鸭肉',240,'肉类',15.50,19.70,0.20,'带皮烤鸭','2026-10-14 15:12:25'),(44,'鱼肉（草鱼）',113,'肉类',16.60,5.20,0.00,'淡水鱼','2026-10-14 15:12:25'),(45,'鱼肉（三文鱼）',139,'肉类',17.20,7.80,0.00,'富含 Omega-3','2026-10-14 15:12:25'),(46,'带鱼',127,'肉类',17.70,4.90,3.10,'煎带鱼','2026-10-14 15:12:25'),(47,'虾',93,'肉类',18.60,0.80,2.80,'白灼虾','2026-10-14 15:12:25'),(48,'螃蟹',103,'肉类',17.50,2.60,2.30,'蒸蟹','2026-10-14 15:12:25'),(49,'鸡蛋',155,'肉类',13.30,8.80,2.80,'全蛋','2026-10-14 15:12:25'),(50,'蛋清',60,'肉类',11.60,0.10,3.10,'纯蛋白','2026-10-14 15:12:25'),(51,'煎蛋',200,'肉类',13.00,15.00,1.50,'油煎','2026-10-14 15:12:25'),(52,'香肠',508,'肉类',24.10,40.70,11.20,'加工肉，高盐高脂','2026-10-14 15:12:25'),(53,'火腿肠',212,'肉类',14.00,10.40,15.60,'加工肉制品','2026-10-14 15:12:25'),(54,'培根',181,'肉类',22.30,9.00,2.60,'烟熏肉','2026-10-14 15:12:25'),(55,'午餐肉',229,'肉类',12.00,17.00,8.00,'罐头肉','2026-10-14 15:12:25'),(56,'鸡胸肉（水煮）',133,'肉类',29.00,1.50,0.00,'减脂首选','2026-10-14 15:12:25'),(57,'西兰花',34,'蔬菜',2.80,0.40,7.00,'水煮','2026-10-14 15:12:25'),(58,'菠菜',23,'蔬菜',2.90,0.40,3.60,'含铁','2026-10-14 15:12:25'),(59,'白菜',17,'蔬菜',1.50,0.10,3.20,'大白菜','2026-10-14 15:12:25'),(60,'生菜',15,'蔬菜',1.30,0.30,2.00,'沙拉用','2026-10-14 15:12:25'),(61,'芹菜',14,'蔬菜',1.20,0.20,3.30,'高纤维','2026-10-14 15:12:25'),(62,'黄瓜',15,'蔬菜',0.70,0.10,2.90,'水分高','2026-10-14 15:12:25'),(63,'西红柿',18,'蔬菜',0.90,0.20,3.90,'番茄','2026-10-14 15:12:25'),(64,'胡萝卜',41,'蔬菜',0.90,0.20,9.60,'富含胡萝卜素','2026-10-14 15:12:25'),(65,'茄子',21,'蔬菜',1.10,0.20,4.90,'注意吸油','2026-10-14 15:12:25'),(66,'青椒',22,'蔬菜',1.00,0.20,5.40,'甜椒','2026-10-14 15:12:25'),(67,'豆角',30,'蔬菜',2.50,0.20,5.70,'四季豆','2026-10-14 15:12:25'),(68,'蘑菇',20,'蔬菜',2.70,0.10,4.10,'香菇','2026-10-14 15:12:25'),(69,'木耳',205,'蔬菜',12.10,1.50,65.60,'干木耳，泡发后热量低','2026-10-14 15:12:25'),(70,'海带',13,'蔬菜',1.20,0.10,2.10,'含碘','2026-10-14 15:12:25'),(71,'豆腐',76,'蔬菜',8.10,4.80,1.90,'北豆腐','2026-10-14 15:12:25'),(72,'豆皮',409,'蔬菜',44.60,17.40,18.80,'干豆皮，热量高','2026-10-14 15:12:25'),(73,'腐竹',459,'蔬菜',44.60,21.70,22.30,'干制品','2026-10-14 15:12:25'),(74,'豆芽',18,'蔬菜',2.10,0.10,2.90,'黄豆芽','2026-10-14 15:12:25'),(75,'洋葱',40,'蔬菜',1.10,0.20,9.00,'紫洋葱','2026-10-14 15:12:25'),(76,'莲藕',70,'蔬菜',1.90,0.20,16.40,'含淀粉','2026-10-14 15:12:25'),(77,'西红柿炒鸡蛋',105,'炒菜',6.00,6.50,5.50,'家常菜','2026-10-14 15:12:25'),(78,'青椒炒肉',165,'炒菜',10.00,11.00,5.00,'含油炒制','2026-10-14 15:12:25'),(79,'鱼香肉丝',190,'炒菜',9.00,12.00,11.00,'川菜，含糖','2026-10-14 15:12:25'),(80,'宫保鸡丁',215,'炒菜',12.00,13.00,12.00,'含花生与糖','2026-10-14 15:12:25'),(81,'麻婆豆腐',150,'炒菜',8.00,11.00,5.00,'川菜，油大','2026-10-14 15:12:25'),(82,'地三鲜',180,'炒菜',3.00,12.00,15.00,'茄子土豆青椒，吸油多','2026-10-14 15:12:25'),(83,'干煸豆角',175,'炒菜',4.00,13.00,10.00,'油煸','2026-10-14 15:12:25'),(84,'清炒时蔬',65,'炒菜',2.00,4.50,4.00,'少油快炒','2026-10-14 15:12:25'),(85,'蒜蓉西兰花',70,'炒菜',3.50,4.50,5.00,'少油','2026-10-14 15:12:25'),(86,'炒青菜',60,'炒菜',2.00,4.00,4.00,'家常','2026-10-14 15:12:25'),(87,'醋溜白菜',75,'炒菜',1.50,5.00,6.00,'家常','2026-10-14 15:12:25'),(88,'木须肉',155,'炒菜',9.00,10.00,7.00,'含鸡蛋木耳','2026-10-14 15:12:25'),(89,'糖醋里脊',245,'炒菜',12.00,13.00,20.00,'油炸+糖，热量高','2026-10-14 15:12:25'),(90,'红烧茄子',160,'炒菜',2.00,12.00,11.00,'吸油多','2026-10-14 15:12:25'),(91,'酸辣土豆丝',110,'炒菜',2.00,6.00,12.00,'家常','2026-10-14 15:12:25'),(92,'炒鸡蛋',190,'炒菜',12.00,14.00,2.00,'油炒','2026-10-14 15:12:25'),(93,'韭菜炒鸡蛋',145,'炒菜',9.00,10.00,4.00,'家常','2026-10-14 15:12:25'),(94,'肉末茄子',165,'炒菜',6.00,12.00,9.00,'家常','2026-10-14 15:12:25'),(95,'水煮肉片',230,'炒菜',14.00,17.00,5.00,'油大','2026-10-14 15:12:25'),(96,'辣子鸡',260,'炒菜',18.00,18.00,6.00,'油炸','2026-10-14 15:12:25'),(97,'炖排骨',210,'炒菜',15.00,15.00,3.00,'汤菜','2026-10-14 15:12:25'),(98,'土豆炖牛肉',145,'炒菜',9.00,7.00,12.00,'炖菜','2026-10-14 15:12:25'),(99,'冬瓜排骨汤',45,'炒菜',3.50,2.50,2.00,'汤类，热量低','2026-10-14 15:12:25'),(100,'紫菜蛋花汤',30,'炒菜',2.50,1.20,2.00,'汤类','2026-10-14 15:12:25'),(101,'苹果',52,'水果',0.30,0.20,13.80,'中等大小约 200g','2026-10-14 15:12:25'),(102,'香蕉',89,'水果',1.10,0.30,22.80,'热量较高的水果','2026-10-14 15:12:25'),(103,'橙子',47,'水果',0.90,0.20,11.10,'富含维C','2026-10-14 15:12:25'),(104,'橘子',44,'水果',0.80,0.10,11.90,'砂糖橘','2026-10-14 15:12:25'),(105,'葡萄',69,'水果',0.70,0.30,18.10,'含糖高','2026-10-14 15:12:25'),(106,'西瓜',30,'水果',0.60,0.10,7.90,'水分多','2026-10-14 15:12:25'),(107,'梨',44,'水果',0.40,0.20,13.30,'润肺','2026-10-14 15:12:25'),(108,'桃子',39,'水果',0.90,0.10,10.90,'水蜜桃','2026-10-14 15:12:25'),(109,'草莓',32,'水果',0.67,0.30,7.10,'低热量','2026-10-14 15:12:25'),(110,'蓝莓',57,'水果',0.70,0.30,14.50,'抗氧化','2026-10-14 15:12:25'),(111,'猕猴桃',61,'水果',1.10,0.50,14.70,'维C高','2026-10-14 15:12:25'),(112,'芒果',60,'水果',0.80,0.40,15.00,'热带水果','2026-10-14 15:12:25'),(113,'菠萝',50,'水果',0.50,0.10,13.10,'含蛋白酶','2026-10-14 15:12:25'),(114,'火龙果',60,'水果',1.10,0.20,13.30,'白心','2026-10-14 15:12:25'),(115,'圣女果',20,'水果',0.90,0.20,4.00,'小番茄','2026-10-14 15:12:25'),(116,'牛油果',160,'水果',2.00,15.00,8.50,'健康脂肪','2026-10-14 15:12:25'),(117,'红枣（干）',276,'水果',3.20,0.50,67.80,'干果，含糖高','2026-10-14 15:12:25'),(118,'牛奶',54,'饮品',3.00,3.20,3.40,'全脂牛奶','2026-10-14 15:12:25'),(119,'脱脂牛奶',35,'饮品',3.40,0.30,5.00,'低脂','2026-10-14 15:12:25'),(120,'酸奶',72,'饮品',3.20,2.70,9.30,'含糖酸奶','2026-10-14 15:12:25'),(121,'无糖酸奶',60,'饮品',3.50,3.20,4.00,'减脂友好','2026-10-14 15:12:25'),(122,'豆浆',31,'饮品',3.00,1.60,1.80,'无糖','2026-10-14 15:12:25'),(123,'豆奶',43,'饮品',2.40,1.50,5.00,'含糖','2026-10-14 15:12:25'),(124,'可乐',42,'饮品',0.00,0.00,10.60,'含糖饮料','2026-10-14 15:12:25'),(125,'雪碧',40,'饮品',0.00,0.00,10.00,'含糖饮料','2026-10-14 15:12:25'),(126,'橙汁',45,'饮品',0.70,0.20,10.40,'鲜榨','2026-10-14 15:12:25'),(127,'奶茶',90,'饮品',1.50,3.00,14.00,'含糖含奶精，热量高','2026-10-14 15:12:25'),(128,'咖啡（黑）',2,'饮品',0.20,0.00,0.30,'无糖美式','2026-10-14 15:12:25'),(129,'拿铁',55,'饮品',3.00,3.00,5.00,'含牛奶','2026-10-14 15:12:25'),(130,'啤酒',43,'饮品',0.30,0.00,3.60,'酒精饮料','2026-10-14 15:12:25'),(131,'白酒',295,'饮品',0.00,0.00,0.00,'高度酒','2026-10-14 15:12:25'),(132,'红酒',85,'饮品',0.20,0.00,2.60,'酒精饮料','2026-10-14 15:12:25'),(133,'运动饮料',26,'饮品',0.00,0.00,6.50,'含电解质与糖','2026-10-14 15:12:25'),(134,'坚果（混合）',607,'零食',20.00,54.00,20.00,'热量极高，建议限量','2026-10-14 15:12:25'),(135,'核桃',646,'零食',14.90,58.80,19.10,'健脑','2026-10-14 15:12:25'),(136,'花生',574,'零食',24.80,44.30,21.70,'炒花生','2026-10-14 15:12:25'),(137,'瓜子',606,'零食',19.10,53.40,16.00,'炒瓜子','2026-10-14 15:12:25'),(138,'杏仁',578,'零食',21.20,50.60,21.70,'巴旦木','2026-10-14 15:12:25'),(139,'巧克力',546,'零食',4.90,31.30,61.00,'含糖高','2026-10-14 15:12:25'),(140,'黑巧克力',598,'零食',7.80,42.60,45.90,'可可含量 70%+','2026-10-14 15:12:25'),(141,'薯片',536,'零食',6.30,34.00,50.00,'油炸零食','2026-10-14 15:12:25'),(142,'饼干',435,'零食',7.00,15.00,70.00,'甜饼干','2026-10-14 15:12:25'),(143,'蛋糕',347,'零食',5.50,13.00,52.00,'奶油蛋糕','2026-10-14 15:12:25'),(144,'冰淇淋',207,'零食',3.50,11.00,24.00,'含糖含脂','2026-10-14 15:12:25'),(145,'蛋黄酥',420,'零食',7.00,22.00,50.00,'中式点心','2026-10-14 15:12:25'),(146,'月饼',400,'零食',6.00,16.00,58.00,'广式月饼','2026-10-14 15:12:25'),(147,'沙琪玛',430,'零食',5.00,18.00,62.00,'油炸点心','2026-10-14 15:12:25'),(148,'蛋白粉',380,'零食',80.00,3.00,8.00,'乳清蛋白','2026-10-14 15:12:25'),(149,'能量棒',380,'零食',10.00,12.00,55.00,'运动补给','2026-10-14 15:12:25'),(150,'蜂蜜',321,'零食',0.40,1.90,75.60,'天然糖','2026-10-14 15:12:25'),(151,'白砂糖',400,'零食',0.00,0.00,99.90,'纯糖','2026-10-14 15:12:25');
/*!40000 ALTER TABLE `common_food` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exercise_record`
--

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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='运动记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exercise_record`
--

LOCK TABLES `exercise_record` WRITE;
/*!40000 ALTER TABLE `exercise_record` DISABLE KEYS */;
INSERT INTO `exercise_record` VALUES (8,3,'跑步','1',30,150,'2026-10-14','2026-10-14 16:01:06',NULL,'2026-10-14 16:01:06');
/*!40000 ALTER TABLE `exercise_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fitness_plan`
--

DROP TABLE IF EXISTS `fitness_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fitness_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `plan_name` varchar(100) NOT NULL COMMENT '计划名称',
  `plan_type` varchar(50) DEFAULT NULL COMMENT '计划类型：减脂/增肌/塑形/维持',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重',
  `weight_to_lose` decimal(5,2) DEFAULT NULL COMMENT '需减体重(kg)',
  `target_calorie` int(11) DEFAULT NULL COMMENT '目标热量',
  `daily_deficit` int(11) DEFAULT NULL COMMENT '每日热量缺口(kcal)',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` int(11) DEFAULT '1' COMMENT '状态：0-停用 1-进行中 2-已完成',
  `remark` text COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='健身计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fitness_plan`
--

LOCK TABLES `fitness_plan` WRITE;
/*!40000 ALTER TABLE `fitness_plan` DISABLE KEYS */;
INSERT INTO `fitness_plan` VALUES (2,3,'维持计划','maintain',70.00,5.00,2460,0,'2026-10-14','2026-12-30',1,'建议摄入 2460 kcal，保持均衡饮食与规律运动','2026-10-14 16:01:37','2026-10-14 17:08:19'),(9,2,'维持计划','maintain',NULL,NULL,2597,0,'2026-10-18','2026-11-17',1,'建议摄入 2597 kcal，保持均衡饮食与规律运动','2026-10-18 13:49:34','2026-10-18 13:49:34');
/*!40000 ALTER TABLE `fitness_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_record`
--

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='食物记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_record`
--

LOCK TABLES `food_record` WRITE;
/*!40000 ALTER TABLE `food_record` DISABLE KEYS */;
INSERT INTO `food_record` VALUES (1,3,'馒头',100,223,'2026-10-14','2026-10-14 15:02:45',NULL,NULL,'2026-10-14 15:02:45'),(2,3,'猪肉',150,395,'2026-10-14','2026-10-14 15:03:09',NULL,NULL,'2026-10-14 15:03:09');
/*!40000 ALTER TABLE `food_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `health_assessment`
--

DROP TABLE IF EXISTS `health_assessment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_assessment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `assess_date` date NOT NULL COMMENT '评测日期',
  `bmi` decimal(4,1) DEFAULT NULL COMMENT 'BMI',
  `body_fat_rate` decimal(4,1) DEFAULT NULL COMMENT '体脂率(%)',
  `muscle_mass` decimal(5,1) DEFAULT NULL COMMENT '肌肉量(kg)',
  `water_rate` decimal(4,1) DEFAULT NULL COMMENT '体水分率(%)',
  `fat_mass` decimal(5,1) DEFAULT NULL COMMENT '脂肪量(kg)',
  `lean_mass` decimal(5,1) DEFAULT NULL COMMENT '去脂体重(kg)',
  `bmr` int(11) DEFAULT NULL COMMENT '基础代谢(kcal)',
  `metabolic_age` int(11) DEFAULT NULL COMMENT '代谢年龄(岁)',
  `visceral_fat` int(11) DEFAULT NULL COMMENT '内脏脂肪等级',
  `health_score` int(11) DEFAULT NULL COMMENT '综合健康评分(0-100)',
  `health_level` varchar(20) DEFAULT NULL COMMENT '健康等级',
  `nutrition_score` int(11) DEFAULT NULL COMMENT '营养均衡评分(0-100)',
  `advice` text COMMENT '建议(JSON数组)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`assess_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='健康评测记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `health_assessment`
--

LOCK TABLES `health_assessment` WRITE;
/*!40000 ALTER TABLE `health_assessment` DISABLE KEYS */;
/*!40000 ALTER TABLE `health_assessment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice_message`
--

DROP TABLE IF EXISTS `notice_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notice_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sender_id` bigint(20) NOT NULL COMMENT '发送人（管理员）ID',
  `sender_name` varchar(50) DEFAULT NULL COMMENT '发送人昵称',
  `receiver_id` bigint(20) DEFAULT NULL COMMENT '接收人ID，NULL 表示全体广播',
  `title` varchar(100) NOT NULL COMMENT '标题',
  `content` varchar(500) NOT NULL COMMENT '内容',
  `type` int(11) NOT NULL DEFAULT '1' COMMENT '类型：1-通知 2-提醒 3-警告',
  `is_read` int(11) NOT NULL DEFAULT '0' COMMENT '0-未读 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_receiver` (`receiver_id`,`is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='管理员消息提醒';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice_message`
--

LOCK TABLES `notice_message` WRITE;
/*!40000 ALTER TABLE `notice_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nutrition_consult`
--

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

--
-- Dumping data for table `nutrition_consult`
--

LOCK TABLES `nutrition_consult` WRITE;
/*!40000 ALTER TABLE `nutrition_consult` DISABLE KEYS */;
/*!40000 ALTER TABLE `nutrition_consult` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

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
  `role` int(11) NOT NULL DEFAULT '0' COMMENT '角色：0-普通用户 1-管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','e10adc3949ba59abbe56e057f20f883e','管理员','admin@fitness.com',NULL,NULL,1,30,175.00,70.00,NULL,NULL,2500,1,1,'2026-10-13 15:46:45','2026-10-26 22:26:34'),(2,'test','e10adc3949ba59abbe56e057f20f883e','jp','','',NULL,1,22,183.00,85.00,NULL,NULL,1800,1,0,'2026-10-13 15:46:45','2026-10-14 19:30:41'),(3,'jp666','e10adc3949ba59abbe56e057f20f883e','jp','','',NULL,1,22,183.00,74.90,NULL,NULL,2000,1,0,'2026-10-13 15:48:04','2026-10-26 22:41:45');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_ai_config`
--

DROP TABLE IF EXISTS `user_ai_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_ai_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `provider` varchar(50) NOT NULL DEFAULT 'glm' COMMENT '服务商：glm/doubao/deepseek/ollama',
  `api_key` varchar(500) DEFAULT NULL COMMENT 'API密钥（加密存储）',
  `api_url` varchar(255) DEFAULT NULL COMMENT '接口地址（可自定义）',
  `model` varchar(100) DEFAULT NULL COMMENT '模型名称',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-否 1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户AI配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_ai_config`
--

LOCK TABLES `user_ai_config` WRITE;
/*!40000 ALTER TABLE `user_ai_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_ai_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `weight_record`
--

DROP TABLE IF EXISTS `weight_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `weight_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `weight` decimal(5,2) NOT NULL COMMENT '体重(kg)',
  `bmi` decimal(4,2) DEFAULT NULL COMMENT 'BMI（记录时计算）',
  `record_date` date NOT NULL COMMENT '记录日期',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`record_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='每日体重记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `weight_record`
--

LOCK TABLES `weight_record` WRITE;
/*!40000 ALTER TABLE `weight_record` DISABLE KEYS */;
INSERT INTO `weight_record` VALUES (2,3,74.90,22.40,'2026-10-14','','2026-10-14 18:53:13','2026-10-14 18:53:21');
/*!40000 ALTER TABLE `weight_record` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-06 14:31:49
