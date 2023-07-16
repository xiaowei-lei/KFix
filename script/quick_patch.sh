old_commit=$1
new_commit=$2
build_type="debug"
if [ -n "$3" ]; then
  build_type=$3
fi

script_dir=$(dirname "$0")
echo "script_dir: ${script_dir}"
cd ${script_dir}
patch_input_dir=`pwd`
cd ..
kfix_dir=`pwd`

function capitalize_first_letter() {
  local input_string="$1"
  local first_char=$(echo "${input_string}" | cut -c 1 | tr '[:lower:]' '[:upper:]')
  local result="${first_char}${input_string:1}"
  echo "${result}"
}

function assemble() {
  ./gradlew clean assemble$(capitalize_first_letter "${build_type}")
}

function buildOldApk() {
  echo "=============================================Build old apk============================================="
  rm -rf ${patch_input_dir}/v1
  mkdir ${patch_input_dir}/v1
  ls ${patch_input_dir}/v1
  git checkout -m ${old_commit}
  assemble
  cp ${kfix_dir}/app/build/outputs/apk/${build_type}/app-${build_type}.apk ${patch_input_dir}/v1/app-${build_type}.apk
  cp ${kfix_dir}/app/build/outputs/mapping/${build_type}/mapping.txt ${patch_input_dir}/v1/mapping.txt
}

function buildNewApk() {
  rm -rf ${patch_input_dir}/v2
  mkdir ${patch_input_dir}/v2
  echo "=============================================Build new apk============================================="
  git checkout -m ${new_commit}
  rule=${kfix_dir}/app/proguard-rules.pro
  chmod 777 ${rule}
  sed -i '' '/applymapping/d' ${rule}
  echo >> ${rule}
  echo "-applymapping \"${patch_input_dir}/v1/mapping.txt\"" >> ${rule}
  assemble

  cp ${kfix_dir}/app/build/outputs/apk/${build_type}/app-${build_type}.apk ${patch_input_dir}/v2/app-${build_type}.apk
  cp ${kfix_dir}/app/build/outputs/mapping/${build_type}/mapping.txt ${patch_input_dir}/v2/mapping.txt
}

function buildPatch() {
  echo "=============================================Build patch============================================="
  args="${kfix_dir}/build/ ${patch_input_dir}/v1/app-${build_type}.apk ${patch_input_dir}/v1/mapping.txt ${patch_input_dir}/v2/app-${build_type}.apk ${patch_input_dir}/v2/mapping.txt"
  echo "${args}"
  ./gradlew :patch:run --args "${args}"
}

function install() {
  echo "=============================================Install============================================="
  adb uninstall com.kfix.sample
  adb install ${patch_input_dir}/v1/app-${build_type}.apk
  adb push ${kfix_dir}/build/patch.zip /sdcard/kfix/patch_${build_type}.zip
  adb shell am start -n com.kfix.sample/.MainActivity
}

function recover() {
  echo "=============================================Recover============================================="
  git checkout -m main
#  git stash
}

# https://source.android.com/docs/core/runtime/configure?hl=zh-cn
# https://source.android.com/docs/core/runtime/jit-compiler?hl=zh-cn
# adb shell cmd package compile -m quicken -f com.kfix.sample
# adb shell cmd package compile -m speed -f com.kfix.sample

buildOldApk
buildNewApk
buildPatch
install
recover