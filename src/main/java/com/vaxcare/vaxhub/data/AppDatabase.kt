/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaxcare.core.model.inventory.WrongProductNdcEntity
import com.vaxcare.core.secret.SecretProvider
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.util.MobileDatabaseUtil
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.data.dao.AppointmentDao
import com.vaxcare.vaxhub.data.dao.ClinicDao
import com.vaxcare.vaxhub.data.dao.DoorSensorEventDao
import com.vaxcare.vaxhub.data.dao.FeatureFlagDao
import com.vaxcare.vaxhub.data.dao.LocationDao
import com.vaxcare.vaxhub.data.dao.LotInventoryDao
import com.vaxcare.vaxhub.data.dao.LotNumberDao
import com.vaxcare.vaxhub.data.dao.OfflineRequestDao
import com.vaxcare.vaxhub.data.dao.OrderDao
import com.vaxcare.vaxhub.data.dao.PayerDao
import com.vaxcare.vaxhub.data.dao.ProductDao
import com.vaxcare.vaxhub.data.dao.ProviderDao
import com.vaxcare.vaxhub.data.dao.SessionDao
import com.vaxcare.vaxhub.data.dao.ShotAdministratorDao
import com.vaxcare.vaxhub.data.dao.SimpleOnHandInventoryDao
import com.vaxcare.vaxhub.data.dao.UserDao
import com.vaxcare.vaxhub.data.dao.WrongProductNdcDao
import com.vaxcare.vaxhub.data.dao.legacy.LegacyCountDao
import com.vaxcare.vaxhub.data.dao.legacy.LegacyLotInventoryDao
import com.vaxcare.vaxhub.data.typeconverter.BasicTypeConverters
import com.vaxcare.vaxhub.data.typeconverter.InventorySourceEnumTypeConverters
import com.vaxcare.vaxhub.data.typeconverter.PatchBodyEnumTypeConverters
import com.vaxcare.vaxhub.data.typeconverter.ProductEnumTypeConverters
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.AppointmentData
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.DoorSensorEventModel
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.Location
import com.vaxcare.vaxhub.model.LotInventory
import com.vaxcare.vaxhub.model.OfflineRequest
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.appointment.AppointmentHubMetaData
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.inventory.AgeIndication
import com.vaxcare.vaxhub.model.inventory.CptCvxCode
import com.vaxcare.vaxhub.model.inventory.LotNumber
import com.vaxcare.vaxhub.model.inventory.NdcCode
import com.vaxcare.vaxhub.model.inventory.Package
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProductDTO
import com.vaxcare.vaxhub.model.legacy.LegacyCount
import com.vaxcare.vaxhub.model.legacy.LegacyCountEntry
import com.vaxcare.vaxhub.model.legacy.LegacyLotInventory
import com.vaxcare.vaxhub.model.legacy.LegacyProductMapping
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.model.user.SessionUser
import net.sqlcipher.database.SupportFactory
import timber.log.Timber

/**
 * The database class responsible for all data stored on the VaxHub.
 *
 * This uses [cwac-saferoom](https://github.com/commonsguy/cwac-saferoom) for the SqLite encryption.
 * We use the Build's Serial number to encrypt the Database.
 *
 * Migrations should be handled here for each database upgrade, we do not want to support downgrades
 * so we should drop the database and rebuild if a user downgrades the app somehow.
 *
 */
