-- MySQL dump 10.13  Distrib 5.7.38, for Linux (x86_64)
--
-- Host: localhost    Database: collectory
-- ------------------------------------------------------
-- Server version	5.7.38-0ubuntu0.18.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `activity_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `action` varchar(255) NOT NULL,
  `admin` bit(1) NOT NULL,
  `administrator_for_entity` bit(1) NOT NULL,
  `contact_for_entity` bit(1) NOT NULL,
  `entity_uid` varchar(255) DEFAULT NULL,
  `timestamp` datetime NOT NULL,
  `user` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `address`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `post_box` varchar(255) DEFAULT NULL,
  `postcode` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attribution`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `name` varchar(256) NOT NULL,
  `uid` varchar(20) NOT NULL,
  `url` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actor` varchar(255) DEFAULT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `event_name` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `new_value` varchar(255) DEFAULT NULL,
  `old_value` varchar(255) DEFAULT NULL,
  `persisted_object_id` varchar(255) DEFAULT NULL,
  `persisted_object_version` bigint(20) DEFAULT NULL,
  `property_name` varchar(255) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `collection`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(45) DEFAULT NULL,
  `active` varchar(14) DEFAULT NULL,
  `address_city` varchar(255) DEFAULT NULL,
  `address_country` varchar(255) DEFAULT NULL,
  `address_post_box` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) DEFAULT NULL,
  `address_state` varchar(255) DEFAULT NULL,
  `address_street` varchar(255) DEFAULT NULL,
  `altitude` varchar(255) DEFAULT NULL,
  `attributions` varchar(256) DEFAULT NULL,
  `collection_type` varchar(256) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `east_coordinate` decimal(13,10) NOT NULL,
  `email` varchar(256) DEFAULT NULL,
  `end_date` varchar(45) DEFAULT NULL,
  `focus` longtext,
  `gbif_registry_key` varchar(36) DEFAULT NULL,
  `geographic_description` varchar(255) DEFAULT NULL,
  `guid` varchar(256) DEFAULT NULL,
  `image_ref_attribution` varchar(255) DEFAULT NULL,
  `image_ref_caption` varchar(255) DEFAULT NULL,
  `image_ref_copyright` varchar(255) DEFAULT NULL,
  `image_ref_file` varchar(255) DEFAULT NULL,
  `institution_id` bigint(20) DEFAULT NULL,
  `isalapartner` bit(1) NOT NULL,
  `keywords` longtext,
  `kingdom_coverage` longtext,
  `last_updated` datetime NOT NULL,
  `latitude` decimal(13,10) NOT NULL,
  `logo_ref_attribution` varchar(255) DEFAULT NULL,
  `logo_ref_caption` varchar(255) DEFAULT NULL,
  `logo_ref_copyright` varchar(255) DEFAULT NULL,
  `logo_ref_file` varchar(255) DEFAULT NULL,
  `longitude` decimal(13,10) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `network_membership` longtext,
  `north_coordinate` decimal(13,10) NOT NULL,
  `notes` longtext,
  `num_records` int(11) NOT NULL,
  `num_records_digitised` int(11) NOT NULL,
  `phone` varchar(200) DEFAULT NULL,
  `pub_description` longtext,
  `pub_short_description` varchar(100) DEFAULT NULL,
  `scientific_names` longtext,
  `south_coordinate` decimal(13,10) NOT NULL,
  `start_date` varchar(45) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `states` varchar(255) DEFAULT NULL,
  `sub_collections` longtext,
  `taxonomy_hints` longtext,
  `tech_description` longtext,
  `uid` varchar(20) NOT NULL,
  `user_last_modified` varchar(255) NOT NULL,
  `website_url` varchar(256) DEFAULT NULL,
  `west_coordinate` decimal(13,10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9835AE9EA1605ED4` (`institution_id`),
  CONSTRAINT `FK9835AE9EA1605ED4` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `date_created` datetime NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `fax` varchar(45) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `mobile` varchar(45) DEFAULT NULL,
  `notes` varchar(1024) DEFAULT NULL,
  `phone` varchar(45) DEFAULT NULL,
  `publish` bit(1) NOT NULL,
  `title` varchar(20) DEFAULT NULL,
  `user_last_modified` varchar(256) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact_for`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contact_for` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `administrator` bit(1) NOT NULL,
  `contact_id` bigint(20) NOT NULL,
  `date_created` datetime NOT NULL,
  `date_last_modified` datetime NOT NULL,
  `entity_uid` varchar(255) NOT NULL,
  `notify` bit(1) NOT NULL,
  `primary_contact` bit(1) NOT NULL,
  `role` varchar(128) DEFAULT NULL,
  `user_last_modified` varchar(256) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK274D02AA9302D4` (`contact_id`),
  KEY `contact_id_idx` (`contact_id`),
  KEY `entity_uid_idx` (`entity_uid`),
  CONSTRAINT `FK274D02AA9302D4` FOREIGN KEY (`contact_id`) REFERENCES `contact` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_hub`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_hub` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(45) DEFAULT NULL,
  `address_city` varchar(255) DEFAULT NULL,
  `address_country` varchar(255) DEFAULT NULL,
  `address_post_box` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) DEFAULT NULL,
  `address_state` varchar(255) DEFAULT NULL,
  `address_street` varchar(255) DEFAULT NULL,
  `altitude` varchar(255) DEFAULT NULL,
  `attributions` varchar(256) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `email` varchar(256) DEFAULT NULL,
  `focus` varchar(255) DEFAULT NULL,
  `gbif_registry_key` varchar(36) DEFAULT NULL,
  `guid` varchar(256) DEFAULT NULL,
  `image_ref_attribution` varchar(255) DEFAULT NULL,
  `image_ref_caption` varchar(255) DEFAULT NULL,
  `image_ref_copyright` varchar(255) DEFAULT NULL,
  `image_ref_file` varchar(255) DEFAULT NULL,
  `isalapartner` bit(1) NOT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `latitude` decimal(13,10) NOT NULL,
  `logo_ref_attribution` varchar(255) DEFAULT NULL,
  `logo_ref_caption` varchar(255) DEFAULT NULL,
  `logo_ref_copyright` varchar(255) DEFAULT NULL,
  `logo_ref_file` varchar(255) DEFAULT NULL,
  `longitude` decimal(13,10) NOT NULL,
  `member_collections` longtext,
  `member_data_resources` longtext,
  `member_institutions` longtext,
  `members` longtext,
  `name` varchar(1024) NOT NULL,
  `network_membership` varchar(256) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `phone` varchar(200) DEFAULT NULL,
  `pub_description` varchar(255) DEFAULT NULL,
  `pub_short_description` varchar(100) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `taxonomy_hints` varchar(255) DEFAULT NULL,
  `tech_description` varchar(255) DEFAULT NULL,
  `uid` varchar(20) NOT NULL,
  `user_last_modified` varchar(255) NOT NULL,
  `website_url` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_link`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_link` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `consumer` varchar(255) NOT NULL,
  `provider` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_provider`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_provider` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(45) DEFAULT NULL,
  `address_city` varchar(255) DEFAULT NULL,
  `address_country` varchar(255) DEFAULT NULL,
  `address_post_box` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) DEFAULT NULL,
  `address_state` varchar(255) DEFAULT NULL,
  `address_street` varchar(255) DEFAULT NULL,
  `altitude` varchar(255) DEFAULT NULL,
  `attributions` varchar(256) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `email` varchar(256) DEFAULT NULL,
  `focus` longtext,
  `gbif_country_to_attribute` varchar(3) DEFAULT NULL,
  `gbif_registry_key` varchar(36) DEFAULT NULL,
  `guid` varchar(256) DEFAULT NULL,
  `hiddenjson` longtext,
  `image_ref_attribution` varchar(255) DEFAULT NULL,
  `image_ref_caption` varchar(255) DEFAULT NULL,
  `image_ref_copyright` varchar(255) DEFAULT NULL,
  `image_ref_file` varchar(255) DEFAULT NULL,
  `isalapartner` bit(1) NOT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `latitude` decimal(13,10) NOT NULL,
  `logo_ref_attribution` varchar(255) DEFAULT NULL,
  `logo_ref_caption` varchar(255) DEFAULT NULL,
  `logo_ref_copyright` varchar(255) DEFAULT NULL,
  `logo_ref_file` varchar(255) DEFAULT NULL,
  `longitude` decimal(13,10) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `network_membership` longtext,
  `notes` longtext,
  `phone` varchar(200) DEFAULT NULL,
  `pub_description` longtext,
  `pub_short_description` longtext,
  `state` varchar(45) DEFAULT NULL,
  `taxonomy_hints` longtext,
  `tech_description` longtext,
  `uid` varchar(20) NOT NULL,
  `user_last_modified` varchar(255) NOT NULL,
  `website_url` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_resource`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(45) DEFAULT NULL,
  `address_city` varchar(255) DEFAULT NULL,
  `address_country` varchar(255) DEFAULT NULL,
  `address_post_box` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) DEFAULT NULL,
  `address_state` varchar(255) DEFAULT NULL,
  `address_street` varchar(255) DEFAULT NULL,
  `altitude` varchar(255) DEFAULT NULL,
  `attributions` varchar(256) DEFAULT NULL,
  `begin_date` varchar(255) DEFAULT NULL,
  `citation` longtext,
  `connection_parameters` longtext,
  `content_types` varchar(2048) DEFAULT NULL,
  `data_currency` datetime DEFAULT NULL,
  `data_generalizations` longtext,
  `data_provider_id` bigint(20) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `default_darwin_core_values` longtext,
  `download_limit` int(11) NOT NULL,
  `east_bounding_coordinate` varchar(255) DEFAULT NULL,
  `email` varchar(256) DEFAULT NULL,
  `end_date` varchar(255) DEFAULT NULL,
  `filed` bit(1) NOT NULL,
  `focus` longtext,
  `gbif_dataset` bit(1) NOT NULL DEFAULT b'0',
  `gbif_doi` varchar(255) DEFAULT NULL,
  `gbif_registry_key` varchar(36) DEFAULT NULL,
  `geographic_description` longtext,
  `guid` varchar(256) DEFAULT NULL,
  `harvest_frequency` int(11) NOT NULL,
  `harvesting_notes` longtext,
  `image_metadata` longtext,
  `image_ref_attribution` varchar(255) DEFAULT NULL,
  `image_ref_caption` varchar(255) DEFAULT NULL,
  `image_ref_copyright` varchar(255) DEFAULT NULL,
  `image_ref_file` varchar(255) DEFAULT NULL,
  `information_withheld` longtext,
  `institution_id` bigint(20) DEFAULT NULL,
  `isalapartner` bit(1) NOT NULL,
  `is_shareable_withgbif` bit(1) NOT NULL DEFAULT b'1',
  `keywords` varchar(255) DEFAULT NULL,
  `last_checked` datetime DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `latitude` decimal(13,10) NOT NULL,
  `license_type` varchar(45) DEFAULT NULL,
  `license_version` varchar(45) DEFAULT NULL,
  `logo_ref_attribution` varchar(255) DEFAULT NULL,
  `logo_ref_caption` varchar(255) DEFAULT NULL,
  `logo_ref_copyright` varchar(255) DEFAULT NULL,
  `logo_ref_file` varchar(255) DEFAULT NULL,
  `longitude` decimal(13,10) NOT NULL,
  `make_contact_public` bit(1) NOT NULL DEFAULT b'1',
  `method_step_description` longtext,
  `mobilisation_notes` longtext,
  `name` varchar(1024) NOT NULL,
  `network_membership` longtext,
  `north_bounding_coordinate` varchar(255) DEFAULT NULL,
  `notes` longtext,
  `permissions_document` longtext,
  `permissions_document_type` varchar(23) DEFAULT NULL,
  `phone` varchar(200) DEFAULT NULL,
  `provenance` varchar(45) DEFAULT NULL,
  `pub_description` longtext,
  `pub_short_description` varchar(100) DEFAULT NULL,
  `public_archive_available` bit(1) NOT NULL,
  `purpose` longtext,
  `quality_control_description` longtext,
  `repatriation_country` varchar(255) DEFAULT NULL,
  `resource_type` varchar(255) NOT NULL,
  `rights` longtext,
  `risk_assessment` bit(1) NOT NULL,
  `south_bounding_coordinate` varchar(255) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `status` varchar(45) NOT NULL,
  `taxonomy_hints` longtext,
  `tech_description` longtext,
  `uid` varchar(20) NOT NULL,
  `user_last_modified` varchar(255) NOT NULL,
  `website_url` varchar(256) DEFAULT NULL,
  `west_bounding_coordinate` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKE040503CE03A3F5` (`data_provider_id`),
  KEY `FKE040503A1605ED4` (`institution_id`),
  CONSTRAINT `FKE040503A1605ED4` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`),
  CONSTRAINT `FKE040503CE03A3F5` FOREIGN KEY (`data_provider_id`) REFERENCES `data_provider` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `external_identifier`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `external_identifier` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `entity_uid` varchar(255) NOT NULL,
  `identifier` varchar(255) NOT NULL,
  `source` varchar(255) NOT NULL,
  `uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `image`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `attribution` varchar(255) DEFAULT NULL,
  `caption` varchar(255) DEFAULT NULL,
  `copyright` varchar(255) DEFAULT NULL,
  `file` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `institution`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `institution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(45) DEFAULT NULL,
  `address_city` varchar(255) DEFAULT NULL,
  `address_country` varchar(255) DEFAULT NULL,
  `address_post_box` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) DEFAULT NULL,
  `address_state` varchar(255) DEFAULT NULL,
  `address_street` varchar(255) DEFAULT NULL,
  `altitude` varchar(255) DEFAULT NULL,
  `attributions` varchar(256) DEFAULT NULL,
  `child_institutions` varchar(255) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `email` varchar(256) DEFAULT NULL,
  `focus` varchar(255) DEFAULT NULL,
  `gbif_country_to_attribute` varchar(3) DEFAULT NULL,
  `gbif_registry_key` varchar(36) DEFAULT NULL,
  `guid` varchar(256) DEFAULT NULL,
  `image_ref_attribution` varchar(255) DEFAULT NULL,
  `image_ref_caption` varchar(255) DEFAULT NULL,
  `image_ref_copyright` varchar(255) DEFAULT NULL,
  `image_ref_file` varchar(255) DEFAULT NULL,
  `institution_type` varchar(45) DEFAULT NULL,
  `isalapartner` bit(1) NOT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `latitude` decimal(13,10) NOT NULL,
  `logo_ref_attribution` varchar(255) DEFAULT NULL,
  `logo_ref_caption` varchar(255) DEFAULT NULL,
  `logo_ref_copyright` varchar(255) DEFAULT NULL,
  `logo_ref_file` varchar(255) DEFAULT NULL,
  `longitude` decimal(13,10) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `network_membership` varchar(256) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `phone` varchar(200) DEFAULT NULL,
  `pub_description` varchar(255) DEFAULT NULL,
  `pub_short_description` varchar(100) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `taxonomy_hints` varchar(255) DEFAULT NULL,
  `tech_description` varchar(255) DEFAULT NULL,
  `uid` varchar(20) NOT NULL,
  `user_last_modified` varchar(255) NOT NULL,
  `website_url` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `licence`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `licence` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `acronym` varchar(255) NOT NULL,
  `date_created` datetime NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `licence_version` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `url` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `provider_code`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_code` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `code` varchar(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `provider_map`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `collection_id` bigint(20) NOT NULL,
  `date_created` datetime NOT NULL,
  `exact` bit(1) NOT NULL,
  `institution_id` bigint(20) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `match_any_collection_code` bit(1) NOT NULL,
  `warning` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `collection_id` (`collection_id`),
  KEY `FKE70A7A0EB01EB1A0` (`collection_id`),
  KEY `FKE70A7A0EA1605ED4` (`institution_id`),
  CONSTRAINT `FKE70A7A0EA1605ED4` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`),
  CONSTRAINT `FKE70A7A0EB01EB1A0` FOREIGN KEY (`collection_id`) REFERENCES `collection` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `provider_map_provider_code`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_map_provider_code` (
  `provider_map_collection_codes_id` bigint(20) DEFAULT NULL,
  `provider_code_id` bigint(20) DEFAULT NULL,
  `provider_map_institution_codes_id` bigint(20) DEFAULT NULL,
  KEY `FK13BB0B0A79EFEF03` (`provider_code_id`),
  KEY `FK13BB0B0A195335D1` (`provider_map_institution_codes_id`),
  KEY `FK13BB0B0A2B882E69` (`provider_map_collection_codes_id`),
  CONSTRAINT `FK13BB0B0A195335D1` FOREIGN KEY (`provider_map_institution_codes_id`) REFERENCES `provider_map` (`id`),
  CONSTRAINT `FK13BB0B0A2B882E69` FOREIGN KEY (`provider_map_collection_codes_id`) REFERENCES `provider_map` (`id`),
  CONSTRAINT `FK13BB0B0A79EFEF03` FOREIGN KEY (`provider_code_id`) REFERENCES `provider_code` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sequence`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sequence` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `next_id` bigint(20) NOT NULL,
  `prefix` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temp_data_resource`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `temp_data_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `ala_id` varchar(256) DEFAULT NULL,
  `citation` longtext,
  `csv_separator` varchar(10) DEFAULT NULL,
  `data_generalisations` longtext,
  `date_created` datetime NOT NULL,
  `description` longtext,
  `email` varchar(256) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `information_withheld` longtext,
  `is_contact_public` bit(1) DEFAULT NULL,
  `key_fields` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `license` varchar(10) DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  `number_of_records` int(11) NOT NULL,
  `prod_uid` varchar(20) DEFAULT NULL,
  `source_file` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `ui_url` varchar(255) DEFAULT NULL,
  `uid` varchar(20) NOT NULL,
  `webservice_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid_idx` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-07-13 10:52:05
