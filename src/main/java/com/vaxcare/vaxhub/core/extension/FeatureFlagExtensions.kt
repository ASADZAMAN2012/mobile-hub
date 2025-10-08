/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.model.FeatureFlag

fun List<FeatureFlag>.isMedDAutoRunEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.FeatureMedDAutoRun.value }

fun List<FeatureFlag>.isRightPatientRightDoseEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.RightPatientRightDose.value }

fun List<FeatureFlag>.isDuplicateRSVDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableDuplicateRSV.value }

fun List<FeatureFlag>.isMbiForMedDEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.FeatureEnableMbiForMedicarePartD.value }

fun List<FeatureFlag>.isFeaturePublicStockPilotEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.FeaturePublicStockPilot.value }

fun List<FeatureFlag>.isMobileStockSelectorEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.MobileStockSelector.value }

fun List<FeatureFlag>.isInsurancePhoneCaptureDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableInsurancePhoneCapture.value }

fun List<FeatureFlag>.isNewInsurancePromptDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableNewInsurancePrompt.value }

fun List<FeatureFlag>.isCheckoutDemographicsCollectionDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableDemoCapture.value }

fun List<FeatureFlag>.isPaymentModeSelectionDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisablePaymentModeSelection.value }

fun List<FeatureFlag>.isPayorSelectionEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.EnablePayorSelection.value }

fun List<FeatureFlag>.isInsuranceScanDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.FeatureSkipInsuranceScan3.value }

fun List<FeatureFlag>.isPayByPhoneDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.FeatureRemovePayByPhone.value }

fun List<FeatureFlag>.isCreditCardCaptureDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableCreditCardCapture.value }

fun List<FeatureFlag>.isDoBCaptureDisabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.DisableDoBCapture.value }

fun List<FeatureFlag>.isVaxCare3Enabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.VaxCare3.value }

fun List<FeatureFlag>.isEmployerCoveredEnabled(): Boolean =
    this.any { it.featureFlagName == FeatureFlagConstant.EmployerCovered.value }
