#!/bin/bash

# Download and extract GBFS json schema
validate () {
    NAME=$1
    VALUE=$2

    if [ -z ${VALUE} ];
    then
        echo "$NAME not set"
        exit 1
    fi
}

validate "GITHUB_URL" ${GITHUB_URL}
validate "DESTINATION_PATH" ${DESTINATION_PATH}

ZIP_FILE=downloaded.zip

echo "GBFS JSON schema repo github URL: $GITHUB_URL"

echo "Removing any existing contents in $DESTINATION_PATH"
rm -rf ${DESTINATION_PATH}/*
mkdir -p ${DESTINATION_PATH}

if [ -f ${ZIP_FILE} ]; then
  echo "Removing existing file $ZIP_FILE"
  rm ${ZIP_FILE}
fi

WGET_URL="${GITHUB_URL}"
echo "About to download from $WGET_URL"
wget -q ${WGET_URL} -O ${ZIP_FILE}

if [ -f ${ZIP_FILE} ]; then
    echo "Done"
    # Validate the downloaded zip file
    if ! unzip -t "${ZIP_FILE}" > /dev/null; then
        (>&2 echo "Error: Downloaded file ${ZIP_FILE} is not a valid zip archive.")
        rm "${ZIP_FILE}"
        exit 1
    fi
    {
    echo "Create ${DESTINATION_PATH}" &&
    mkdir -p ${DESTINATION_PATH} &&

    echo "Unzip files from zip file ${ZIP_FILE} to ${DESTINATION_PATH}" &&
    unzip -q ${ZIP_FILE} -d ${DESTINATION_PATH} &&

    echo "Remove zipfile ${ZIP_FILE}" &&
    rm ${ZIP_FILE} &&

    # This script empties $DESTINATION_PATH before downloading, so the unzip
    # leaves exactly one gbfs-json-schema-<version> folder in it. Resolve that
    # folder rather than naming the version, so the version does not have to be
    # repeated outside the download URL. Resolve it before the move, since the
    # move can lift up entries that would themselves match the glob.
    echo "Remove intermediate folder" &&
    SCHEMA_DIR=$(ls -d ${DESTINATION_PATH}/gbfs-json-schema-*) &&
    mv "${SCHEMA_DIR}"/* ${DESTINATION_PATH} &&
    rm -rf "${SCHEMA_DIR}"

    echo "JSON schema extracted to $DESTINATION_PATH"
    } ||
    {
        (>&2 echo "Error extracting zip file $ZIP_FILE from $WGET_URL. See my previous output for details")
        exit 1
    }
else
    (>&2 echo "Error downloading zip from $WGET_URL")
    exit 1
fi