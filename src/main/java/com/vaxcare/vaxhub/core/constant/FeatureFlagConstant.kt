/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.constant

sealed class FeatureFlagConstant(val value: String) {
    /**
     * Allows addition of legacy COVID products
     * @deprecated
     */
    object FeatureCovidAssist : FeatureFlagConstant("COVIDAssist")

    /**
     * Allows the UI to display stock for some flows
     */
    object FeaturePublicStockPilot : FeatureFlagConstant("PublicStockPilot")

    /**
     * Allows RPRD and orders to be displayed on the UI
     */
    object RightPatientRightDose : FeatureFlagConstant("RightPatientRightDose")

    /**
     * Allows prefilling of phone number for phone collection during the payment flow
     */
    object FeaturePrefillPhone : FeatureFlagConstant("PrefillPhone")

    /**
     * Allows the creation of appointments
     */
    object FeatureAddAppt3 : FeatureFlagConstant("AddAppts3.0")

    /**
     * Hides the insurance card collection
     */
    object FeatureSkipInsuranceScan3 : FeatureFlagConstant("SkipInsuranceScan3.0")

    /**
     * Login Okta/SSO via vaxidentity
     * This will require internet access when pinning in
     */
    object FeatureHubLoginUserAndPin : FeatureFlagConstant("HubLoginUserAndPin")

    /**
     * Removes Phone Collection option from Payment Collection and instead, treat as if they
     * tapped on "Cash/Check" instead during the previous flow
     */
    object FeatureRemovePayByPhone : FeatureFlagConstant("RemovePayByPhone")

    /**
     * Enables use of MBI for Medicare Part D Eligibility
     */
    object FeatureEnableMbiForMedicarePartD : FeatureFlagConstant("EnableMbiForMedicarePartD")

    /**
     * Enables use of Med-D autorun for appointments
     */
    object FeatureMedDAutoRun : FeatureFlagConstant("MedDAutoRun")

    /**
     * Disables allowing duplicate RSV dose in checkout
     */
    object DisableDuplicateRSV : FeatureFlagConstant("DisableDoubleRSVDoses")

    /**
     * Enables ability to switch stock type during checkout
     * E.g. From Private to VFC
     * See InventorySource class for more stock reference
     */
    object MobileStockSelector : FeatureFlagConstant("MobileStockSelector")

    /**
     * Hides the Phone collection flow for missing insurance appointment.
     */
    object DisableInsurancePhoneCapture : FeatureFlagConstant("DisableInsurancePhoneCapture")

    /**
     * Disables the "New Insurance?" screen during checkout missing payer info flow
     */
    object DisableNewInsurancePrompt : FeatureFlagConstant("DisableNewInsurancePrompt")

    /**
     * Hides the demographics collection for missing insurance checkout flow.
     */
    object DisableDemoCapture : FeatureFlagConstant("DisableDemoCapture")

    /**
     * Disables payment mode selection in dialogs in checkouts.
     */
    object DisablePaymentModeSelection : FeatureFlagConstant("DisablePaymentModeSelection")

    /**
     * Enables the payer selection screen during checkout missing payer info flow
     */
    object EnablePayorSelection : FeatureFlagConstant("EnablePayorSelection")

    /**
     * Disables the credit card capture screen and flips all doses that are SP or Copay > 0 to PB
     */
    object DisableCreditCardCapture : FeatureFlagConstant("DisableCCCapture")

    /**
     * Disables date of birth capture
     */
    object DisableDoBCapture : FeatureFlagConstant("DisableDoBCapture")

    /**
     * Enable PlayCore InAppUpdates
     */
    object UseInAppUpdates : FeatureFlagConstant("UseInAppUpdates")

    /**
     * Disables the warning / blocker feature from the Config Job
     */
    object DisableOutOfDate : FeatureFlagConstant("DisableOutOfDate")

    /**
     * Enables the paymentMode change of a self pay visit when skipping the phone collection flow
     */
    object VaxCare3 : FeatureFlagConstant("VaxCare3.0")

    /**
     * Enables showing "Employer Covered" selection in Payer Selection
     */
    object EmployerCovered : FeatureFlagConstant("EmployerCovered")
}
