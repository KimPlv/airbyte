#!/usr/bin/env bash

set -e

ROOT_DIR=$(cd ../../ && pwd)

YAML_DIR=airbyte-cdk/python/airbyte_cdk/sources/declarative
OUTPUT_DIR=airbyte-cdk/python/airbyte_cdk/sources/declarative/models

function main() {
  rm -rf "$ROOT_DIR/$OUTPUT_DIR"/*.py
  echo "# generated by generate-component-manifest-files" > "$ROOT_DIR/$OUTPUT_DIR"/__init__.py

  for f in "$ROOT_DIR/$YAML_DIR"/*.yaml; do
    filename_wo_ext=$(basename "$f" | cut -d . -f 1)
    echo "from .$filename_wo_ext import *" >> "$ROOT_DIR/$OUTPUT_DIR"/__init__.py

    datamodel-codegen \
      --input "$ROOT_DIR/$YAML_DIR/$filename_wo_ext.yaml" \
      --output "$ROOT_DIR/$OUTPUT_DIR/$filename_wo_ext.py" \
      --output-model-type 'pydantic_v2.BaseModel' \
      --disable-timestamp \
      --enum-field-as-literal one \
      --set-default-enum-member

    # There is a limitation of Pydantic where a model's private fields starting with an underscore are inaccessible.
    # The Pydantic model generator replaces special characters like $ with the underscore which results in all
    # component's $parameters field resulting in _parameters. We have been unable to find a workaround in the
    # model generator or while reading the field. There is no estimated timeline on the fix even though it is
    # widely debated here:
    # https://github.com/pydantic/pydantic/issues/288.
    #
    # Our low effort way to address this is to perform additional post-processing to rename _parameters to parameters.
    # We can revisit this if there is movement on a fix.
    temp_file=$(mktemp)
    sed 's/ _parameters:/ parameters:/g' "$ROOT_DIR/$OUTPUT_DIR/$filename_wo_ext.py" > "${temp_file}"
    output_file="$ROOT_DIR/$OUTPUT_DIR/$filename_wo_ext.py"
    mv "${temp_file}" "${output_file}"
    echo "Generated component manifest files into '${output_file}'."
  done
}

main "$@"
