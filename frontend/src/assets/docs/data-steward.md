# Data Steward Guide: Reference Data Service

## 1. Introduction

Welcome, Data Steward! This guide provides you with the information you need to manage the reference data in this application. As a Data Steward, you are responsible for maintaining the accuracy, completeness, and quality of the reference data through the Change Request workflow.

## 2. Core Responsibilities

- **Data Accuracy**: Ensure that all reference data is correct and up-to-date.
- **Change Management**: Create and manage Change Requests (CRs) for any modifications to the data.
- **Data Governance**: Approve or reject CRs submitted by other users, ensuring they comply with data standards.
- **Bulk Data Imports**: Use the Bulk Import feature to load large datasets from external sources.

## 3. Managing Data with Change Requests

All modifications to reference data, whether creating a new record, updating an existing one, or deleting one, must be done through a Change Request.

### 3.1. Creating a Change Request for a Single Record

1.  **Navigate to the Data**: From the main menu, navigate to the type of data you wish to modify (e.g., "Reference Data" -> "Countries").
2.  **Find the Record**: Use the search and filter functions to locate the specific record you want to change.
3.  **Initiate the Change**:
    *   To **update** a record, click the "Edit" button for that record.
    *   To **create** a new record, click the "Add New" button.
4.  **Enter the Changes**: Fill out the form with the new or updated information.
5.  **Submit the Change Request**:
    *   Click "Request Update" or "Request Creation".
    *   You will be prompted to provide a **Business Justification**. This is a mandatory field and should clearly explain why the change is needed. Be as detailed as possible.
    *   Click "Submit Request".
6.  **Track Your Request**: A unique Change Request ID (e.g., `CR-2025-123456`) will be generated. You can track the status of your request on the "Change Requests" dashboard.

### 3.2. Creating a Change Request via Bulk Import

The Bulk Import Wizard allows you to upload a file (CSV or JSON) to create or update many records at once.

1.  **Access the Bulk Import Wizard**: From the main menu, go to "Import/Export" -> "Bulk Import Wizard".
2.  **Upload Your File**:
    *   Drag and drop your CSV or JSON file into the upload area.
    *   Select the **Import Mode**:
        *   `Create Only`: Only new records will be created.
        *   `Update Only`: Only existing records will be updated.
        *   `Upsert`: The system will create new records and update existing ones.
3.  **Review Validation Results**:
    *   The system will validate your file against the data standards.
    *   Review the validation summary (e.g., "95 valid records, 5 invalid records").
    *   You can **download the validation report** to see detailed errors for each invalid row. You may need to fix your source file and re-upload it.
4.  **Preview and Submit**:
    *   Preview the changes that will be made.
    *   Provide a **Business Justification** for the bulk import.
    *   Click "Submit for Approval". A single Change Request will be created for the entire bulk import batch.

## 4. Approving Change Requests

If you have approval permissions, you will be able to approve or reject change requests submitted by yourself or other users.

1.  **Access the Approval Dashboard**: Navigate to "Change Requests" -> "Pending Approvals".
2.  **Review the Change Request**:
    *   Click on a CR to see the details.
    *   The "Diff" view is particularly useful, as it shows a side-by-side comparison of the current values and the proposed changes.
    *   Carefully review the **Business Justification** to ensure the change is valid and necessary.
3.  **Take Action**:
    *   **Approve**: If the change is correct and well-justified, click "Approve". You can add an optional comment.
    *   **Reject**: If the change is incorrect or lacks justification, click "Reject". You **must** provide a clear reason for the rejection so the requester can make corrections.
    *   **Request More Info**: If you need more information, use the comments section to communicate with the requester. The CR will remain in a "PENDING" state.
4.  **Bulk Approval/Rejection**: You can select multiple CRs from the dashboard and approve or reject them in a single action.

## 5. Understanding Change Request Statuses

- **`PENDING`**: The CR has been submitted and is awaiting approval.
- **`APPROVED`**: The CR has been approved by a Data Steward. It is now ready to be applied to the database.
- **`REJECTED`**: The CR has been rejected. The requester will need to create a new CR with corrections.
- **`APPLIED`**: The changes have been successfully saved to the database and are now live.
- **`CANCELLED`**: The requester or an administrator has cancelled the CR.

---
*For API details for programmatic access, see the [API Documentation](API_DOCUMENTATION.md).*
