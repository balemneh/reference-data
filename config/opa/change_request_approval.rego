package change_request_approval

import future.keywords.contains
import future.keywords.if
import future.keywords.in

# Default deny
default allow := false
default requiresAdditionalApproval := false

# Auto-approve minor changes from trusted sources
allow if {
    input.changeType in ["UPDATE", "CORRECTION"]
    input.requestor in trusted_users
    not high_risk_dataset
}

# Require additional approval for high-risk changes
requiresAdditionalApproval if {
    input.changeType in ["DELETE", "BULK_UPDATE"]
}

# Require additional approval for sensitive datasets
requiresAdditionalApproval if {
    high_risk_dataset
}

# Deny deletions on critical datasets without explicit approval
allow if {
    input.changeType == "DELETE"
    not critical_dataset
    has_delete_permission
}

# Allow creates from authorized users
allow if {
    input.changeType == "CREATE"
    has_create_permission
}

# Allow updates from authorized users with proper justification
allow if {
    input.changeType == "UPDATE"
    has_update_permission
    has_valid_justification
}

# Define trusted users
trusted_users := {
    "system",
    "data_curator",
    "admin"
}

# Define high-risk datasets
high_risk_dataset if {
    input.datasetType in ["COUNTRY", "CARRIER"]
}

# Define critical datasets that require special handling
critical_dataset if {
    input.datasetType == "COUNTRY"
}

# Check permissions based on requestor role
has_create_permission if {
    input.requestor in ["admin", "data_curator", "system"]
}

has_update_permission if {
    input.requestor in ["admin", "data_curator", "system", "data_editor"]
}

has_delete_permission if {
    input.requestor in ["admin", "system"]
}

# Check for valid justification in the payload
has_valid_justification if {
    input.payload.justification
    count(input.payload.justification) > 10
}

# Generate reason for the decision
reason := msg if {
    not allow
    msg := concat(" ", [
        "Change request denied.",
        denied_reason
    ])
} else := "Change request approved based on policy rules"

denied_reason := "User lacks required permissions" if {
    input.changeType == "DELETE"
    not has_delete_permission
} else := "User lacks required permissions" if {
    input.changeType == "CREATE"
    not has_create_permission
} else := "User lacks required permissions" if {
    input.changeType == "UPDATE"
    not has_update_permission
} else := "Missing valid justification" if {
    input.changeType == "UPDATE"
    not has_valid_justification
} else := "Critical dataset requires manual approval" if {
    critical_dataset
} else := "Unknown denial reason"