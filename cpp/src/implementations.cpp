#include "dev_lukebemish_opensesame_runtime_NativeImplementations.h"

JNIEXPORT jobject JNICALL Java_dev_lukebemish_opensesame_runtime_NativeImplementations_nativeImplLookup
  (JNIEnv* env, jclass) {
    jclass cls = env->FindClass("java/lang/invoke/MethodHandles$Lookup");
    jfieldID fid = env->GetStaticFieldID(cls, "IMPL_LOOKUP", "Ljava/lang/invoke/MethodHandles$Lookup;");
    return env->GetStaticObjectField(cls, fid);
}

