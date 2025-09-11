package data_validation

import future.keywords.contains
import future.keywords.if
import future.keywords.in

# Validate country data
valid_country if {
    input.countryCode
    regex.match("^[A-Z]{2,3}$", input.countryCode)
    input.countryName
    count(input.countryName) > 1
    count(input.countryName) < 100
}

# Validate port data
valid_port if {
    input.portCode
    regex.match("^[A-Z0-9]{3,10}$", input.portCode)
    input.portName
    input.countryCode
    regex.match("^[A-Z]{2,3}$", input.countryCode)
}

# Validate airport data
valid_airport if {
    valid_airport_codes
    input.airportName
    input.countryCode
    regex.match("^[A-Z]{2,3}$", input.countryCode)
}

valid_airport_codes if {
    input.iataCode
    regex.match("^[A-Z]{3}$", input.iataCode)
} else if {
    input.icaoCode
    regex.match("^[A-Z]{4}$", input.icaoCode)
}

# Validate carrier data
valid_carrier if {
    input.carrierCode
    regex.match("^[A-Z0-9]{2,10}$", input.carrierCode)
    input.carrierName
    count(input.carrierName) > 1
}

# Check for duplicate codes
no_duplicate_country_code if {
    not data.existing_countries[input.countryCode]
}

no_duplicate_port_code if {
    not data.existing_ports[input.portCode]
}

no_duplicate_iata_code if {
    not data.existing_airports.iata[input.iataCode]
}

no_duplicate_icao_code if {
    not data.existing_airports.icao[input.icaoCode]
}

# Validate coordinates
valid_coordinates if {
    input.latitude
    input.longitude
    input.latitude >= -90
    input.latitude <= 90
    input.longitude >= -180
    input.longitude <= 180
}

# Overall validation result
validation_result := {
    "valid": valid,
    "errors": errors
}

default valid := false

valid if {
    input.datasetType == "COUNTRY"
    valid_country
} else if {
    input.datasetType == "PORT"
    valid_port
} else if {
    input.datasetType == "AIRPORT"
    valid_airport
} else if {
    input.datasetType == "CARRIER"
    valid_carrier
}

errors[msg] if {
    input.datasetType == "COUNTRY"
    not valid_country
    msg := "Invalid country data format"
}

errors[msg] if {
    input.datasetType == "PORT"
    not valid_port
    msg := "Invalid port data format"
}

errors[msg] if {
    input.datasetType == "AIRPORT"
    not valid_airport
    msg := "Invalid airport data format"
}

errors[msg] if {
    input.datasetType == "CARRIER"
    not valid_carrier
    msg := "Invalid carrier data format"
}

errors[msg] if {
    input.latitude
    input.longitude
    not valid_coordinates
    msg := "Invalid coordinates"
}