@Database(
    entities = [
        AdministeredVaccine::class,
        AgeIndication::class,
        AppointmentData::class,
        CptCvxCode::class,
        DoorSensorEventModel::class,
        FeatureFlag::class,
        LegacyCount::class,
        LegacyCountEntry::class,
        LegacyLotInventory::class,
        LegacyProductMapping::class,
        Location::class,
        LotNumber::class,
        LotInventory::class,
        NdcCode::class,
        OfflineRequest::class,
        Product::class,
        ProductOneTouch::class,
        Package::class,
        Provider::class,
        ShotAdministrator::class,
        User::class,
        Payer::class,
        Clinic::class,
        OrderEntity::class,
        EncounterStateEntity::class,
        EncounterMessageEntity::class,
        AppointmentHubMetaData::class,
        SimpleOnHandProductDTO::class,
        SessionUser::class,
        WrongProductNdcEntity::class
    ],
    version = 39
)
@TypeConverters(
    BasicTypeConverters::class,
    InventorySourceEnumTypeConverters::class,
    ProductEnumTypeConverters::class,
    PatchBodyEnumTypeConverters::class
)
@Suppress("ktlint:standard:max-line-length")
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val DATABASE_NAME = "vaxhub"

        fun initialize(
            context: Context,
            secretProvider: SecretProvider,
            localStorage: LocalStorage
        ): AppDatabase {
            val secretBytes = secretProvider.databaseSecret().toByteArray()

            if (!BuildConfig.DEBUG) {
                MobileDatabaseUtil.encryptDatabase(
                    context,
                    secretBytes,
                    localStorage.deviceSerialNumber,
                    DATABASE_NAME
                )

                Timber.i("Database: Encrypted")
            }

            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .apply {
                    fallbackToDestructiveMigration()
                    if (!BuildConfig.DEBUG) {
                        openHelperFactory(SupportFactory(secretBytes))
                    }

                    addMigrations(
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_28_29,
                        MIGRATION_31_32,
                        MIGRATION_33_34,
                        MIGRATION_34_35,
                        MIGRATION_36_37
                    )
                }.build()
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS CptCvxCode")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `CptCvxCode` (`cptCode` TEXT NOT NULL, `cvxCode` TEXT, `isMedicare` INTEGER NOT NULL, `productId` INTEGER NOT NULL, PRIMARY KEY(`cptCode`))"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // For AdministeredVaccine will add columns: deletedDate / isDeleted
                database.execSQL("DROP TABLE IF EXISTS AdministeredVaccine")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `AdministeredVaccine` (`id` INTEGER NOT NULL, `checkInVaccinationId` INTEGER NOT NULL, `appointmentId` INTEGER NOT NULL, `lotNumber` TEXT NOT NULL, `ageIndicated` INTEGER NOT NULL, `method` TEXT, `site` TEXT, `productId` INTEGER NOT NULL, `doseSeries` INTEGER, `synced` INTEGER, `deletedDate` INTEGER, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )

                // For AppointmentData will add columns: isEditable / isProcessing
                database.execSQL("ALTER TABLE AppointmentData ADD COLUMN `isEditable` INTEGER")
                // isProcessing: It is used for local data judgment, Api will not return, that is, the initial value is always 0
                database.execSQL("ALTER TABLE AppointmentData ADD COLUMN `isProcessing` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Users")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Users` (`firstName` TEXT, `lastName` TEXT, `pin` TEXT NOT NULL, `userName` TEXT, `userId` INTEGER NOT NULL, PRIMARY KEY(`userId`))"
                )
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS LotNumber")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `LotNumber` (`expirationDate` INTEGER, `id` INTEGER NOT NULL, `name` TEXT NOT NULL, `productId` INTEGER NOT NULL, `salesLotNumberId` INTEGER NOT NULL, `salesProductId` INTEGER NOT NULL, `unreviewed` INTEGER NOT NULL, `source` INTEGER, PRIMARY KEY(`id`, `productId`))"
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Metrics")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Clinics")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Clinics` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `state` TEXT NOT NULL, `startDate` INTEGER, `endDate` INTEGER, `type` INTEGER NOT NULL, `locationId` INTEGER NOT NULL, `locationNodeId` TEXT NOT NULL, `isIntegrated` INTEGER NOT NULL, `parentClinicId` INTEGER NOT NULL, `isSchoolCaresEnabled` INTEGER NOT NULL, `temporaryClinicType` TEXT, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS LegacyProductMapping")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `LegacyProductMapping` (`id` INTEGER NOT NULL, `epProductName` TEXT NOT NULL, `epPackageId` INTEGER NOT NULL, `epProductId` INTEGER NOT NULL, `prettyName` VARCHAR, `dosesInSeries` INTEGER, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS RiskAssessment")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `RiskAssessment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `appointmentId` INTEGER NOT NULL, `riskStatus` INTEGER NOT NULL, `compensationStatus` INTEGER, `compensationSubStatus` INTEGER, `iconUrl` TEXT, `mobileMessage` TEXT, `primaryMessage` TEXT, `secondaryMessage` TEXT, `detailedMessage` TEXT, `hasMedDRisk` INTEGER NOT NULL, `type` TEXT, `topRejectCode` TEXT, `callToAction` INTEGER, `riskAssessmentMedDDetermination_callToAction` INTEGER, `riskAssessmentMedDDetermination_riskStatus` INTEGER, `riskAssessmentMedDDetermination_iconUrl` TEXT, `riskAssessmentMedDDetermination_mobileMessage` TEXT, `riskAssessmentMedDDetermination_primaryMessage` TEXT, `riskAssessmentMedDDetermination_secondaryMessage` TEXT)"
                )
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS RiskAssessment")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `RiskAssessment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `appointmentId` INTEGER NOT NULL, `riskStatus` INTEGER NOT NULL, `compensationStatus` INTEGER, `compensationSubStatus` INTEGER, `iconUrl` TEXT, `mobileMessage` TEXT, `primaryMessage` TEXT, `secondaryMessage` TEXT, `detailedMessage` TEXT, `hasMedDRisk` INTEGER NOT NULL, `type` TEXT, `topRejectCode` TEXT, `callToAction` INTEGER, `shotStatus` INTEGER, `riskAssessmentMedDDetermination_callToAction` INTEGER, `riskAssessmentMedDDetermination_riskStatus` INTEGER, `riskAssessmentMedDDetermination_iconUrl` TEXT, `riskAssessmentMedDDetermination_mobileMessage` TEXT, `riskAssessmentMedDDetermination_primaryMessage` TEXT, `riskAssessmentMedDDetermination_secondaryMessage` TEXT)"
                )
            }
        }
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS RiskAssessment")
                database.execSQL("DROP TABLE IF EXISTS RiskAssessmentMedDDeterminations")
            }
        }
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE OfflineRequest ADD COLUMN `originalDateTime` INTEGER")
            }
        }
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS LotNumber")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `LotNumber` (`expirationDate` INTEGER, `id` INTEGER NOT NULL, `name` TEXT NOT NULL, `productId` INTEGER NOT NULL, `salesLotNumberId` INTEGER NOT NULL, `salesProductId` INTEGER NOT NULL, `unreviewed` INTEGER NOT NULL, `source` INTEGER, PRIMARY KEY(`id`))"
                )
            }
        }
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Location ADD COLUMN `integrationType` TEXT")
            }
        }
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Product ADD COLUMN `prettyName` TEXT")
            }
        }
        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AgeIndication ADD COLUMN `warning_title` TEXT")
                database.execSQL("ALTER TABLE AgeIndication ADD COLUMN `warning_message` TEXT")
            }
        }
        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Product")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Product` (`antigen` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `description` TEXT NOT NULL, `displayName` TEXT NOT NULL, `id` INTEGER PRIMARY KEY NOT NULL, `inventoryGroup` TEXT NOT NULL, `lossFee` INTEGER, `productNdc` TEXT, `routeCode` INTEGER NOT NULL, `presentation` INTEGER NOT NULL, `purchaseOrderFee` INTEGER, `visDates` TEXT, `status` INTEGER NOT NULL, `prettyName` TEXT)"
                )
            }
        }
        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Payers")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Payers` (`portalInsuranceMappingId` INTEGER, `insuranceName` TEXT, `extensionFlags` TEXT, `state` TEXT, `insuranceId` INTEGER, `insurancePlanId` INTEGER, `updatedTime` INTEGER, `id` INTEGER PRIMARY KEY NOT NULL)"
                )
            }
        }
    }

    abstract fun appointmentDao(): AppointmentDao

    abstract fun doorSensorDao(): DoorSensorEventDao

    abstract fun featureFlagDao(): FeatureFlagDao

    abstract fun legacyCountDao(): LegacyCountDao

    abstract fun legacyLotInventoryDao(): LegacyLotInventoryDao

    abstract fun locationDao(): LocationDao

    abstract fun lotInventoryDao(): LotInventoryDao

    abstract fun lotNumberDao(): LotNumberDao

    abstract fun offlineRequestDao(): OfflineRequestDao

    abstract fun productDao(): ProductDao

    abstract fun shotAdministratorDao(): ShotAdministratorDao

    abstract fun userDao(): UserDao

    abstract fun providerDao(): ProviderDao

    abstract fun payerDao(): PayerDao

    abstract fun clinicDao(): ClinicDao

    abstract fun orderDao(): OrderDao

    abstract fun simpleOnHandInventoryDao(): SimpleOnHandInventoryDao

    abstract fun sessionDao(): SessionDao

    abstract fun wrongProductNdcDao(): WrongProductNdcDao
}
