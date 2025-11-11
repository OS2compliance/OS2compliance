#!/bin/sh

# Define the directory (relative path from Bitbucket Pipeline workspace root)
FOLDER="src/main/resources/db/migration"

# Ensure the folder exists
if [ ! -d "$FOLDER" ]; then
  echo "Error: Migration folder '$FOLDER' does not exist."
  exit 1
fi

# Get all SQL files in the folder
FILES=$(ls "$FOLDER" | grep -E '^V[0-9]+_[0-9]+__.*\.sql$')

# Track versions using a simple string with newlines
SEEN_VERSIONS=""

# Loop through each file
for FILE in $FILES; do
  # Extract the version number (pattern: Vx_x__)
  VERSION=$(echo "$FILE" | grep -oE '^V[0-9]+_[0-9]+__')

  # Check if a version number was found
  if [ -n "$VERSION" ]; then
    # Check if the version already exists in our tracking string
    case "$SEEN_VERSIONS" in
      *"$VERSION"*)
        # Find the original file with this version
        ORIGINAL_FILE=$(echo "$SEEN_VERSIONS" | grep "$VERSION" | cut -d'|' -f2)
        echo "Error: Files '$ORIGINAL_FILE' and '$FILE' in folder '$FOLDER' have the same version number: '$VERSION'"
        exit 1
        ;;
    esac

    # Store the version and filename (format: VERSION|FILENAME)
    SEEN_VERSIONS="$SEEN_VERSIONS
$VERSION|$FILE"
  fi
done

# If no issues found, pass the check
echo "No duplicate Flyway migration versions found."
exit 0