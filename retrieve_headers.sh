BUILD_DIR=$(pwd)

rm -rf "${BUILD_DIR}/jni_headers"
mkdir -p "${BUILD_DIR}/jni_headers"
git -C "${BUILD_DIR}/jni_headers" init
git -C "${BUILD_DIR}/jni_headers" remote add origin https://github.com/openjdk/jdk.git
git -C "${BUILD_DIR}/jni_headers" sparse-checkout set src/java.base/share/native/include src/java.base/unix/native/include src/java.base/windows/native/include
git -C "${BUILD_DIR}/jni_headers" fetch --depth=1 --filter=blob:none origin dfacda488bfbe2e11e8d607a6d08527710286982
git -C "${BUILD_DIR}/jni_headers" checkout dfacda488bfbe2e11e8d607a6d08527710286982